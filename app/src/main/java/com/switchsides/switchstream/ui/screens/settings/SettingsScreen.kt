package com.switchsides.switchstream.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.components.FocusableButton
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.GlassSurfaceLight
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.PureWhite
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.theme.TextSecondary
import com.switchsides.switchstream.ui.theme.TextTertiary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSwitchServer: () -> Unit = {},
    onOfflineModeChanged: () -> Unit = {}
) {
    val dims = LocalDimensions.current
    val uiState by viewModel.uiState.collectAsState()
    val playback = uiState.playbackSettings

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val seekOptions = listOf(5, 10, 15, 30)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = dims.screenPadding,
            end = dims.screenPadding,
            top = dims.topBarClearance + 32.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Playback section title
        item {
            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        // Playback Speed
        item {
            SettingsTile(
                icon = Icons.Outlined.Speed,
                label = "Playback Speed",
                value = "${playback.defaultPlaybackSpeed}x",
                onClick = {
                    val idx = speedOptions.indexOf(playback.defaultPlaybackSpeed)
                    val next = if (idx >= 0) (idx + 1) % speedOptions.size else 0
                    viewModel.updatePlaybackSpeed(speedOptions[next])
                }
            )
        }

        // Seek Forward
        item {
            SettingsTile(
                icon = Icons.Outlined.FastForward,
                label = "Seek Forward",
                value = "${playback.seekForwardSeconds}s",
                onClick = {
                    val idx = seekOptions.indexOf(playback.seekForwardSeconds)
                    val next = if (idx >= 0) (idx + 1) % seekOptions.size else 0
                    viewModel.updateSeekForward(seekOptions[next])
                }
            )
        }

        // Seek Back
        item {
            SettingsTile(
                icon = Icons.Outlined.FastRewind,
                label = "Seek Back",
                value = "${playback.seekBackSeconds}s",
                onClick = {
                    val idx = seekOptions.indexOf(playback.seekBackSeconds)
                    val next = if (idx >= 0) (idx + 1) % seekOptions.size else 0
                    viewModel.updateSeekBack(seekOptions[next])
                }
            )
        }

        // Auto-play
        item {
            SettingsTile(
                icon = Icons.Outlined.SkipNext,
                label = "Auto-play Next Episode",
                value = if (playback.autoPlayNextEpisode) "On" else "Off",
                valueColor = if (playback.autoPlayNextEpisode) AccentBlue else TextSecondary,
                onClick = { viewModel.updateAutoPlayNext(!playback.autoPlayNextEpisode) }
            )
        }

        // Picture-in-Picture
        item {
            SettingsTile(
                icon = Icons.Outlined.PictureInPicture,
                label = "Picture-in-Picture",
                value = if (playback.pictureInPictureEnabled) "On" else "Off",
                valueColor = if (playback.pictureInPictureEnabled) AccentBlue else TextSecondary,
                onClick = { viewModel.updatePipEnabled(!playback.pictureInPictureEnabled) }
            )
        }

        // Subtitle section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Subtitles",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        item {
            val sizeOptions = listOf(12, 14, 16, 20, 24)
            val sizeLabels = listOf("Small", "Medium", "Default", "Large", "Extra Large")
            val currentIdx = sizeOptions.indexOf(playback.subtitleFontSize).coerceAtLeast(0)
            SettingsTile(
                icon = Icons.Outlined.FormatSize,
                label = "Font Size",
                value = sizeLabels[currentIdx],
                onClick = {
                    val next = (currentIdx + 1) % sizeOptions.size
                    viewModel.updateSubtitleFontSize(sizeOptions[next])
                }
            )
        }

        item {
            val opacityOptions = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
            val opacityLabels = listOf("Off", "25%", "50%", "75%", "100%")
            val currentIdx = opacityOptions.indexOf(playback.subtitleBackgroundOpacity).coerceAtLeast(0)
            SettingsTile(
                icon = Icons.Outlined.Opacity,
                label = "Background Opacity",
                value = opacityLabels[currentIdx],
                onClick = {
                    val next = (currentIdx + 1) % opacityOptions.size
                    viewModel.updateSubtitleBackgroundOpacity(opacityOptions[next])
                }
            )
        }

        item {
            SettingsTile(
                icon = Icons.Outlined.ScreenRotation,
                label = "Lock Landscape in Player",
                value = if (playback.lockLandscapeDuringPlayback) "On" else "Off",
                valueColor = if (playback.lockLandscapeDuringPlayback) AccentBlue else TextSecondary,
                onClick = { viewModel.updateLockLandscape(!playback.lockLandscapeDuringPlayback) }
            )
        }

        // Offline mode (mobile/tablet only)
        if (!dims.isTV) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            item {
                SettingsTile(
                    icon = Icons.Outlined.CloudOff,
                    label = "Offline Mode",
                    value = if (playback.offlineMode) "On" else "Off",
                    valueColor = if (playback.offlineMode) AccentBlue else TextSecondary,
                    onClick = {
                        viewModel.updateOfflineMode(!playback.offlineMode)
                        onOfflineModeChanged()
                    }
                )
            }
        }

        // Server section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Server",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        item {
            SettingsTile(
                icon = Icons.Outlined.Dns,
                label = "Switch Server",
                value = "",
                onClick = {
                    viewModel.switchServer()
                    onSwitchServer()
                }
            )
        }

        // About
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SwitchStream",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by Jellyfin",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GlassCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    valueColor: androidx.compose.ui.graphics.Color = TextSecondary
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) GlassSurfaceLight else GlassSurface)
            .then(
                if (isFocused) Modifier.border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                else Modifier
            )
            .selectable(selected = false, onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) TextPrimary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFocused) PureWhite else valueColor
        )
    }
}
