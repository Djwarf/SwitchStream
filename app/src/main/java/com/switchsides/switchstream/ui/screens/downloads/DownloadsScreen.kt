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
                        onDelete = { viewModel.deleteDownload(download.itemId) },
                        onCancel = { viewModel.cancelDownload(download.itemId) },
                        onRetry = { viewModel.retryDownload(download) }
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
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = download.thumbnailUrl,
            contentDescription = download.title,
            modifier = Modifier
                .size(80.dp, 50.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

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
                text = statusLine(download),
                style = MaterialTheme.typography.labelSmall,
                color = when (download.downloadState) {
                    DownloadState.COMPLETE -> SuccessGreen
                    DownloadState.FAILED -> ErrorRed
                    else -> AccentBlue
                }
            )
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

        when (download.downloadState) {
            DownloadState.DOWNLOADING, DownloadState.QUEUED ->
                FocusableButton(text = "Cancel", onClick = onCancel, isPrimary = false)
            DownloadState.FAILED -> {
                FocusableButton(text = "Retry", onClick = onRetry, isPrimary = true)
                Spacer(modifier = Modifier.width(8.dp))
                FocusableButton(text = "Delete", onClick = onDelete, isPrimary = false)
            }
            DownloadState.COMPLETE ->
                FocusableButton(text = "Delete", onClick = onDelete, isPrimary = false)
        }
    }
}

private fun statusLine(d: DownloadedMedia): String = when (d.downloadState) {
    DownloadState.QUEUED -> "Waiting\u2026"
    DownloadState.DOWNLOADING -> {
        val pct = if (d.totalBytes > 0) (d.downloadedBytes * 100 / d.totalBytes).toInt() else 0
        val speed = formatSpeed(d.bytesPerSec)
        val eta = formatEta(d.totalBytes, d.downloadedBytes, d.bytesPerSec)
        val size = if (d.totalBytes > 0) "${formatBytes(d.downloadedBytes)} / ${formatBytes(d.totalBytes)}"
        else formatBytes(d.downloadedBytes)
        buildString {
            append("$pct%  \u00b7  $size  \u00b7  $speed")
            if (eta.isNotEmpty()) append("  \u00b7  ETA $eta")
        }
    }
    DownloadState.COMPLETE -> "Complete \u00b7 ${formatBytes(d.totalBytes)}"
    DownloadState.FAILED -> "Failed"
}

private fun formatBytes(b: Long): String {
    if (b <= 0) return "0 B"
    if (b < 1024) return "$b B"
    if (b < 1024 * 1024) return "${b / 1024} KB"
    if (b < 1024L * 1024 * 1024) return "%.1f MB".format(b / 1_048_576.0)
    return "%.2f GB".format(b / 1_073_741_824.0)
}

private fun formatSpeed(bps: Long): String {
    if (bps <= 0) return "\u2014"
    if (bps < 1024 * 1024) return "${bps / 1024} KB/s"
    return "%.1f MB/s".format(bps / 1_048_576.0)
}

private fun formatEta(total: Long, done: Long, bps: Long): String {
    if (total <= 0 || bps <= 0) return ""
    val remainingSec = ((total - done).coerceAtLeast(0L)) / bps
    if (remainingSec <= 0) return ""
    val hours = remainingSec / 3600
    val minutes = (remainingSec % 3600) / 60
    val seconds = remainingSec % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
