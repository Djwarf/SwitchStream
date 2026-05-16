package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.EditorialMono
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.PureWhite
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun HeroCarousel(
    items: List<BaseItemDto>,
    imageRepo: ImageRepository,
    onPlayClick: (itemId: String) -> Unit,
    onInfoClick: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    parallaxOffsetProvider: () -> Float = { 0f }
) {
    if (items.isEmpty()) return

    val dims = LocalDimensions.current
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(8000L)
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dims.heroHeight)
    ) {
        run {
            val index = currentIndex.coerceIn(0, items.lastIndex)
            val item = items[index]
            val itemId = remember(item.id) { item.id.toString() }
            val backdropUrl = remember(item.id) { item.id?.let { imageRepo.getBackdropUrl(it) } }
            val metaItems = remember(item.productionYear, item.officialRating, item.genres) {
                val year = item.productionYear?.toString() ?: ""
                val rating = item.officialRating ?: ""
                val genres = item.genres?.take(2)?.joinToString(" / ") ?: ""
                listOfNotNull(
                    year.takeIf { it.isNotEmpty() },
                    rating.takeIf { it.isNotEmpty() },
                    genres.takeIf { it.isNotEmpty() }
                ).joinToString("  ·  ")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.heroHeight)
            ) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Left-to-right darkening behind the copy column, stacked with the
                // bottom-up gradient. Gives the title side cinematic weight without
                // dimming the image's focal point (usually right of center).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to PureBlack.copy(alpha = 0.75f),
                                    0.45f to PureBlack.copy(alpha = 0.25f),
                                    1f to PureBlack.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to PureBlack.copy(alpha = 0.15f),
                                    0.5f to PureBlack.copy(alpha = 0.2f),
                                    0.78f to PureBlack.copy(alpha = 0.72f),
                                    1f to PureBlack
                                )
                            )
                        )
                        .filmGrain(alpha = 0.025f)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(if (dims.isTV) 0.6f else 0.9f)
                        .padding(start = dims.screenPadding, bottom = if (dims.isTV) 80.dp else 24.dp)
                ) {
                    // Eyebrow — small, wide-tracked, above the title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(AccentBlue)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "FEATURED",
                            style = EditorialRowLabel,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (dims.isTV) 14.dp else 8.dp))

                    Text(
                        text = item.name ?: "",
                        style = if (dims.isTV) MaterialTheme.typography.displayLarge
                               else MaterialTheme.typography.headlineLarge,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (metaItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = metaItems,
                            style = EditorialMono,
                            color = PureWhite.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPlayClick(itemId) },
                            modifier = Modifier.clickable { onPlayClick(itemId) },
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            colors = ButtonDefaults.colors(
                                containerColor = PureWhite,
                                contentColor = PureBlack,
                                focusedContainerColor = PureWhite.copy(alpha = 0.85f),
                                focusedContentColor = PureBlack
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    horizontal = if (dims.isTV) 20.dp else 12.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(if (dims.isTV) 22.dp else 18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Play",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                            }
                        }

                        FocusableButton(
                            text = "Info",
                            onClick = { onInfoClick(itemId) },
                            isPrimary = false,
                            stretchContent = false
                        )
                    }
                }
            }
        }

        // Pagination rubric with static dots reflecting the current slide.
        if (items.size > 1 && dims.isTV) {
            val slideLabel = "${(currentIndex + 1).toString().padStart(2, '0')} / ${items.size.toString().padStart(2, '0')}"
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = dims.screenPadding)
            ) {
                Text(
                    text = slideLabel,
                    style = EditorialMono,
                    color = PureWhite.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    repeat(items.size) { i ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentIndex) AccentBlue
                                    else PureWhite.copy(alpha = 0.18f)
                                )
                        )
                    }
                }
            }
        }
    }
}
