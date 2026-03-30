package com.example.switchstream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.Divider
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.PureWhite
import com.example.switchstream.ui.theme.SuccessGreen
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.GlassSurfaceLight
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun EpisodeRow(
    episode: BaseItemDto,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val episodeNumber = episode.indexNumber
    val runtimeMinutes = episode.runTimeTicks?.let { it / 600_000_000 }
    val playedPercentage = episode.userData?.playedPercentage
    val isPlayed = episode.userData?.played == true

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = if (isFocused) 1.03f else 1f
                scaleY = if (isFocused) 1.03f else 1f
            },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = GlassSurfaceLight
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(1.5.dp, PureWhite.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                    contentScale = ContentScale.Crop
                )

                // Watched checkmark
                if (isPlayed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuccessGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Watched",
                            tint = PureBlack,
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                // Progress bar
                if (playedPercentage != null && playedPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Divider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (playedPercentage / 100.0).toFloat())
                                .background(AccentBlue)
                        )
                    }
                }
            }

            // Episode info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (episodeNumber != null) {
                    Text(
                        text = "E$episodeNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = episode.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (runtimeMinutes != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${runtimeMinutes} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                if (!episode.overview.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = episode.overview ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
