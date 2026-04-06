package com.switchsides.switchstream.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.NetworkMonitor
import com.switchsides.switchstream.data.repository.DownloadRepository
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.data.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto

data class SearchUiState(
    val query: String = "",
    val results: List<BaseItemDto> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository,
    private val downloadRepo: DownloadRepository? = null,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val _offlineResults = MutableStateFlow<List<com.switchsides.switchstream.data.db.DownloadedMedia>>(emptyList())
    val offlineResults: StateFlow<List<com.switchsides.switchstream.data.db.DownloadedMedia>> = _offlineResults.asStateFlow()

    init {
        queryFlow
            .debounce(250L)
            .distinctUntilChanged()
            .onEach { debouncedQuery ->
                if (debouncedQuery.length >= 2) {
                    performSearch(debouncedQuery)
                } else if (debouncedQuery.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        results = emptyList(),
                        hasSearched = false,
                        isSearching = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateQuery(text: String) {
        _uiState.value = _uiState.value.copy(query = text)
        queryFlow.value = text
    }

    private fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)

        viewModelScope.launch {
            val isOnline = networkMonitor?.isOnline?.value ?: true

            if (!isOnline && downloadRepo != null) {
                // Offline: search through downloaded content
                val downloads = downloadRepo.getCompletedDownloads().first()
                val lowerQuery = query.lowercase()
                val matched = downloads.filter {
                    it.title.lowercase().contains(lowerQuery) ||
                        (it.seriesName?.lowercase()?.contains(lowerQuery) == true)
                }
                _uiState.value = _uiState.value.copy(
                    results = emptyList(),
                    isSearching = false,
                    hasSearched = true
                )
                // Store offline results separately since they're DownloadedMedia not BaseItemDto
                _offlineResults.value = matched
            } else {
                // Online: search by title + genre in parallel, deduplicate
                _offlineResults.value = emptyList()
                try {
                    val titleResults = libraryRepo.search(query).getOrNull() ?: emptyList()
                    val genreResults = libraryRepo.searchByGenre(query).getOrNull() ?: emptyList()
                    val seenIds = titleResults.map { it.id }.toSet()
                    val combined = titleResults + genreResults.filter { it.id !in seenIds }

                    _uiState.value = _uiState.value.copy(
                        results = combined,
                        isSearching = false,
                        hasSearched = true
                    )
                } catch (_: Exception) {
                    // Fallback to offline search on network error
                    if (downloadRepo != null) {
                        val downloads = downloadRepo.getCompletedDownloads().first()
                        val lowerQuery = query.lowercase()
                        _offlineResults.value = downloads.filter {
                            it.title.lowercase().contains(lowerQuery) ||
                                (it.seriesName?.lowercase()?.contains(lowerQuery) == true)
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        results = emptyList(),
                        isSearching = false,
                        hasSearched = true
                    )
                }
            }
        }
    }

}
