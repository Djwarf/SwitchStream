package com.example.switchstream.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.data.repository.LibraryRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val libraries: List<BaseItemDto> = emptyList(),
    val continueWatching: List<BaseItemDto> = emptyList(),
    val nextUp: List<BaseItemDto> = emptyList(),
    val featuredItems: List<BaseItemDto> = emptyList(),
    val recentlyAdded: List<BaseItemDto> = emptyList(),
    val favorites: List<BaseItemDto> = emptyList(),
    val latestByLibrary: Map<UUID, List<BaseItemDto>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            libraryRepo.getLibraries().fold(
                onSuccess = { libs ->
                    _uiState.value = _uiState.value.copy(libraries = libs)

                    // Continue watching
                    libraryRepo.getResumeItems().onSuccess { items ->
                        _uiState.value = _uiState.value.copy(continueWatching = items)
                    }

                    // Next Up — next unwatched episodes across all shows
                    libraryRepo.getNextUp(limit = 10).onSuccess { nextUp ->
                        _uiState.value = _uiState.value.copy(nextUp = nextUp)
                    }

                    // Featured hero — Jellyfin's recommendation engine for discovery
                    libraryRepo.getSuggestions(limit = 8).onSuccess { suggestions ->
                        _uiState.value = _uiState.value.copy(featuredItems = suggestions)
                    }.onFailure {
                        // Fallback: use latest items from first library
                        val fallback = uiState.value.latestByLibrary.values
                            .flatten().take(8)
                        if (fallback.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(featuredItems = fallback)
                        }
                    }

                    // Recently added across all libraries
                    libraryRepo.getRecentlyAdded(limit = 20).onSuccess { items ->
                        _uiState.value = _uiState.value.copy(recentlyAdded = items)
                    }

                    // My Favorites
                    libraryRepo.getFavorites(limit = 16).onSuccess { items ->
                        _uiState.value = _uiState.value.copy(favorites = items)
                    }

                    // Latest media per library
                    val latestMap = mutableMapOf<UUID, List<BaseItemDto>>()
                    for (lib in libs) {
                        libraryRepo.getLatestMedia(lib.id).onSuccess { items ->
                            latestMap[lib.id] = items
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        latestByLibrary = latestMap,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load: ${e.message}"
                    )
                }
            )
        }
    }

    fun refresh() {
        loadHomeData()
    }
}