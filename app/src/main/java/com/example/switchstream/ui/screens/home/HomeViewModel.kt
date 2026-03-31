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
                    // Fetch all sections, then batch into a single state update
                    val continueWatching = libraryRepo.getResumeItems().getOrNull() ?: emptyList()
                    val nextUp = libraryRepo.getNextUp(limit = 10).getOrNull() ?: emptyList()
                    val suggestions = libraryRepo.getSuggestions(limit = 8).getOrNull() ?: emptyList()
                    val recentlyAdded = libraryRepo.getRecentlyAdded(limit = 20).getOrNull() ?: emptyList()
                    val favorites = libraryRepo.getFavorites(limit = 16).getOrNull() ?: emptyList()

                    val latestMap = mutableMapOf<UUID, List<BaseItemDto>>()
                    for (lib in libs) {
                        libraryRepo.getLatestMedia(lib.id).onSuccess { items ->
                            latestMap[lib.id] = items
                        }
                    }

                    // Fallback for featured items if suggestions API fails
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