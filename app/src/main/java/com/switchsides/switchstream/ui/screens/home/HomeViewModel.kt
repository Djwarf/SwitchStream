package com.switchsides.switchstream.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.data.repository.LibraryRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class RecommendationRow(
    val sourceTitle: String,
    val items: List<BaseItemDto>
)

data class HomeUiState(
    val libraries: List<BaseItemDto> = emptyList(),
    val continueWatching: List<BaseItemDto> = emptyList(),
    val nextUp: List<BaseItemDto> = emptyList(),
    val featuredItems: List<BaseItemDto> = emptyList(),
    val recentlyAdded: List<BaseItemDto> = emptyList(),
    val favorites: List<BaseItemDto> = emptyList(),
    val latestByLibrary: Map<UUID, List<BaseItemDto>> = emptyMap(),
    val recommendations: List<RecommendationRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false,
    val offlineItems: List<com.switchsides.switchstream.data.db.DownloadedMedia> = emptyList()
)

class HomeViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository,
    private val downloadRepo: com.switchsides.switchstream.data.repository.DownloadRepository? = null,
    private val settingsManager: com.switchsides.switchstream.data.SettingsManager? = null,
    private val isTV: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkOfflineModeAndLoad()
    }

    private fun checkOfflineModeAndLoad() {
        if (isTV || settingsManager == null) {
            loadHomeData()
            return
        }
        viewModelScope.launch {
            val settings = settingsManager.settings.first()
            if (settings.offlineMode) {
                loadOfflineContent()
            } else {
                loadHomeData()
            }
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            libraryRepo.getLibraries().fold(
                onSuccess = { libs ->
                    // Fetch all sections in parallel
                    val continueWatchingDef = async { libraryRepo.getResumeItems().getOrNull() ?: emptyList() }
                    val nextUpDef = async { libraryRepo.getNextUp(limit = 10).getOrNull() ?: emptyList() }
                    val suggestionsDef = async { libraryRepo.getSuggestions(limit = 8).getOrNull() ?: emptyList() }
                    val recentlyAddedDef = async { libraryRepo.getRecentlyAdded(limit = 20).getOrNull() ?: emptyList() }
                    val favoritesDef = async { libraryRepo.getFavorites(limit = 16).getOrNull() ?: emptyList() }

                    // Fetch per-library items in parallel
                    val latestDefs = libs.map { lib ->
                        async { lib.id to (libraryRepo.getLatestMedia(lib.id).getOrNull() ?: emptyList()) }
                    }

                    val continueWatching = continueWatchingDef.await()
                    val nextUp = nextUpDef.await()
                    val suggestions = suggestionsDef.await()
                    val recentlyAdded = recentlyAddedDef.await()
                    val favorites = favoritesDef.await()
                    val latestMap = latestDefs.awaitAll().toMap()

                    val featured = suggestions.ifEmpty {
                        latestMap.values.flatten().take(8)
                    }

                    _uiState.value = _uiState.value.copy(
                        libraries = libs,
                        continueWatching = continueWatching,
                        nextUp = nextUp,
                        featuredItems = featured,
                        recentlyAdded = recentlyAdded,
                        favorites = favorites,
                        latestByLibrary = latestMap,
                        isLoading = false
                    )

                    // Load "Because you watched" recommendations in background
                    loadRecommendations(continueWatching)
                },
                onFailure = { e ->
                    if (!isTV) {
                        // Mobile/tablet: offline fallback — show downloaded content
                        loadOfflineContent()
                    }
                    if (isTV || _uiState.value.offlineItems.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = if (com.switchsides.switchstream.util.isNetworkError(e)) "You're offline" else "Failed to load: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    private fun loadRecommendations(recentItems: List<BaseItemDto>) {
        viewModelScope.launch {
            // Take up to 3 recently watched items for recommendations
            val sources = recentItems
                .filter { it.seriesId != null || it.type == org.jellyfin.sdk.model.api.BaseItemKind.MOVIE }
                .take(3)

            val rows = sources.mapNotNull { item ->
                val sourceId = item.seriesId ?: item.id
                val sourceName = item.seriesName ?: item.name ?: return@mapNotNull null
                val similar = libraryRepo.getSimilarItems(sourceId).getOrNull() ?: emptyList()
                if (similar.isNotEmpty()) RecommendationRow(sourceName, similar.take(12))
                else null
            }

            if (rows.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(recommendations = rows)
            }
        }
    }

    private fun loadOfflineContent() {
        if (downloadRepo == null) return
        viewModelScope.launch {
            downloadRepo.getCompletedDownloads().collect { downloads ->
                _uiState.value = _uiState.value.copy(
                    offlineItems = downloads,
                    isOffline = true,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    fun refresh() {
        checkOfflineModeAndLoad()
    }
}