package com.switchsides.switchstream.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.data.repository.LibraryRepository
import com.switchsides.switchstream.util.isNetworkError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto

data class HistoryUiState(
    val items: List<BaseItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = true,
    val error: String? = null
)

class HistoryViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val pageSize = 50

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            libraryRepo.getWatchedItems(startIndex = 0, limit = pageSize).fold(
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
                        error = if (isNetworkError(e)) "You're offline" else "Failed to load watch history: ${e.message}"
                    )
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMore) return

        viewModelScope.launch {
            libraryRepo.getWatchedItems(
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
        _uiState.value = HistoryUiState()
        loadItems()
    }
}
