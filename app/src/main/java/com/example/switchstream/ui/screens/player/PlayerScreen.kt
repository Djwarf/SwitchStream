package com.example.switchstream.ui.screens.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Replay
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.switchstream.MainActivity
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.switchstream.ui.components.FocusableButton
import com.example.switchstream.ui.components.PlaybackSpeedSelector
import com.example.switchstream.ui.components.TrackSelectionDialog
import com.example.switchstream.ui.components.UpNextOverlay
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.Divider
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.OverlayBlack
import com.example.switchstream.ui.theme.SurfaceFocus
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val activity = LocalContext.current as? MainActivity

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (uiState.showControls) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.showControls()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        viewModel.seekBack()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        viewModel.seekForward()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (uiState.showTrackDialog != null || uiState.showSpeedDialog) {
                            viewModel.dismissDialogs()
                        } else if (activity?.enterPipIfEnabled() == true) {
                            // Entered PiP, don't navigate back
                        } else {
                            viewModel.stopPlayback()
                            onBack()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        viewModel.togglePlayPause()
                        true
                    }
                    KeyEvent.KEYCODE_CAPTIONS -> {
                        if (uiState.showTrackDialog == TrackDialogType.SUBTITLE) {
                            viewModel.dismissDialogs()
                        } else {
                            viewModel.showSubtitleDialog()
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom overlay controls
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OverlayBlack)
            ) {
                // Title at top
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(32.dp)
                )

                // Controls at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Track control buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Restart button
                        ControlIconButton(
                            icon = Icons.Filled.Replay,
                            contentDescription = "Restart",
                            onClick = { viewModel.seekTo(0) }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // CC button
                        ControlIconButton(
                            icon = Icons.Filled.ClosedCaption,
                            contentDescription = "Subtitles",
                            onClick = { viewModel.showSubtitleDialog() }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Audio button
                        ControlIconButton(
                            icon = Icons.Filled.Audiotrack,
                            contentDescription = "Audio Track",
                            onClick = { viewModel.showAudioDialog() }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Speed indicator
                        ControlTextButton(
                            text = "${uiState.playbackSpeed}x",
                            onClick = { viewModel.showSpeedDialog() }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive seek bar
                    androidx.compose.material3.Slider(
                        value = if (uiState.duration > 0) {
                            uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                        } else 0f,
                        onValueChange = { fraction ->
                            val seekPosition = (fraction * uiState.duration).toLong()
                            viewModel.seekTo(seekPosition)
                        },
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = AccentBlue,
                            activeTrackColor = AccentBlue,
                            inactiveTrackColor = Divider
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(uiState.currentPosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.isPlaying) "Playing" else "Paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentBlue
                        )
                        Text(
                            text = formatTime(uiState.duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Track selection dialogs
        when (uiState.showTrackDialog) {
            TrackDialogType.AUDIO -> {
                TrackSelectionDialog(
                    title = "Audio Track",
                    tracks = uiState.audioTracks,
                    selectedIndex = uiState.selectedAudioIndex,
                    onSelect = { index -> viewModel.selectAudioTrack(index) },
                    onDismiss = { viewModel.dismissDialogs() },
                    showNoneOption = false
                )
            }
            TrackDialogType.SUBTITLE -> {
                TrackSelectionDialog(
                    title = "Subtitles",
                    tracks = uiState.subtitleTracks,
                    selectedIndex = uiState.selectedSubtitleIndex,
                    onSelect = { index -> viewModel.selectSubtitleTrack(index) },
                    onDismiss = { viewModel.dismissDialogs() },
                    showNoneOption = true
                )
            }
            null -> { /* no dialog */ }
        }

        // Playback speed dialog
        if (uiState.showSpeedDialog) {
            PlaybackSpeedSelector(
                currentSpeed = uiState.playbackSpeed,
                onSelect = { speed -> viewModel.setPlaybackSpeed(speed) },
                onDismiss = { viewModel.dismissDialogs() }
            )
        }

        // Skip Intro button
        AnimatedVisibility(
            visible = uiState.showSkipIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 120.dp)
        ) {
            FocusableButton(
                text = "Skip Intro",
                onClick = { viewModel.skipIntro() }
            )
        }

        // Skip Credits button
        AnimatedVisibility(
            visible = uiState.showSkipCredits,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 120.dp)
        ) {
            FocusableButton(
                text = "Skip Credits",
                onClick = { viewModel.skipCredits() }
            )
        }

        // Up Next overlay
        if (uiState.showUpNext && uiState.nextEpisode != null) {
            val nextEp = uiState.nextEpisode!!
            UpNextOverlay(
                nextEpisode = nextEp,
                imageUrl = null,
                countdown = uiState.upNextCountdown,
                onPlayNow = { viewModel.playNextEpisode() },
                onCancel = { viewModel.cancelUpNext() },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier.focusable(),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = GlassSurface
        )
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun ControlTextButton(
    text: String,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier.focusable(),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = GlassSurface
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
