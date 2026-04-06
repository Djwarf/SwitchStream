package com.switchsides.switchstream.ui.screens.downloads

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.switchsides.switchstream.data.db.DownloadState
import com.switchsides.switchstream.data.db.DownloadedMedia
import com.switchsides.switchstream.ui.components.EmptyState
import com.switchsides.switchstream.ui.components.FocusableButton
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.ErrorRed
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.SuccessGreen
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.theme.TextSecondary

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onItemClick: (itemId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dims = LocalDimensions.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        if (uiState.downloads.isEmpty()) {
            EmptyState("No downloads yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = dims.topBarClearance + 16.dp,
                    start = dims.screenPadding,
                    end = dims.screenPadding,
                    bottom = 48.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(uiState.downloads, key = { it.itemId }) { download ->
                    DownloadRow(
                        download = download,
                        onClick = { onItemClick(download.itemId) },
                        onDelete = { viewModel.deleteDownload(download.itemId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    download: DownloadedMedia,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = download.thumbnailUrl,
            contentDescription = download.title,
            modifier = Modifier
                .size(80.dp, 50.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1
            )
            if (download.seriesName != null) {
                Text(
                    text = download.seriesName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = when (download.downloadState) {
                    DownloadState.QUEUED -> "Waiting..."
                    DownloadState.DOWNLOADING -> {
                        if (download.totalBytes > 0) {
                            val pct = (download.downloadedBytes * 100 / download.totalBytes).toInt()
                            "Downloading $pct%"
                        } else "Downloading..."
                    }
                    DownloadState.COMPLETE -> {
                        val mb = download.totalBytes / (1024 * 1024)
                        "Complete (${mb}MB)"
                    }
                    DownloadState.FAILED -> "Failed"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (download.downloadState) {
                    DownloadState.COMPLETE -> SuccessGreen
                    DownloadState.FAILED -> ErrorRed
                    else -> AccentBlue
                }
            )
            // Progress bar for active downloads
            if (download.downloadState == DownloadState.DOWNLOADING && download.totalBytes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { download.downloadedBytes.toFloat() / download.totalBytes.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentBlue,
                    trackColor = GlassBorder
                )
            } else if (download.downloadState == DownloadState.QUEUED) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentBlue,
                    trackColor = GlassBorder
                )
            }
        }

        // Status icon
        Icon(
            imageVector = when (download.downloadState) {
                DownloadState.COMPLETE -> Icons.Outlined.CheckCircle
                DownloadState.FAILED -> Icons.Outlined.Error
                else -> Icons.Outlined.CloudDownload
            },
            contentDescription = "Download status",
            tint = when (download.downloadState) {
                DownloadState.COMPLETE -> SuccessGreen
                DownloadState.FAILED -> ErrorRed
                else -> AccentBlue
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Delete button
        FocusableButton(
            text = "Delete",
            onClick = onDelete,
            isPrimary = false
        )
    }
}
