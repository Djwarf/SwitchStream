package com.switchsides.switchstream.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.ui.components.EditorialCard
import com.switchsides.switchstream.ui.components.EpisodeRow
import com.switchsides.switchstream.ui.components.ErrorState
import com.switchsides.switchstream.ui.components.FocusableButton
import com.switchsides.switchstream.ui.components.LoadingIndicator
import com.switchsides.switchstream.ui.components.ShimmerDetailScreen
import com.switchsides.switchstream.ui.components.PersonCard
import com.switchsides.switchstream.ui.components.SeasonSelector
import com.switchsides.switchstream.ui.components.SectionHeader
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.EditorialMono
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.SuccessGreen
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PersonKind

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onPlayClick: (itemId: String) -> Unit,
    onPersonClick: (personId: String, personName: String) -> Unit = { _, _ -> },
    onGenreClick: (genre: String) -> Unit = {},
    onSeriesClick: (seriesId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showBulkDownloadChooser by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.queuedToastEpisodes) {
        uiState.queuedToastEpisodes?.let { n ->
            val msg = if (n > 0) "Queued $n episode${if (n == 1) "" else "s"}" else "Nothing new to download"
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearQueuedToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        when {
            uiState.isLoading -> ShimmerDetailScreen()
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.refresh() }
            )
            uiState.item != null -> DetailContent(
                uiState = uiState,
                imageRepo = viewModel.imageRepo,
                onPlayClick = onPlayClick,
                onSeasonSelected = viewModel::selectSeason,
                onToggleFavorite = viewModel::toggleFavorite,
                onTogglePlayed = viewModel::togglePlayed,
                onToggleDownload = viewModel::toggleDownload,
                onBulkDownloadClick = { showBulkDownloadChooser = true },
                onDownloadEpisode = viewModel::downloadEpisode,
                onPersonClick = onPersonClick,
                onGenreClick = onGenreClick,
                onSeriesClick = onSeriesClick
            )
        }

        // Bulk download chooser (series only)
        if (showBulkDownloadChooser) {
            val epsInCurrentSeason = uiState.episodes.size
            val seasonName = uiState.seasons.getOrNull(uiState.selectedSeasonIndex)?.name ?: "This season"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.switchsides.switchstream.ui.theme.OverlayBlack)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showBulkDownloadChooser = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose what to download.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusableButton(
                            text = "Cancel",
                            onClick = { showBulkDownloadChooser = false },
                            isPrimary = false
                        )
                        FocusableButton(
                            text = "$seasonName ($epsInCurrentSeason)",
                            onClick = {
                                showBulkDownloadChooser = false
                                viewModel.downloadSeason()
                            },
                            isPrimary = false
                        )
                        FocusableButton(
                            text = "Entire series",
                            onClick = {
                                showBulkDownloadChooser = false
                                viewModel.downloadSeries()
                            }
                        )
                    }
                }
            }
        }

        // Delete download confirmation dialog
        if (uiState.showDeleteConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.switchsides.switchstream.ui.theme.OverlayBlack)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { viewModel.dismissDeleteConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Delete Download?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = com.switchsides.switchstream.ui.theme.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will remove the downloaded file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.switchsides.switchstream.ui.theme.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusableButton(
                            text = "Cancel",
                            onClick = { viewModel.dismissDeleteConfirm() },
                            isPrimary = false
                        )
                        FocusableButton(
                            text = "Delete",
                            onClick = { viewModel.confirmDeleteDownload() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailContent(
    uiState: DetailUiState,
    imageRepo: ImageRepository,
    onPlayClick: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onToggleDownload: () -> Unit,
    onBulkDownloadClick: () -> Unit,
    onDownloadEpisode: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit,
    onPersonClick: (personId: String, personName: String) -> Unit,
    onGenreClick: (genre: String) -> Unit,
    onSeriesClick: (String) -> Unit
) {
    val dims = LocalDimensions.current
    val item = uiState.item ?: return

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        // Ambient backdrop — blurred hero image with a long vertical gradient.
        // Also the destination for the shared-element transition from card thumb.
        val backdropUrl = androidx.compose.runtime.remember(item.id) {
            imageRepo.getBackdropUrl(item.id)
        }
        val backdropHeight = dims.backdropHeight + 200.dp

        val sharedScope = com.switchsides.switchstream.ui.util.LocalSharedTransitionScope.current
        val animatedScope = com.switchsides.switchstream.ui.util.LocalAnimatedContentScope.current
        @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
        val sharedBoundsModifier = if (sharedScope != null && animatedScope != null) {
            with(sharedScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(
                        key = com.switchsides.switchstream.ui.util.sharedItemKey(item.id)
                    ),
                    animatedVisibilityScope = animatedScope,
                    resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.ScaleToBounds()
                )
            }
        } else Modifier

        coil.compose.AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(backdropHeight)
                .then(sharedBoundsModifier)
                .then(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Modifier.blur(radius = 48.dp)
                    } else Modifier
                )
                .graphicsLayer { alpha = 0.55f },
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(backdropHeight)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to PureBlack.copy(alpha = 0.2f),
                            0.45f to PureBlack.copy(alpha = 0.55f),
                            0.85f to PureBlack.copy(alpha = 0.9f),
                            1f to PureBlack
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        // Compact header: poster + info
        item {
            Spacer(modifier = Modifier.height(dims.topBarClearance + 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Poster
                AsyncImage(
                    model = imageRepo.getPrimaryImageUrl(item.id),
                    contentDescription = item.name,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                // Info column
                Column(modifier = Modifier.weight(1f)) {
                    // Editorial eyebrow — FILM / SERIES / EPISODE, or the parent series link.
                    if (uiState.isEpisode && !uiState.parentSeriesId.isNullOrEmpty() && !uiState.parentSeriesName.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSeriesClick(uiState.parentSeriesId) }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(2.dp)
                                    .background(AccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.parentSeriesName.uppercase(),
                                style = EditorialRowLabel,
                                color = AccentBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        val kindLabel = when {
                            uiState.isSeries -> "SERIES"
                            uiState.isEpisode -> "EPISODE"
                            else -> "FILM"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(2.dp)
                                    .background(AccentBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = kindLabel,
                                style = EditorialRowLabel,
                                color = AccentBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = item.name ?: "",
                        style = if (dims.isTV) MaterialTheme.typography.displayMedium
                               else MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        maxLines = 2
                    )

                    // Episode S/E label
                    if (uiState.isEpisode) {
                        val s = item.parentIndexNumber
                        val e = item.indexNumber
                        if (s != null && e != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "S%02d \u00B7 E%02d".format(s, e),
                                style = EditorialMono,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metadata row — monospace editorial strip
                    val metadataParts = buildList {
                        item.productionYear?.let { add(it.toString()) }
                        item.runTimeTicks?.let { ticks ->
                            val minutes = ticks / 600_000_000
                            add("${minutes}min")
                        }
                        item.officialRating?.let { add(it) }
                    }
                    if (metadataParts.isNotEmpty()) {
                        Text(
                            text = metadataParts.joinToString("  \u00B7  "),
                            style = EditorialMono,
                            color = TextSecondary
                        )
                    }

                    item.communityRating?.let { rating ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\u2606 ${"%.1f".format(rating)}",
                            style = EditorialMono,
                            color = AccentBlue
                        )
                    }

                    if (uiState.isSeries && uiState.seasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${uiState.seasons.size} SEASON${if (uiState.seasons.size != 1) "S" else ""}",
                            style = EditorialRowLabel,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play button
                    if (uiState.isSeries) {
                        val nextUp = uiState.nextUpEpisode
                        val playText = if (nextUp != null) {
                            val s = nextUp.parentIndexNumber ?: 1
                            val e = nextUp.indexNumber ?: 1
                            "Resume S${s}:E${e}"
                        } else {
                            "Play S1:E1"
                        }
                        val playId = if (nextUp != null) {
                            nextUp.id.toString()
                        } else {
                            uiState.episodes.firstOrNull()?.id?.toString() ?: item.id.toString()
                        }
                        FocusableButton(
                            text = playText,
                            onClick = { onPlayClick(playId) }
                        )
                    } else {
                        FocusableButton(
                            text = "Play",
                            onClick = { onPlayClick(item.id.toString()) }
                        )
                    }
                }
            }
        }

        // Action buttons row
        item {
            Row(
                modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Action buttons row (Favorite + Watched)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite button
                    Surface(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onToggleFavorite() },
                        shape = ClickableSurfaceDefaults.shape(
                            shape = RoundedCornerShape(12.dp)
                        ),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = GlassSurface,
                            focusedContainerColor = GlassSurface
                        ),
                        border = ClickableSurfaceDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(12.dp)
                            )
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (uiState.isFavorite) AccentBlue else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Watched toggle button
                    Surface(
                        onClick = onTogglePlayed,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onTogglePlayed() },
                        shape = ClickableSurfaceDefaults.shape(
                            shape = RoundedCornerShape(12.dp)
                        ),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = GlassSurface,
                            focusedContainerColor = GlassSurface
                        ),
                        border = ClickableSurfaceDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(12.dp)
                            )
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlayed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = if (uiState.isPlayed) "Mark as unwatched" else "Mark as watched",
                                tint = if (uiState.isPlayed) SuccessGreen else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // Download button with progress (mobile/tablet only)
                    if (!com.switchsides.switchstream.ui.theme.LocalDimensions.current.isTV) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable { if (uiState.isSeries) onBulkDownloadClick() else onToggleDownload() },
                            contentAlignment = Alignment.Center
                        ) {
                            // Circular progress behind icon when downloading
                            if (uiState.downloadState == com.switchsides.switchstream.data.db.DownloadState.DOWNLOADING ||
                                uiState.downloadState == com.switchsides.switchstream.data.db.DownloadState.QUEUED
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    progress = { uiState.downloadProgress },
                                    modifier = Modifier.size(34.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.5.dp,
                                    trackColor = GlassBorder
                                )
                            }
                            Icon(
                                imageVector = when (uiState.downloadState) {
                                    com.switchsides.switchstream.data.db.DownloadState.COMPLETE -> Icons.Filled.CheckCircle
                                    com.switchsides.switchstream.data.db.DownloadState.DOWNLOADING -> Icons.Outlined.CloudDownload
                                    com.switchsides.switchstream.data.db.DownloadState.QUEUED -> Icons.Outlined.CloudDownload
                                    else -> Icons.Outlined.CloudDownload
                                },
                                contentDescription = "Download",
                                tint = when (uiState.downloadState) {
                                    com.switchsides.switchstream.data.db.DownloadState.COMPLETE -> AccentBlue
                                    com.switchsides.switchstream.data.db.DownloadState.DOWNLOADING,
                                    com.switchsides.switchstream.data.db.DownloadState.QUEUED -> AccentBlue.copy(alpha = 0.7f)
                                    else -> TextSecondary
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Overview section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenPadding)
            ) {
                // Tagline — italic serif pull quote, editorial subhead.
                if (!item.taglines.isNullOrEmpty()) {
                    Text(
                        text = "\u201C${item.taglines!!.first()}\u201D",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Overview (expandable) — editorial measure, widened leading.
                item.overview?.let { overview ->
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = overview,
                        style = com.switchsides.switchstream.ui.theme.EditorialBody,
                        color = TextPrimary,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 640.dp)
                    )
                    if (overview.length > 200) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.clickable { expanded = !expanded },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = GlassSurface,
                                focusedContainerColor = GlassSurface
                            )
                        ) {
                            Text(
                                text = if (expanded) "Show less" else "Show more",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Genres as pill chips
                val genres = item.genres.orEmpty()
                if (genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Genres LazyRow (separate item to allow horizontal scrolling)
        val genres = item.genres.orEmpty()
        if (genres.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.focusGroup(),
                    contentPadding = PaddingValues(horizontal = dims.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres, key = { it }) { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                .clickable { onGenreClick(genre) }
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentBlue,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Director and Studio info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenPadding)
            ) {
                // Director
                val directors = item.people.orEmpty().filter { it.type == PersonKind.DIRECTOR }
                if (directors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Directed by ${directors.joinToString(", ") { it.name ?: "" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                // Studios
                val studios = item.studios.orEmpty()
                if (studios.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Studio: ${studios.joinToString(", ") { it.name ?: "" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (uiState.isSeries) {
            // Season selector
            item {
                SeasonSelector(
                    seasons = uiState.seasons,
                    selectedIndex = uiState.selectedSeasonIndex,
                    onSeasonSelected = onSeasonSelected,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Episode list
            itemsIndexed(uiState.episodes, key = { _, ep -> ep.id }) { _, episode ->
                EpisodeRow(
                    episode = episode,
                    imageUrl = imageRepo.getEpisodeThumbUrl(episode.id),
                    onClick = { onPlayClick(episode.id.toString()) },
                    modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 4.dp),
                    downloadState = uiState.episodeDownloadStates[episode.id.toString()],
                    onDownloadClick = if (!dims.isTV) { { onDownloadEpisode(episode) } } else null
                )
            }
        }

        // Cast & crew (for both movies and series)
        val people = item.people.orEmpty()
        if (people.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Cast & Crew")
            }
            item {
                LazyRow(
                    modifier = Modifier.focusGroup(),
                    contentPadding = PaddingValues(horizontal = dims.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(people, key = { it.id ?: it.hashCode() }) { person ->
                        val personImageUrl = person.id?.let { personId ->
                            imageRepo.getPrimaryImageUrl(personId)
                        }
                        PersonCard(
                            name = person.name ?: "",
                            role = person.role,
                            imageUrl = personImageUrl,
                            onClick = { onPersonClick(person.id?.toString() ?: "", person.name ?: "") }
                        )
                    }
                }
            }
        }

        // Similar items (movies only)
        if (!uiState.isSeries && uiState.similarItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "Similar")
            }
            item {
                LazyRow(
                    modifier = Modifier.focusGroup(),
                    contentPadding = PaddingValues(horizontal = dims.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(uiState.similarItems, key = { it.id }) { similarItem ->
                        EditorialCard(
                            title = similarItem.name ?: "",
                            imageUrl = imageRepo.getPrimaryImageUrl(similarItem.id),
                            onClick = { onPlayClick(similarItem.id.toString()) },
                            subtitle = similarItem.productionYear?.toString()
                        )
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    } // end ambient-backdrop Box
}
