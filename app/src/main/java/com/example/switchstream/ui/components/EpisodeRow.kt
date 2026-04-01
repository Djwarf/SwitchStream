package com.example.switchstream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.animation.core.animateFloatAsState
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
    modifier: Modifier = Modifier,
    downloadState: com.example.switchstream.data.db.DownloadState? = null,
    onDownloadClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.03f else 1f)

    val episodeNumber = episode.indexNumber
    val runtimeMinutes = episode.runTimeTicks?.let { it / 600_000_000 }
    val playedPercentage = episode.userData?.playedPercentage
    val isPlayed = episode.userData?.played == true

    val dims = com.example.switchstream.ui.theme.LocalDimensions.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(dims.episodeRowHeight)
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
                    .width(dims.episodeThumbWidth)
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
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Episode number + runtime on one line
                val metaLine = buildString {
                    if (episodeNumber != null) append("E$episodeNumber")
                    if (runtimeMinutes != null) {
                        if (isNotEmpty()) append(" \u00b7 ")
                        append("${runtimeMinutes}min")
                    }
                }
                if (metaLine.isNotEmpty()) {
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentBlue,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = episode.name ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = if (dims.isTV) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!episode.overview.isNullOrEmpty() && dims.isTV) {
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

            // Download icon (mobile/tablet only)
            if (onDownloadClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 12.dp)
                        .size(32.dp)
                        .clickable { onDownloadClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (downloadState) {
                            com.example.switchstream.data.db.DownloadState.COMPLETE ->
                                androidx.compose.material.icons.Icons.Filled.Check
                            com.example.switchstream.data.db.DownloadState.DOWNLOADING,
                            com.example.switchstream.data.db.DownloadState.QUEUED ->
                                androidx.compose.material.icons.Icons.Outlined.CloudDownload
                            else ->
                                androidx.compose.material.icons.Icons.Outlined.CloudDownload
                        },
                        contentDescription = "Download episode",
                        tint = when (downloadState) {
                            com.example.switchstream.data.db.DownloadState.COMPLETE -> SuccessGreen
                            com.example.switchstream.data.db.DownloadState.DOWNLOADING,
                            com.example.switchstream.data.db.DownloadState.QUEUED -> AccentBlue.copy(alpha = 0.6f)
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
