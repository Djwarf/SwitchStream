package com.example.switchstream.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.PureWhite
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun HeroCarousel(
    items: List<BaseItemDto>,
    imageRepo: ImageRepository,
    onPlayClick: (itemId: String) -> Unit,
    onInfoClick: (itemId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

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
            .height(500.dp)
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(androidx.compose.animation.core.tween(800)) togetherWith
                    fadeOut(androidx.compose.animation.core.tween(800))
            },
            label = "hero_crossfade"
        ) { index ->
            val item = items[index]
            val itemId = item.id.toString()
            val backdropUrl = item.id?.let { imageRepo.getBackdropUrl(it) }
            val year = item.productionYear?.toString() ?: ""
            val rating = item.officialRating ?: ""
            val genres = item.genres?.take(2)?.joinToString(", ") ?: ""
            val metaItems = listOfNotNull(
                year.takeIf { it.isNotEmpty() },
                genres.takeIf { it.isNotEmpty() },
                rating.takeIf { it.isNotEmpty() }
            ).joinToString("     ")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                AsyncImage(
                    model = backdropUrl,
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
                                    0f to PureBlack.copy(alpha = 0.1f),
                                    0.4f to PureBlack.copy(alpha = 0.15f),
                                    0.7f to PureBlack.copy(alpha = 0.6f),
                                    1f to PureBlack.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.6f)
                        .padding(start = 56.dp, bottom = 80.dp)
                ) {
                    Text(
                        text = item.name ?: "",
                        style = MaterialTheme.typography.displayLarge,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (metaItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = metaItems,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPlayClick(itemId) },
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
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        FocusableButton(
                            text = "More Info",
                            onClick = { onInfoClick(itemId) },
                            isPrimary = false
                        )
                    }
                }
            }
        }

    }
}
