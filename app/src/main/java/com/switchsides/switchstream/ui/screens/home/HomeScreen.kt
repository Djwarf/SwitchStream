package com.switchsides.switchstream.ui.screens.home

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.ui.components.EditorialCard
import com.switchsides.switchstream.ui.components.filmGrain
import com.switchsides.switchstream.ui.components.EmptyState
import com.switchsides.switchstream.ui.components.ErrorState
import com.switchsides.switchstream.ui.components.HeroCarousel
import com.switchsides.switchstream.ui.components.LoadingIndicator
import com.switchsides.switchstream.ui.components.ShimmerHomeScreen
import com.switchsides.switchstream.ui.components.SectionHeader
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.PureBlack
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

    val lifecycleOwner = LocalLifecycleOwner.current
    val firstResume = remember { arrayOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume[0]) {
                    firstResume[0] = false
                } else {
                    viewModel.refresh()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
            .then(if (dims.isTV) Modifier.filmGrain(alpha = 0.022f) else Modifier)
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
                        isRefreshing = uiState.isRefreshing,
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

        // Thin refresh bar for TV (mobile uses PullToRefreshBox's built-in indicator).
        if (dims.isTV && uiState.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
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

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // Parallax driver — how far the hero has been scrolled. Zero when hero is the
    // first visible item; caps at its height once the hero is offscreen (below).
    val parallaxScrollPx by androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else 0f
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Hero Carousel — appears first.
        if (uiState.featuredItems.isNotEmpty()) {
            item(key = "hero_carousel") {
                HeroCarousel(
                    items = uiState.featuredItems,
                    imageRepo = imageRepo,
                    onPlayClick = onItemClick,
                    onInfoClick = onItemClick,
                    parallaxOffsetProvider = { parallaxScrollPx }
                )
            }
        }

        // Recently Added
        if (uiState.recentlyAdded.isNotEmpty()) {
            item {
                androidx.compose.foundation.layout.Column {
                    SectionHeader(title = "Recently Added")
                    MediaRow(
                        items = uiState.recentlyAdded,
                        imageRepo = imageRepo,
                        onItemClick = onItemClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Continue Watching
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                androidx.compose.foundation.layout.Column {
                    SectionHeader(title = "Continue Watching")
                    ContinueWatchingRow(
                        items = uiState.continueWatching,
                        imageRepo = imageRepo,
                        onItemClick = onItemClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Next Up
        if (uiState.nextUp.isNotEmpty()) {
            item {
                androidx.compose.foundation.layout.Column {
                    SectionHeader(title = "Next Up")
                    NextUpRow(
                        items = uiState.nextUp,
                        imageRepo = imageRepo,
                        onItemClick = onItemClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // My Favorites
        if (uiState.favorites.isNotEmpty()) {
            item {
                androidx.compose.foundation.layout.Column {
                    SectionHeader(title = "My Favorites")
                    MediaRow(
                        items = uiState.favorites,
                        imageRepo = imageRepo,
                        onItemClick = onItemClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Per-library rows
        for (library in uiState.libraries) {
            val libraryItems = uiState.latestByLibrary[library.id].orEmpty()
            if (libraryItems.isNotEmpty()) {
                item {
                    androidx.compose.foundation.layout.Column {
                        SectionHeader(title = library.name ?: "Library")
                        MediaRow(
                            items = libraryItems,
                            imageRepo = imageRepo,
                            onItemClick = onItemClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // "Because you watched" recommendations
        for (row in uiState.recommendations) {
            item {
                androidx.compose.foundation.layout.Column {
                    SectionHeader(title = "Because you watched ${row.sourceTitle}")
                    MediaRow(
                        items = row.items,
                        imageRepo = imageRepo,
                        onItemClick = onItemClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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
            // For episodes, show the parent series (poster + name) and route to the series detail.
            val targetId = item.seriesId ?: item.id
            EditorialCard(
                title = item.seriesName ?: item.name ?: "",
                imageUrl = imageRepo.getThumbUrl(targetId),
                subtitle = year,
                typeIcon = itemTypeIcon(item),
                onClick = { onItemClick(targetId.toString()) },
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
    items: List<com.switchsides.switchstream.data.db.DownloadedMedia>,
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
