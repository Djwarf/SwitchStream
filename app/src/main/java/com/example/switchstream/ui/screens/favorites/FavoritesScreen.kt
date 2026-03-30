package com.example.switchstream.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.switchstream.ui.components.EmptyState
import com.example.switchstream.ui.components.ErrorState
import com.example.switchstream.ui.components.LoadingIndicator
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.TextPrimary

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onItemClick: (itemId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= uiState.items.size - 10
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMore && !uiState.isLoading) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        // Title
        Text(
            text = "My Favorites",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 56.dp, vertical = 24.dp)
        )

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "An error occurred",
                onRetry = { viewModel.retry() }
            )
            uiState.items.isEmpty() -> EmptyState(
                "No favorites yet \u2014 tap the heart on any title"
            )
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 56.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        val subtitle = item.productionYear?.toString()
                        val icon = when (item.type) {
                            BaseItemKind.MOVIE -> Icons.Outlined.Movie
                            BaseItemKind.SERIES -> Icons.Outlined.Tv
                            else -> null
                        }
                        EditorialCard(
                            title = item.name ?: "",
                            imageUrl = viewModel.imageRepo.getThumbUrl(item.id),
                            subtitle = subtitle,
                            typeIcon = icon,
                            onClick = { onItemClick(item.id.toString()) }
                        )
                    }
                }
            }
        }
    }
}
