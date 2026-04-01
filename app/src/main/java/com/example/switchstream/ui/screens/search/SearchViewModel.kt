package com.example.switchstream.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.NetworkMonitor
import com.example.switchstream.data.repository.DownloadRepository
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.data.repository.LibraryRepository
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
                // Online: search server
                _offlineResults.value = emptyList()
                libraryRepo.search(query).fold(
                    onSuccess = { items ->
                        _uiState.value = _uiState.value.copy(
                            results = items,
                            isSearching = false,
                            hasSearched = true
                        )
                    },
                    onFailure = {
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
                )
            }
        }
    }

    private val _offlineResults = MutableStateFlow<List<com.example.switchstream.data.db.DownloadedMedia>>(emptyList())
    val offlineResults: StateFlow<List<com.example.switchstream.data.db.DownloadedMedia>> = _offlineResults.asStateFlow()
}
