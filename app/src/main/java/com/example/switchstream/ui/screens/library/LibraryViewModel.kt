package com.example.switchstream.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.data.repository.LibraryRepository
import com.example.switchstream.ui.components.WatchedFilter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class LibraryUiState(
    val items: List<BaseItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val libraryName: String = "",
    val sortBy: ItemSortBy = ItemSortBy.SORT_NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val availableGenres: List<String> = emptyList(),
    val selectedGenres: List<String> = emptyList(),
    val watchedFilter: WatchedFilter = WatchedFilter.ALL,
    val totalItems: Int = 0
)

class LibraryViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository,
    private val libraryId: UUID,
    libraryName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(libraryName = libraryName))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val pageSize = 50

    init {
        loadItems()
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            libraryRepo.getGenres(libraryId).onSuccess { genres ->
                _uiState.value = _uiState.value.copy(
                    availableGenres = genres.mapNotNull { it.name }
                )
            }
        }
    }

    fun setSortBy(sort: ItemSortBy) {
        _uiState.value = _uiState.value.copy(
            sortBy = sort,
            items = emptyList(),
            isLoading = true,
            hasMore = true
        )
        loadItems()
    }

    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == SortOrder.ASCENDING)
            SortOrder.DESCENDING else SortOrder.ASCENDING
        _uiState.value = _uiState.value.copy(
            sortOrder = newOrder,
            items = emptyList(),
            isLoading = true,
            hasMore = true
        )
        loadItems()
    }

    fun toggleGenre(genre: String) {
        val current = _uiState.value.selectedGenres
        val updated = if (genre in current) current - genre else current + genre
        _uiState.value = _uiState.value.copy(
            selectedGenres = updated,
            items = emptyList(),
            isLoading = true,
            hasMore = true
        )
        loadItems()
    }

    fun setWatchedFilter(filter: WatchedFilter) {
        _uiState.value = _uiState.value.copy(
            watchedFilter = filter,
            items = emptyList(),
            isLoading = true,
            hasMore = true
        )
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val state = _uiState.value
            val itemFilters = when (state.watchedFilter) {
                WatchedFilter.ALL -> null
                WatchedFilter.WATCHED -> listOf(ItemFilter.IS_PLAYED)
                WatchedFilter.UNWATCHED -> listOf(ItemFilter.IS_UNPLAYED)
            }
            libraryRepo.getItems(
                parentId = libraryId,
                startIndex = 0,
                limit = pageSize,
                sortBy = state.sortBy,
                sortOrder = state.sortOrder,
                genres = state.selectedGenres.takeIf { it.isNotEmpty() },
                filters = itemFilters
            ).fold(
                onSuccess = { (items, total) ->
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false,
                        hasMore = items.size >= pageSize,
                        totalItems = total
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            val itemFilters = when (state.watchedFilter) {
                WatchedFilter.ALL -> null
                WatchedFilter.WATCHED -> listOf(ItemFilter.IS_PLAYED)
                WatchedFilter.UNWATCHED -> listOf(ItemFilter.IS_UNPLAYED)
            }
            libraryRepo.getItems(
                parentId = libraryId,
                startIndex = state.items.size,
                limit = pageSize,
                sortBy = state.sortBy,
                sortOrder = state.sortOrder,
                genres = state.selectedGenres.takeIf { it.isNotEmpty() },
                filters = itemFilters
            ).fold(
                onSuccess = { (newItems, _) ->
                    _uiState.value = _uiState.value.copy(
                        items = _uiState.value.items + newItems,
                        isLoadingMore = false,
                        hasMore = newItems.size >= pageSize
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }
}
