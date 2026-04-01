package com.example.switchstream.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import com.example.switchstream.ui.components.EditorialCard
import org.jellyfin.sdk.model.api.BaseItemKind
import com.example.switchstream.ui.components.FocusableButton
import com.example.switchstream.ui.components.FilterSortBar
import com.example.switchstream.ui.components.LoadingIndicator
import com.example.switchstream.ui.theme.LocalDimensions
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.TextPrimary

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onItemClick: (itemId: String) -> Unit,
    onGenreBrowse: ((libraryId: String) -> Unit)? = null
) {
    val dims = LocalDimensions.current
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // Scroll to top when sort/filter changes
    LaunchedEffect(uiState.sortBy, uiState.sortOrder, uiState.selectedGenres, uiState.watchedFilter) {
        gridState.scrollToItem(0)
    }

    // Infinite scroll trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= uiState.items.size - 10
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        // Header — offset below floating nav on mobile/tablet
        Text(
            text = uiState.libraryName,
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier
                .padding(top = dims.topBarClearance)
                .padding(horizontal = dims.screenPadding, vertical = 24.dp)
        )

        FilterSortBar(
            sortBy = uiState.sortBy,
            onSortChange = viewModel::setSortBy,
            sortOrder = uiState.sortOrder,
            onSortOrderChange = { viewModel.toggleSortOrder() },
            availableGenres = uiState.availableGenres,
            selectedGenres = uiState.selectedGenres,
            onGenreToggle = viewModel::toggleGenre,
            watchedFilter = uiState.watchedFilter,
            onWatchedFilterChange = viewModel::setWatchedFilter,
            onGenreBrowse = if (onGenreBrowse != null) {
                { onGenreBrowse(uiState.libraryId) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = dims.gridMinCellSize),
                state = gridState,
                contentPadding = PaddingValues(horizontal = dims.screenPadding, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    val year = item.productionYear?.toString()
                    val icon = when (item.type) {
                        BaseItemKind.MOVIE -> Icons.Outlined.Movie
                        BaseItemKind.SERIES -> Icons.Outlined.Tv
                        else -> null
                    }
                    EditorialCard(
                        title = item.name ?: "",
                        imageUrl = viewModel.imageRepo.getThumbUrl(item.id),
                        subtitle = year,
                        typeIcon = icon,
                        onClick = { onItemClick(item.id.toString()) }
                    )
                }
            }
        }
    }
}
