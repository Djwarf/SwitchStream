package com.switchsides.switchstream.ui.screens.player

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
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
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.DeviceType
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.components.FocusableButton
import com.switchsides.switchstream.ui.components.PlaybackSpeedSelector
import com.switchsides.switchstream.ui.components.PlayerMoreSheet
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val upNextFocusRequester = remember { FocusRequester() }
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
        || uiState.showMoreSheet
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
    LaunchedEffect(uiState.showUpNext) {
        if (uiState.showUpNext && dims.isTV) runCatching { upNextFocusRequester.requestFocus() }
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
                if (!dims.isTV) {
                    // Mobile: double-tap sides to seek, single tap to toggle controls,
                    // vertical swipe on left half = brightness, right half = volume.
                    // When the child lock is engaged, ALL gestures here no-op — the user
                    // must long-press the unlock affordance first. The pointerInput keys
                    // include isLocked so the lambdas refresh when state flips.
                    Modifier
                        .pointerInput(uiState.isLocked) {
                            detectTapGestures(
                                onTap = {
                                    if (uiState.isLocked) return@detectTapGestures
                                    if (uiState.showControls) viewModel.hideControls()
                                    else viewModel.showControls()
                                },
                                onDoubleTap = { offset ->
                                    if (uiState.isLocked) return@detectTapGestures
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
                        .pointerInput(uiState.isLocked) {
                            var startSide: DragSide? = null
                            var fraction = 0f
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    if (uiState.isLocked) return@detectVerticalDragGestures
                                    val halfWidth = size.width / 2f
                                    val side = if (offset.x < halfWidth) DragSide.LEFT else DragSide.RIGHT
                                    startSide = side
                                    fraction = when (side) {
                                        DragSide.LEFT -> activity?.window?.let { readBrightnessFraction(it, context) } ?: 0.5f
                                        DragSide.RIGHT -> readVolumeFraction(audioManager)
                                    }
                                },
                                onVerticalDrag = { _, dy ->
                                    if (uiState.isLocked) return@detectVerticalDragGestures
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
                            // Transient overlays come BEFORE the chrome-hide branch so
                            // BACK consistently means "dismiss the topmost thing"
                            // rather than swallowing it as a chrome hide.
                            uiState.showSkipIntro -> {
                                viewModel.dismissSkipIntro()
                                runCatching { focusRequester.requestFocus() }
                            }
                            uiState.showSkipCredits -> {
                                viewModel.dismissSkipCredits()
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

        // Custom overlay controls — fully suppressed when the lock is engaged on mobile.
        if (uiState.showControls && !uiState.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OverlayBlack)
            ) {
                // Lock affordance (mobile only). Lives at the top-right corner of the
                // chrome opposite the title. Tap once to engage the child lock.
                if (!dims.isTV) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(32.dp)
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(com.switchsides.switchstream.ui.theme.PureWhite.copy(alpha = 0.12f))
                            .clickable { viewModel.setLocked(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Lock controls",
                            tint = com.switchsides.switchstream.ui.theme.PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
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

                        // More — opens a sheet with audio track, speed, quality, sleep timer, aspect.
                        ControlIconButton(
                            icon = Icons.Filled.MoreHoriz,
                            contentDescription = "More",
                            onClick = { viewModel.showMoreSheet() }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seek bar — interactive Slider on touch, plain progress on TV (d-pad seeks).
                    val progressFraction = if (uiState.duration > 0) {
                        uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                    } else 0f
                    if (dims.isTV) {
                        TvSeekBar(
                            currentPosition = uiState.currentPosition,
                            duration = uiState.duration,
                            onSeekDelta = { deltaMs ->
                                viewModel.seekBy(deltaMs)
                                viewModel.showControls()
                            }
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
        if (seekIndicator != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
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
        verticalIndicator?.let { indicator ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
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

        // Player options "More" sheet — consolidates audio/speed/quality/sleep/aspect.
        if (uiState.showMoreSheet) {
            val selectedAudio = uiState.audioTracks.getOrNull(uiState.selectedAudioIndex)
            val audioTrackLabel = selectedAudio?.let { track ->
                track.title.ifEmpty { track.language ?: "Track ${uiState.selectedAudioIndex + 1}" }
            } ?: "Default"

            val sleepTimerLabel = when {
                uiState.sleepTimerEndOfEpisode -> "End of episode"
                uiState.sleepTimerRemainingMs > 0 -> {
                    val minutes = ((uiState.sleepTimerRemainingMs + 59_999L) / 60_000L).toInt()
                    "$minutes min"
                }
                else -> "Off"
            }

            val aspectLabel = when (resizeMode) {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
                else -> "Zoom"
            }

            PlayerMoreSheet(
                audioTrackLabel = audioTrackLabel,
                speedLabel = "${uiState.playbackSpeed}x",
                qualityLabel = qualityLabel(uiState.streamingQuality),
                sleepTimerLabel = sleepTimerLabel,
                aspectLabel = aspectLabel,
                onAudioClick = { viewModel.showAudioDialog() },
                onSpeedClick = { viewModel.showSpeedDialog() },
                onQualityClick = { viewModel.showQualityDialog() },
                onSleepTimerClick = { viewModel.showSleepTimerDialog() },
                onAspectClick = {
                    resizeMode = when (resizeMode) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT ->
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL ->
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else ->
                            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
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
        if (uiState.showSkipIntro) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 120.dp)
            ) {
                SkipOverlayButton(
                    text = "Skip Intro",
                    focusRequester = skipIntroFocusRequester,
                    onClick = { viewModel.skipIntro() }
                )
            }
        }

        // Skip Credits button
        if (uiState.showSkipCredits) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 120.dp)
            ) {
                SkipOverlayButton(
                    text = "Skip Credits",
                    focusRequester = skipCreditsFocusRequester,
                    onClick = { viewModel.skipCredits() }
                )
            }
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
                requestFocusOnAppear = dims.isTV,
                playNowFocusRequester = if (dims.isTV) upNextFocusRequester else null
            )
        }

        // Unlock affordance — mobile-only. Renders OUTSIDE the chrome's
        // `showControls` gate so it remains the single visible UI when locked.
        // The padlock is the only target the user can interact with; everywhere
        // else on the screen ignores taps until released.
        if (uiState.isLocked && !dims.isTV) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .size(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(com.switchsides.switchstream.ui.theme.PureWhite.copy(alpha = 0.18f))
                    .pointerInput(Unit) {
                        // Long-press (~1s default) releases the lock. Single taps
                        // are deliberately ignored so a pocket touch can't undo it.
                        detectTapGestures(
                            onLongPress = { viewModel.setLocked(false) }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Hold to unlock",
                    tint = com.switchsides.switchstream.ui.theme.PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
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

/**
 * TV-only focusable seek bar with rich scrub feedback. Four layered cues fire
 * each time the user presses LEFT/RIGHT, so it's obvious which bar is selected,
 * that it's moving, and how big the step has grown:
 *
 * 1. **Beefier focused state** — the track grows 4 → 8 dp, the unfocused dark
 *    track lifts to a translucent white, and a 22 dp thumb appears with a white
 *    outer ring so it reads at couch distance.
 * 2. **Time preview chip** — a monospace chip floats above the thumb showing
 *    the post-seek time. Follows the thumb horizontally as it moves.
 * 3. **Pulse on played fill** — each press flashes the played-progress portion
 *    bright-white, fading over ~280 ms. Gives a "kick" feel to each step.
 * 4. **Step-size badge** — chip below the thumb shows `±5s`, `±10s`, `±20s`…
 *    matching the ramp. Stays visible for 500 ms after the last press then
 *    fades over 200 ms, so the user sees how fast they're scrubbing.
 *
 * Step ramp: 5 s base, doubles on each press within 400 ms of the last, capped
 * at 60 s. Resets to 5 s after a 400 ms idle gap.
 *
 * Layout reserves 60 dp tall on TV regardless of focus so the time row below
 * doesn't jump when focus enters/leaves. Unfocused, only the thin 4 dp track
 * is drawn — the chip/badge slots stay empty.
 */
@Composable
private fun TvSeekBar(
    currentPosition: Long,
    duration: Long,
    onSeekDelta: (deltaMs: Long) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var lastSeekAtMs by remember { mutableStateOf(0L) }
    var currentStepMs by remember { mutableStateOf(BASE_SEEK_STEP_MS) }
    var pulseCounter by remember { mutableStateOf(0) }
    var stepLabel by remember { mutableStateOf("") }

    val pulseAlpha = remember { Animatable(0f) }
    val badgeAlpha = remember { Animatable(0f) }

    LaunchedEffect(pulseCounter) {
        if (pulseCounter > 0) {
            // Snap to peak immediately so rapid presses always re-trigger the
            // pulse instead of waiting for the previous fade to land.
            pulseAlpha.snapTo(1f)
            badgeAlpha.snapTo(1f)
            launch { pulseAlpha.animateTo(0f, tween(280)) }
            delay(500)
            badgeAlpha.animateTo(0f, tween(200))
        }
    }

    fun handlePress(direction: Int) {
        val now = System.currentTimeMillis()
        currentStepMs = if (now - lastSeekAtMs < SEEK_RAMP_GAP_MS) {
            (currentStepMs * 2L).coerceAtMost(MAX_SEEK_STEP_MS)
        } else {
            BASE_SEEK_STEP_MS
        }
        lastSeekAtMs = now
        val sign = if (direction > 0) "+" else "−"
        stepLabel = "$sign${currentStepMs / 1000}s"
        pulseCounter++
        onSeekDelta(direction * currentStepMs)
    }

    val fraction = if (duration > 0) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val barHeight = if (isFocused) 8.dp else 4.dp
    val barShape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { handlePress(-1); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { handlePress(1); true }
                    else -> false
                }
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val barWidth = maxWidth
            val thumbDp = 22.dp
            val thumbCenter = barWidth * fraction
            val thumbStart = (thumbCenter - thumbDp / 2).coerceIn(0.dp, barWidth - thumbDp)

            // 1. Time preview chip — top, centred above the thumb.
            if (isFocused) {
                val chipWidth = 92.dp
                val chipStart = (thumbCenter - chipWidth / 2).coerceIn(0.dp, barWidth - chipWidth)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = chipStart)
                        .width(chipWidth)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = EditorialMono,
                        color = PureWhite,
                        maxLines = 1
                    )
                }
            }

            // 2. Track (unfilled portion) — vertically centred in the 60dp slot.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(barShape)
                    .background(if (isFocused) PureWhite.copy(alpha = 0.20f) else Divider)
            )
            // 2b. Played-progress fill.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(fraction)
                    .height(barHeight)
                    .clip(barShape)
                    .background(AccentBlue)
            )
            // 3. Pulse overlay on played fill — full fill brightens on press,
            //    fades over ~280 ms.
            if (pulseAlpha.value > 0f && isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(fraction)
                        .height(barHeight)
                        .clip(barShape)
                        .background(PureWhite.copy(alpha = pulseAlpha.value * 0.45f))
                )
            }
            // 1b. Thumb — white ring around an accent core, scaled bigger than
            //     the unfocused bar so it pops at couch distance.
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = thumbStart)
                        .size(thumbDp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                )
            }
            // 4. Step-size badge — bottom, fades after 600 ms idle.
            if (isFocused && badgeAlpha.value > 0f) {
                val badgeWidth = 60.dp
                val badgeStart = (thumbCenter - badgeWidth / 2).coerceIn(0.dp, barWidth - badgeWidth)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = badgeStart)
                        .width(badgeWidth)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentBlue.copy(alpha = badgeAlpha.value * 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepLabel,
                        style = EditorialMono,
                        color = PureWhite,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private const val BASE_SEEK_STEP_MS = 5_000L
private const val MAX_SEEK_STEP_MS = 60_000L
private const val SEEK_RAMP_GAP_MS = 400L

/**
 * Compact glass-style button used by the transient Skip Intro / Skip Credits
 * overlays. Visually quieter than [FocusableButton] so it doesn't read as core
 * chrome over the video, and the focused-state white-fill makes the auto-focus
 * on TV obvious without screaming.
 */
@Composable
private fun SkipOverlayButton(
    text: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable(),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = PureWhite.copy(alpha = 0.18f)
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.5.dp, com.switchsides.switchstream.ui.theme.GlassBorderFocus),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) PureWhite else TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
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
