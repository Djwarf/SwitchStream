package com.example.switchstream.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto

data class FavoritesUiState(
    val items: List<BaseItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = true,
    val error: String? = null
)

class FavoritesViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val pageSize = 50

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            libraryRepo.getFavorites(startIndex = 0, limit = pageSize).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false,
                        hasMore = items.size >= pageSize
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load favorites: ${e.message}"
                    )
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMore) return

        viewModelScope.launch {
            libraryRepo.getFavorites(
                startIndex = state.items.size,
                limit = pageSize
            ).fold(
                onSuccess = { newItems ->
                    _uiState.value = _uiState.value.copy(
                        items = _uiState.value.items + newItems,
                        hasMore = newItems.size >= pageSize
                    )
                },
                onFailure = { /* silently fail on pagination */ }
            )
        }
    }

    fun retry() {
        _uiState.value = FavoritesUiState()
        loadItems()
    }
}
