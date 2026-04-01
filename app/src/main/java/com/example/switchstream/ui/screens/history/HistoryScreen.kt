package com.example.switchstream.ui.screens.history

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
import com.example.switchstream.ui.theme.LocalDimensions
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.TextPrimary
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onItemClick: (itemId: String) -> Unit
) {
    val dims = LocalDimensions.current
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
            .padding(top = dims.topBarClearance)
    ) {
        // Title
        Text(
            text = "Watch History",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 24.dp)
        )

        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "An error occurred",
                onRetry = { viewModel.retry() }
            )
            uiState.items.isEmpty() -> EmptyState("Nothing watched yet")
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = dims.screenPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        val progress = item.userData?.playedPercentage?.let { it.toFloat() / 100f }
                        val subtitle = buildHistorySubtitle(item)
                        // Use series thumbnail for episodes
                        val imageId = item.seriesId ?: item.id
                        val icon = when (item.type) {
                            BaseItemKind.MOVIE -> Icons.Outlined.Movie
                            BaseItemKind.SERIES, BaseItemKind.EPISODE -> Icons.Outlined.Tv
                            else -> null
                        }
                        EditorialCard(
                            title = item.seriesName ?: item.name ?: "",
                            imageUrl = viewModel.imageRepo.getThumbUrl(imageId),
                            subtitle = subtitle,
                            progress = progress,
                            typeIcon = icon,
                            onClick = { onItemClick(item.id.toString()) },
                            cardWidth = 280.dp,
                            imageHeight = 158.dp
                        )
                    }
                }
            }
        }
    }
}

private fun buildHistorySubtitle(item: BaseItemDto): String {
    val parts = mutableListOf<String>()

    // Episode info
    val season = item.parentIndexNumber
    val episode = item.indexNumber
    if (season != null && episode != null) {
        parts.add("S$season:E$episode")
    }

    // Watched status
    val playedPercent = item.userData?.playedPercentage
    if (playedPercent != null && playedPercent >= 95) {
        parts.add("Watched")
    } else if (playedPercent != null && playedPercent > 0) {
        val totalTicks = item.runTimeTicks
        if (totalTicks != null) {
            val totalMin = totalTicks / 600_000_000
            val remainingMin = (totalMin * (100 - playedPercent) / 100).toLong()
            if (remainingMin > 0) parts.add("${remainingMin}min left")
        }
    }

    // Year fallback
    if (parts.isEmpty()) {
        item.productionYear?.let { parts.add(it.toString()) }
    }

    return parts.joinToString(" · ")
}
