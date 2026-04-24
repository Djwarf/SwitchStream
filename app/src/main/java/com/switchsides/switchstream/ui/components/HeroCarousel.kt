package com.switchsides.switchstream.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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

    // Auto-advance
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
            // Parallax translates the backdrop image; without this clip the translated
            // image paints below the hero's bottom edge and bleeds into the Recently
            // Added row beneath it.
            .clipToBounds()
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(androidx.compose.animation.core.tween(400)) togetherWith
                    fadeOut(androidx.compose.animation.core.tween(400))
            },
            label = "hero_crossfade"
        ) { index ->
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
            val isFeatured = remember(item.id) { items.size > 1 }
            val slideLabel = remember(index, items.size) {
                "${(index + 1).toString().padStart(2, '0')} / ${items.size.toString().padStart(2, '0')}"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.heroHeight)
            ) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Image drifts at 45% of scroll speed — enough to register as
                            // depth, not so much it breaks framing. A subtle scale-up keeps
                            // the edges covered as the image is shifted.
                            val offset = parallaxOffsetProvider()
                            translationY = offset * 0.45f
                            val scaleBoost = (offset / 1000f).coerceIn(0f, 0.08f)
                            scaleX = 1f + scaleBoost
                            scaleY = 1f + scaleBoost
                        },
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

        // Persistent pagination rubric + auto-advance progress line. Lives outside
        // the AnimatedContent crossfade so it doesn't flicker per slide, and the
        // progress bar's target restart is keyed to currentIndex.
        if (items.size > 1 && dims.isTV) {
            val slideLabel = "${(currentIndex + 1).toString().padStart(2, '0')} / ${items.size.toString().padStart(2, '0')}"
            val progress = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(currentIndex) {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 8000,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                )
            }
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
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(1.5.dp)
                        .background(PureWhite.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.value)
                            .background(AccentBlue)
                    )
                }
            }
        }
    }
}
