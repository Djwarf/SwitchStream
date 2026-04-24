package com.switchsides.switchstream.ui.screens.player

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.switchsides.switchstream.MainActivity
import com.switchsides.switchstream.ui.components.ResumeDialog
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.DeviceType
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.components.FocusableButton
import com.switchsides.switchstream.ui.components.PlaybackSpeedSelector
import com.switchsides.switchstream.ui.components.QualitySelector
import com.switchsides.switchstream.ui.components.SleepTimerChoice
import com.switchsides.switchstream.ui.components.SleepTimerSelector
import com.switchsides.switchstream.ui.components.TrackSelectionDialog
import com.switchsides.switchstream.ui.components.UpNextOverlay
import com.switchsides.switchstream.ui.components.qualityLabel
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.Divider
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.OverlayBlack
import com.switchsides.switchstream.ui.theme.EditorialMono
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.PureWhite
import com.switchsides.switchstream.ui.theme.SurfaceFocus
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.theme.TextSecondary

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val bottomRowFocusRequester = remember { FocusRequester() }
    val skipIntroFocusRequester = remember { FocusRequester() }
    val skipCreditsFocusRequester = remember { FocusRequester() }
    var rootHasFocus by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val activity = context as? MainActivity
    val dims = LocalDimensions.current
    var seekIndicator by remember { mutableStateOf<String?>(null) }
    var verticalIndicator by remember { mutableStateOf<VerticalAdjust?>(null) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val isInPip = activity?.isInPipState?.value == true

    val anyDialogOpen = uiState.showTrackDialog != null
        || uiState.showSpeedDialog
        || uiState.showSleepTimerDialog
        || uiState.showQualityDialog
        || uiState.showResumePrompt
        || uiState.showUpNext

    // Claim focus on entry so d-pad reaches our key handler immediately.
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // When controls auto-hide (or are dismissed), pull focus back to the root so the
    // user can immediately seek/play with d-pad — but only when no overlay is open,
    // otherwise we'd steal focus from a dialog the user is interacting with.
    LaunchedEffect(uiState.showControls, anyDialogOpen) {
        if (!uiState.showControls && !anyDialogOpen) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Auto-focus transient overlays so the user can press CENTER without navigating.
    LaunchedEffect(uiState.showSkipIntro) {
        if (uiState.showSkipIntro) runCatching { skipIntroFocusRequester.requestFocus() }
    }
    LaunchedEffect(uiState.showSkipCredits) {
        if (uiState.showSkipCredits) runCatching { skipCreditsFocusRequester.requestFocus() }
    }

    // Push current video aspect ratio into the activity so PiP is sized correctly.
    DisposableEffect(viewModel.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                val w = videoSize.width
                val h = videoSize.height
                if (w > 0 && h > 0) {
                    activity?.currentVideoAspect = android.util.Rational(w, h)
                }
            }
        }
        viewModel.player.addListener(listener)
        // Seed immediately in case the video was already prepared before we attached.
        val initial = viewModel.player.videoSize
        if (initial.width > 0 && initial.height > 0) {
            activity?.currentVideoAspect = android.util.Rational(initial.width, initial.height)
        }
        onDispose {
            viewModel.player.removeListener(listener)
            activity?.currentVideoAspect = null
        }
    }
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Immersive fullscreen — hide system bars during playback
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(
            (view.context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose {},
            view
        )
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Keep the screen awake only while actually playing; clear the flag on pause or exit.
    DisposableEffect(uiState.isPlaying) {
        val window = (view.context as? android.app.Activity)?.window
        if (uiState.isPlaying) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-hide seek indicator
    LaunchedEffect(seekIndicator) {
        if (seekIndicator != null) {
            kotlinx.coroutines.delay(600)
            seekIndicator = null
        }
    }

    // Auto-hide brightness/volume indicator
    LaunchedEffect(verticalIndicator) {
        if (verticalIndicator != null) {
            kotlinx.coroutines.delay(800)
            verticalIndicator = null
        }
    }

    // Restore system-default brightness when leaving the player so per-app dim doesn't leak.
    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? android.app.Activity)?.window ?: return@onDispose
            window.attributes = window.attributes.apply { screenBrightness = -1f }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .then(
                if (!dims.isTV && !uiState.showResumePrompt) {
                    // Mobile: double-tap sides to seek, single tap to toggle controls,
                    // vertical swipe on left half = brightness, right half = volume.
                    // Disabled when resume dialog is showing so dialog buttons are tappable.
                    Modifier
                        .pointerInput(uiState.showResumePrompt) {
                            detectTapGestures(
                                onTap = {
                                    if (uiState.showControls) viewModel.hideControls()
                                    else viewModel.showControls()
                                },
                                onDoubleTap = { offset ->
                                    val halfWidth = size.width / 2
                                    if (offset.x < halfWidth) {
                                        viewModel.seekBack()
                                        seekIndicator = "-10s"
                                    } else {
                                        viewModel.seekForward()
                                        seekIndicator = "+10s"
                                    }
                                    viewModel.showControls()
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            var startSide: DragSide? = null
                            var fraction = 0f
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    val halfWidth = size.width / 2f
                                    val side = if (offset.x < halfWidth) DragSide.LEFT else DragSide.RIGHT
                                    startSide = side
                                    fraction = when (side) {
                                        DragSide.LEFT -> activity?.window?.let { readBrightnessFraction(it, context) } ?: 0.5f
                                        DragSide.RIGHT -> readVolumeFraction(audioManager)
                                    }
                                },
                                onVerticalDrag = { _, dy ->
                                    val side = startSide ?: return@detectVerticalDragGestures
                                    fraction = (fraction - dy / size.height).coerceIn(0f, 1f)
                                    when (side) {
                                        DragSide.LEFT -> activity?.window?.let { applyBrightness(it, fraction) }
                                        DragSide.RIGHT -> applyVolume(audioManager, fraction)
                                    }
                                    verticalIndicator = VerticalAdjust(
                                        kind = if (side == DragSide.LEFT) VerticalAdjust.Kind.BRIGHTNESS
                                               else VerticalAdjust.Kind.VOLUME,
                                        fraction = fraction
                                    )
                                },
                                onDragEnd = { startSide = null },
                                onDragCancel = { startSide = null }
                            )
                        }
                } else {
                    // TV: root focusable + d-pad handler below covers everything.
                    // No clickable here — TV has no touch and an extra clickable would
                    // create a phantom focus target that competes with the focusable root.
                    Modifier
                }
            )
            .focusRequester(focusRequester)
            .onFocusChanged { state -> rootHasFocus = state.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                val controlsVisible = uiState.showControls
                // When an overlay/dialog is open, all directional keys (including CENTER)
                // must pass through so the dialog can navigate its own list and the user
                // can press CENTER to select. The dialog handles BACK on its own too,
                // but we also handle BACK here as a safety net for dismissDialogs().
                // Outside dialogs, when a focusable child (bottom row button, skip
                // overlay) holds focus, let directional keys propagate to TV focus
                // traversal instead of hijacking them at the root.
                val passThroughDirectional = anyDialogOpen || (controlsVisible && !rootHasFocus)

                // Any d-pad or ENTER press while controls are up should reset the
                // auto-hide timer — including events we're about to pass through to
                // focus traversal, so navigating the bottom row keeps the chrome alive.
                val isInteractionKey = when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                    else -> false
                }
                if (isInteractionKey && controlsVisible && !anyDialogOpen) {
                    viewModel.showControls()
                }
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (passThroughDirectional) return@onPreviewKeyEvent false
                        viewModel.togglePlayPause()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (passThroughDirectional) return@onPreviewKeyEvent false
                        viewModel.seekBack()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (passThroughDirectional) return@onPreviewKeyEvent false
                        viewModel.seekForward()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (passThroughDirectional) return@onPreviewKeyEvent false
                        viewModel.showControls()
                        runCatching { bottomRowFocusRequester.requestFocus() }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (passThroughDirectional) return@onPreviewKeyEvent false
                        if (controlsVisible && !rootHasFocus) {
                            // Bring focus back to the video surface from a control button.
                            viewModel.showControls()
                            runCatching { focusRequester.requestFocus() }
                        } else {
                            viewModel.showControls()
                            runCatching { bottomRowFocusRequester.requestFocus() }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        when {
                            uiState.showTrackDialog != null
                                || uiState.showSpeedDialog
                                || uiState.showSleepTimerDialog
                                || uiState.showQualityDialog -> {
                                viewModel.dismissDialogs()
                                runCatching { focusRequester.requestFocus() }
                            }
                            uiState.showUpNext -> {
                                viewModel.cancelUpNext()
                                runCatching { focusRequester.requestFocus() }
                            }
                            controlsVisible -> {
                                viewModel.hideControls()
                                runCatching { focusRequester.requestFocus() }
                            }
                            activity?.enterPipIfEnabled() == true -> {
                                // Entered PiP, don't navigate back
                            }
                            else -> {
                                viewModel.stopPlayback()
                                onBack()
                            }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        viewModel.togglePlayPause()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        if (!uiState.isPlaying) viewModel.togglePlayPause()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        if (uiState.isPlaying) viewModel.togglePlayPause()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        viewModel.seekForward()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        viewModel.seekBack()
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (uiState.nextEpisode != null) viewModel.playNextEpisode()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        viewModel.seekTo(0)
                        viewModel.showControls()
                        true
                    }
                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        viewModel.stopPlayback()
                        onBack()
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
                    this.resizeMode = resizeMode
                    playerViewRef = this
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Hide all overlays in PiP mode — just show video
        if (!isInPip) {

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
                // Title at top — eyebrow + serif headline
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .background(AccentBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOW PLAYING",
                            style = EditorialRowLabel,
                            color = com.switchsides.switchstream.ui.theme.PureWhite.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2
                    )
                }

                // Center play/pause button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(com.switchsides.switchstream.ui.theme.PureWhite.copy(alpha = 0.2f))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = com.switchsides.switchstream.ui.theme.PureWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }

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
                        if (uiState.previousEpisode != null) {
                            ControlIconButton(
                                icon = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Episode",
                                onClick = { viewModel.playPreviousEpisode() }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        if (uiState.nextEpisode != null) {
                            ControlIconButton(
                                icon = Icons.Filled.SkipNext,
                                contentDescription = "Next Episode",
                                onClick = { viewModel.playNextEpisode() }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // bottomRowFocusRequester is pinned to Restart so d-pad DOWN from
                        // the video surface lands here regardless of whether the skip
                        // buttons are present.
                        ControlIconButton(
                            icon = Icons.Filled.Replay,
                            contentDescription = "Restart",
                            onClick = { viewModel.seekTo(0) },
                            modifier = Modifier.focusRequester(bottomRowFocusRequester)
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

                        Spacer(modifier = Modifier.width(16.dp))

                        // Sleep timer
                        ControlIconButton(
                            icon = Icons.Filled.Bedtime,
                            contentDescription = "Sleep Timer",
                            onClick = { viewModel.showSleepTimerDialog() }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Quality
                        ControlTextButton(
                            text = qualityLabel(uiState.streamingQuality),
                            onClick = { viewModel.showQualityDialog() }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Scale/aspect ratio toggle
                        ControlIconButton(
                            icon = when (resizeMode) {
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Filled.FitScreen
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> Icons.Filled.Fullscreen
                                else -> Icons.Filled.AspectRatio
                            },
                            contentDescription = when (resizeMode) {
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
                                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
                                else -> "Zoom"
                            },
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT ->
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL ->
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    else ->
                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seek bar — interactive Slider on touch, plain progress on TV (d-pad seeks).
                    val progressFraction = if (uiState.duration > 0) {
                        uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                    } else 0f
                    if (dims.isTV) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = AccentBlue,
                            trackColor = Divider
                        )
                    } else {
                        androidx.compose.material3.Slider(
                            value = progressFraction,
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Time display — monospace for the clock rhythm
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(uiState.currentPosition),
                            style = EditorialMono,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.isPlaying) "PLAYING" else "PAUSED",
                            style = EditorialRowLabel,
                            color = AccentBlue
                        )
                        Text(
                            text = formatTime(uiState.duration),
                            style = EditorialMono,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Seek indicator
        AnimatedVisibility(
            visible = seekIndicator != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(com.switchsides.switchstream.ui.theme.PureBlack.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = seekIndicator ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    color = com.switchsides.switchstream.ui.theme.PureWhite
                )
            }
        }

        // Brightness/volume indicator
        AnimatedVisibility(
            visible = verticalIndicator != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val indicator = verticalIndicator
            if (indicator != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(com.switchsides.switchstream.ui.theme.PureBlack.copy(alpha = 0.7f))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (indicator.kind == VerticalAdjust.Kind.BRIGHTNESS)
                Icons.Filled.BrightnessMedium else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = com.switchsides.switchstream.ui.theme.PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { indicator.fraction },
                        modifier = Modifier.width(160.dp),
                        color = AccentBlue,
                        trackColor = com.switchsides.switchstream.ui.theme.PureWhite.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "${(indicator.fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = com.switchsides.switchstream.ui.theme.PureWhite
                    )
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

        // Quality dialog
        if (uiState.showQualityDialog) {
            QualitySelector(
                title = "Streaming Quality",
                currentQuality = uiState.streamingQuality,
                onSelect = { q -> viewModel.setStreamingQuality(q) },
                onDismiss = { viewModel.dismissDialogs() }
            )
        }

        // Sleep timer dialog
        if (uiState.showSleepTimerDialog) {
            val activeMinutes = if (uiState.sleepTimerRemainingMs > 0) {
                ((uiState.sleepTimerRemainingMs + 59_999L) / 60_000L).toInt()
            } else null
            SleepTimerSelector(
                activeMinutes = activeMinutes,
                endOfEpisodeActive = uiState.sleepTimerEndOfEpisode,
                onSelect = { choice ->
                    when (choice) {
                        is SleepTimerChoice.Off -> viewModel.cancelSleepTimer()
                        is SleepTimerChoice.Minutes -> viewModel.setSleepTimerMinutes(choice.value)
                        is SleepTimerChoice.EndOfEpisode -> viewModel.setSleepTimerEndOfEpisode()
                    }
                },
                onDismiss = { viewModel.dismissDialogs() }
            )
        }

        // Sleep timer remaining badge
        if (uiState.sleepTimerRemainingMs > 0 || uiState.sleepTimerEndOfEpisode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(com.switchsides.switchstream.ui.theme.PureBlack.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (uiState.sleepTimerEndOfEpisode) "End of ep"
                        else formatSleepRemaining(uiState.sleepTimerRemainingMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
            }
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
                onClick = { viewModel.skipIntro() },
                modifier = Modifier.focusRequester(skipIntroFocusRequester)
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
                onClick = { viewModel.skipCredits() },
                modifier = Modifier.focusRequester(skipCreditsFocusRequester)
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
                modifier = Modifier.align(Alignment.BottomEnd),
                requestFocusOnAppear = dims.isTV
            )
        }

        // Resume prompt
        if (uiState.showResumePrompt) {
            ResumeDialog(
                positionMs = uiState.resumePositionMs,
                onResume = { viewModel.resumeFromPosition() },
                onStartOver = { viewModel.startFromBeginning() },
                requestFocusOnAppear = dims.isTV
            )
        }

        // "Now Playing" launch intro — 900ms film-leader overlay shown on entry and
        // on episode transitions. Keyed to title so it replays when the ViewModel
        // loads a new episode. Suppressed if the resume prompt takes over.
        var introVisible by remember(uiState.title) { mutableStateOf(true) }
        LaunchedEffect(uiState.title) {
            introVisible = true
            kotlinx.coroutines.delay(900)
            introVisible = false
        }
        LaunchedEffect(uiState.showResumePrompt) {
            if (uiState.showResumePrompt) introVisible = false
        }
        AnimatedVisibility(
            visible = introVisible && !uiState.showResumePrompt,
            enter = fadeIn(androidx.compose.animation.core.tween(200)),
            exit = fadeOut(androidx.compose.animation.core.tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            com.switchsides.switchstream.ui.components.NowPlayingIntro(
                title = uiState.title,
                backdropUrl = uiState.backdropUrl
            )
        }

        } // end if (!isInPip)
    }
}

@Composable
private fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .scale(if (isFocused) 1.1f else 1f)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = PureWhite
        )
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) PureBlack else TextPrimary,
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
    var isFocused by remember { mutableStateOf(false) }
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier
            .scale(if (isFocused) 1.1f else 1f)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = PureWhite
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) PureBlack else TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private enum class DragSide { LEFT, RIGHT }

private data class VerticalAdjust(val kind: Kind, val fraction: Float) {
    enum class Kind { BRIGHTNESS, VOLUME }
}

private fun readBrightnessFraction(window: android.view.Window, context: Context): Float {
    val attr = window.attributes.screenBrightness
    if (attr in 0f..1f) return attr
    return try {
        val sys = android.provider.Settings.System.getInt(
            context.contentResolver,
            android.provider.Settings.System.SCREEN_BRIGHTNESS
        )
        (sys / 255f).coerceIn(0f, 1f)
    } catch (_: Exception) {
        0.5f
    }
}

private fun applyBrightness(window: android.view.Window, fraction: Float) {
    val lp = window.attributes
    lp.screenBrightness = fraction.coerceIn(0.01f, 1f)
    window.attributes = lp
}

private fun readVolumeFraction(am: AudioManager): Float {
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (max <= 0) return 0f
    return am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun applyVolume(am: AudioManager, fraction: Float) {
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val v = kotlin.math.round(fraction * max).toInt().coerceIn(0, max)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
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

private fun formatSleepRemaining(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
