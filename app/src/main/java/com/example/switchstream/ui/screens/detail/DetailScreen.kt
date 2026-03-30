package com.example.switchstream.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.ui.components.EditorialCard
import com.example.switchstream.ui.components.EpisodeRow
import com.example.switchstream.ui.components.ErrorState
import com.example.switchstream.ui.components.FocusableButton
import com.example.switchstream.ui.components.LoadingIndicator
import com.example.switchstream.ui.components.PersonCard
import com.example.switchstream.ui.components.SeasonSelector
import com.example.switchstream.ui.components.SectionHeader
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.SuccessGreen
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PersonKind

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onPlayClick: (itemId: String) -> Unit,
    onPersonClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        when {
            uiState.isLoading -> LoadingIndicator()
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
                onPersonClick = onPersonClick
            )
        }
    }
}

@Composable
private fun DetailContent(
    uiState: DetailUiState,
    imageRepo: ImageRepository,
    onPlayClick: (String) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onPersonClick: (String) -> Unit
) {
    val item = uiState.item ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Backdrop with gradient overlay
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                AsyncImage(
                    model = imageRepo.getBackdropUrl(item.id),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to PureBlack.copy(alpha = 0f),
                                    0.2f to PureBlack.copy(alpha = 0.2f),
                                    0.85f to PureBlack.copy(alpha = 0.85f),
                                    1f to PureBlack
                                )
                            )
                        )
                )
            }
        }

        // Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp)
            ) {
                Text(
                    text = item.name ?: "",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata row
                Row {
                    item.productionYear?.let { year ->
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    item.runTimeTicks?.let { ticks ->
                        val minutes = ticks / 600_000_000
                        Text(
                            text = "  \u00B7${minutes}min",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    item.officialRating?.let { rating ->
                        Text(
                            text = "  \u00B7$rating",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    item.communityRating?.let { rating ->
                        Text(
                            text = "  \u00B7\u2606${"%.1f".format(rating)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentBlue
                        )
                    }
                    // Series info: season count
                    if (uiState.isSeries && uiState.seasons.isNotEmpty()) {
                        Text(
                            text = "  \u00B7${uiState.seasons.size} Season${if (uiState.seasons.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons row (Favorite + Watched)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite button
                    Surface(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(40.dp),
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
                        modifier = Modifier.size(40.dp),
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
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Overview
                item.overview?.let { overview ->
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
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
                    contentPadding = PaddingValues(horizontal = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres) { genre ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = GlassSurface,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
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
                    .padding(horizontal = 56.dp)
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
            itemsIndexed(uiState.episodes) { _, episode ->
                EpisodeRow(
                    episode = episode,
                    imageUrl = imageRepo.getPrimaryImageUrl(episode.id),
                    onClick = { onPlayClick(episode.id.toString()) },
                    modifier = Modifier.padding(horizontal = 56.dp, vertical = 4.dp)
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        } else {
            // Cast & crew
            val people = item.people.orEmpty()
            if (people.isNotEmpty()) {
                item {
                    SectionHeader(title = "Cast & Crew")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 56.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(people) { person ->
                            val personImageUrl = person.id?.let { personId ->
                                imageRepo.getPrimaryImageUrl(personId)
                            }
                            PersonCard(
                                name = person.name ?: "",
                                role = person.role,
                                imageUrl = personImageUrl,
                                onClick = { onPersonClick(person.name ?: "") }
                            )
                        }
                    }
                }
            }

            // Similar items
            if (uiState.similarItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "Similar")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 56.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(uiState.similarItems) { similarItem ->
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
    }
}
