package com.example.switchstream.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.ui.components.EditorialCard
import com.example.switchstream.ui.components.EmptyState
import com.example.switchstream.ui.components.ErrorState
import com.example.switchstream.ui.components.HeroCarousel
import com.example.switchstream.ui.components.LoadingIndicator
import com.example.switchstream.ui.components.ShimmerHomeScreen
import com.example.switchstream.ui.components.SectionHeader
import com.example.switchstream.ui.theme.LocalDimensions
import com.example.switchstream.ui.theme.PureBlack
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onItemClick: (itemId: String) -> Unit,
    onLibraryClick: (libraryId: String, libraryName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dims = LocalDimensions.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        when {
            uiState.isLoading -> ShimmerHomeScreen()
            uiState.isOffline && uiState.offlineItems.isNotEmpty() -> OfflineContent(
                items = uiState.offlineItems,
                onItemClick = onItemClick
            )
            uiState.isOffline && uiState.offlineItems.isEmpty() -> EmptyState(
                "You're offline with no downloads.\nDownload content or turn off Offline Mode in Settings."
            )
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::refresh
            )
            else -> {
                if (!dims.isTV) {
                    // Mobile/Tablet: pull-to-refresh wrapper
                    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = viewModel::refresh,
                        state = pullState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        HomeContent(
                            uiState = uiState,
                            imageRepo = viewModel.imageRepo,
                            onItemClick = onItemClick,
                            onLibraryClick = onLibraryClick
                        )
                    }
                } else {
                    HomeContent(
                        uiState = uiState,
                        imageRepo = viewModel.imageRepo,
                        onItemClick = onItemClick,
                        onLibraryClick = onLibraryClick
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    imageRepo: ImageRepository,
    onItemClick: (String) -> Unit,
    onLibraryClick: (String, String) -> Unit
) {
    val dims = LocalDimensions.current
    val hasContent = uiState.featuredItems.isNotEmpty() ||
            uiState.recentlyAdded.isNotEmpty() ||
            uiState.continueWatching.isNotEmpty() ||
            uiState.latestByLibrary.values.any { it.isNotEmpty() }

    if (!hasContent) {
        EmptyState("No content available yet")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Hero Carousel
        if (uiState.featuredItems.isNotEmpty()) {
            item(key = "hero_carousel") {
                HeroCarousel(
                    items = uiState.featuredItems,
                    imageRepo = imageRepo,
                    onPlayClick = onItemClick,
                    onInfoClick = onItemClick
                )
            }
        }

        // Recently Added
        if (uiState.recentlyAdded.isNotEmpty()) {
            item { SectionHeader(title = "Recently Added") }
            item {
                MediaRow(
                    items = uiState.recentlyAdded,
                    imageRepo = imageRepo,
                    onItemClick = onItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Continue Watching — wider cards with progress bars
        if (uiState.continueWatching.isNotEmpty()) {
            item { SectionHeader(title = "Continue Watching") }
            item {
                ContinueWatchingRow(
                    items = uiState.continueWatching,
                    imageRepo = imageRepo,
                    onItemClick = onItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Next Up — next episodes for shows you're watching
        if (uiState.nextUp.isNotEmpty()) {
            item { SectionHeader(title = "Next Up") }
            item {
                NextUpRow(
                    items = uiState.nextUp,
                    imageRepo = imageRepo,
                    onItemClick = onItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // My Favorites
        if (uiState.favorites.isNotEmpty()) {
            item { SectionHeader(title = "My Favorites") }
            item {
                MediaRow(
                    items = uiState.favorites,
                    imageRepo = imageRepo,
                    onItemClick = onItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Per-library rows — standard cards
        for (library in uiState.libraries) {
            val libraryItems = uiState.latestByLibrary[library.id].orEmpty()
            if (libraryItems.isNotEmpty()) {
                item { SectionHeader(title = library.name ?: "Library") }
                item {
                    MediaRow(
                        items = libraryItems,
                        imageRepo = imageRepo,
                        onItemClick = onItemClick
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        // "Because you watched" recommendation rows
        for (row in uiState.recommendations) {
            item { SectionHeader(title = "Because you watched ${row.sourceTitle}") }
            item {
                MediaRow(
                    items = row.items,
                    imageRepo = imageRepo,
                    onItemClick = onItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/**
 * Continue Watching — wider cards (300dp) with progress bar overlay.
 * Shows episode info like "S2:E5 · 23min left" for TV episodes.
 */
@Composable
private fun ContinueWatchingRow(
    items: List<BaseItemDto>,
    imageRepo: ImageRepository,
    onItemClick: (String) -> Unit
) {
    val dims = LocalDimensions.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = dims.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.focusGroup()
    ) {
        items(items, key = { it.id }) { item ->
            val progress = item.userData?.playedPercentage?.let { it.toFloat() / 100f }
            val subtitle = buildSmartSubtitle(item)
            // Use series image for episodes, own image for movies
            val imageId = item.seriesId ?: item.id
            EditorialCard(
                title = item.seriesName ?: item.name ?: "",
                imageUrl = imageRepo.getThumbUrl(imageId),
                subtitle = subtitle,
                progress = progress,
                typeIcon = itemTypeIcon(item),
                onClick = { onItemClick(item.id.toString()) },
                cardWidth = 300.dp,
                imageHeight = 169.dp
            )
        }
    }
}

/**
 * Next Up — shows next unwatched episodes with "Next · S2, E5" style subtitle.
 */
@Composable
private fun NextUpRow(
    items: List<BaseItemDto>,
    imageRepo: ImageRepository,
    onItemClick: (String) -> Unit
) {
    val dims = LocalDimensions.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = dims.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.focusGroup()
    ) {
        items(items, key = { it.id }) { item ->
            val season = item.parentIndexNumber
            val episode = item.indexNumber
            val subtitle = if (season != null && episode != null) {
                "Next · S$season, E$episode"
            } else {
                item.seriesName
            }

            // Use series image for episodes
            val imageId = item.seriesId ?: item.id
            EditorialCard(
                title = item.seriesName ?: item.name ?: "",
                imageUrl = imageRepo.getThumbUrl(imageId),
                subtitle = subtitle,
                typeIcon = itemTypeIcon(item),
                onClick = { onItemClick(item.id.toString()) },
                cardWidth = 260.dp,
                imageHeight = 146.dp
            )
        }
    }
}

/**
 * Standard library media row.
 */
@Composable
private fun MediaRow(
    items: List<BaseItemDto>,
    imageRepo: ImageRepository,
    onItemClick: (String) -> Unit
) {
    val dims = LocalDimensions.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = dims.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.focusGroup()
    ) {
        items(items, key = { it.id }) { item ->
            val year = item.productionYear?.toString()
            val unplayed = item.userData?.unplayedItemCount ?: 0
            val badgeText = if (unplayed > 0) "$unplayed NEW" else null
            EditorialCard(
                title = item.name ?: "",
                imageUrl = imageRepo.getThumbUrl(item.id),
                subtitle = year,
                typeIcon = itemTypeIcon(item),
                onClick = { onItemClick(item.id.toString()) },
                badge = badgeText
            )
        }
    }
}

/**
 * Build a smart subtitle string for continue watching items.
 * Shows "S2:E5 · 23min left" for episodes, or year for movies.
 */
private fun buildSmartSubtitle(item: BaseItemDto): String {
    val parts = mutableListOf<String>()

    // Episode info
    val season = item.parentIndexNumber
    val episode = item.indexNumber
    if (season != null && episode != null) {
        parts.add("S$season:E$episode")
    }

    // Time remaining
    val totalTicks = item.runTimeTicks
    val playedPercent = item.userData?.playedPercentage
    if (totalTicks != null && playedPercent != null && playedPercent > 0) {
        val totalMin = totalTicks / 600_000_000
        val remainingMin = (totalMin * (100 - playedPercent) / 100).toLong()
        if (remainingMin > 0) {
            parts.add("${remainingMin}min left")
        }
    }

    if (parts.isNotEmpty()) return parts.joinToString(" · ")

    // Fallback: year
    return item.productionYear?.toString() ?: ""
}

private fun itemTypeIcon(item: BaseItemDto): ImageVector? = when (item.type) {
    BaseItemKind.MOVIE -> Icons.Outlined.Movie
    BaseItemKind.SERIES -> Icons.Outlined.Tv
    BaseItemKind.EPISODE -> Icons.Outlined.Tv
    else -> null
}

@Composable
private fun OfflineContent(
    items: List<com.example.switchstream.data.db.DownloadedMedia>,
    onItemClick: (String) -> Unit
) {
    val dims = LocalDimensions.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = dims.topBarClearance + 16.dp,
            bottom = 48.dp
        )
    ) {
        item {
            SectionHeader(title = "Available Offline")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = dims.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(items, key = { it.itemId }) { download ->
                    EditorialCard(
                        title = download.title,
                        imageUrl = download.thumbnailUrl,
                        subtitle = download.seriesName,
                        onClick = { onItemClick(download.itemId) }
                    )
                }
            }
        }
    }
}
