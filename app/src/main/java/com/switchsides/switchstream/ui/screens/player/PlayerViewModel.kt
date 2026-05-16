package com.switchsides.switchstream.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.switchsides.switchstream.data.PlaybackSettings
import com.switchsides.switchstream.data.SettingsManager
import com.switchsides.switchstream.data.model.MediaTrackInfo
import com.switchsides.switchstream.data.model.TrackType
import com.switchsides.switchstream.data.repository.IntroTimestamps
import com.switchsides.switchstream.data.repository.LibraryRepository
import com.switchsides.switchstream.data.repository.PlaybackRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val showControls: Boolean = true,
    val title: String = "",
    val audioTracks: List<MediaTrackInfo> = emptyList(),
    val subtitleTracks: List<MediaTrackInfo> = emptyList(),
    val selectedAudioIndex: Int = -1,
    val selectedSubtitleIndex: Int = -1,
    val showTrackDialog: TrackDialogType? = null,
    val playbackSpeed: Float = 1.0f,
    val showSpeedDialog: Boolean = false,
    val nextEpisode: BaseItemDto? = null,
    val previousEpisode: BaseItemDto? = null,
    val showUpNext: Boolean = false,
    val upNextCountdown: Int = 30,
    val showSkipIntro: Boolean = false,
    val showSkipCredits: Boolean = false,
    val sleepTimerRemainingMs: Long = 0L,
    val sleepTimerEndOfEpisode: Boolean = false,
    val showSleepTimerDialog: Boolean = false,
    val streamingQuality: Int = 0,
    val showQualityDialog: Boolean = false,
    val showMoreSheet: Boolean = false,
    val backdropUrl: String? = null,
    // Mobile-only "child lock". When true the entire chrome is suppressed and
    // single taps no-op so a pocket touch can't pause/seek; long-press the unlock
    // affordance to release.
    val isLocked: Boolean = false
)

enum class TrackDialogType { AUDIO, SUBTITLE }

class PlayerViewModel(
    context: Context,
    private val playbackRepo: PlaybackRepository,
    private val libraryRepo: LibraryRepository,
    private val settingsManager: SettingsManager,
    private val imageRepo: com.switchsides.switchstream.data.repository.ImageRepository,
    initialItemId: UUID,
    private val seriesId: UUID? = null,
    title: String,
    private val isTV: Boolean = false
) : ViewModel() {

    private var currentItemId: UUID = initialItemId

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            title = title,
            backdropUrl = imageRepo.getBackdropUrl(initialItemId)
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 60_000,
                    /* bufferForPlaybackMs = */ 2_500,
                    /* bufferForPlaybackAfterRebufferMs = */ 5_000
                )
                .build()
        )
        .build()
    private val mediaSession: MediaSession = MediaSession.Builder(context, player).build()

    private var seekForwardMs: Long = 10_000L
    private var seekBackMs: Long = 10_000L
    private var autoPlayNext: Boolean = true
    private var currentStreamingQuality: Int = 0
    // Jellyfin global stream indices for the currently selected audio / subtitle.
    // Null means "let the server pick its default" (no override on the URL).
    // These survive quality/track changes and feed every stream URL build.
    private var currentAudioStreamIndex: Int? = null
    private var currentSubtitleStreamIndex: Int? = null
    private var upNextDismissed: Boolean = false
    private var introTimestamps: IntroTimestamps? = null
    private var introSkipped: Boolean = false
    private var creditsSkipped: Boolean = false
    private var nextEpisodeTriggered: Boolean = false
    private var resolvedSeriesId: UUID? = seriesId
    private var controlsHideJob: kotlinx.coroutines.Job? = null
    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _uiState.value = _uiState.value.copy(
                isPlaying = player.isPlaying,
                duration = player.duration.coerceAtLeast(0)
            )
            if (state == Player.STATE_ENDED) {
                if (_uiState.value.sleepTimerEndOfEpisode) {
                    _uiState.value = _uiState.value.copy(sleepTimerEndOfEpisode = false)
                    player.pause()
                    return
                }
                if (autoPlayNext && !upNextDismissed && _uiState.value.nextEpisode != null) {
                    playNextEpisode()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }
    }

    init {
        player.addListener(playerListener)
        viewModelScope.launch {
            applySettings()
            loadCurrentItem()
        }
        startPositionTracking()
        restartHideTimer()
    }

    private suspend fun applySettings() {
        val settings: PlaybackSettings = settingsManager.settings.first()
        seekForwardMs = settings.seekForwardSeconds * 1000L
        seekBackMs = settings.seekBackSeconds * 1000L
        autoPlayNext = settings.autoPlayNextEpisode
        currentStreamingQuality = settings.streamingQuality
        _uiState.value = _uiState.value.copy(streamingQuality = settings.streamingQuality)

        if (settings.defaultPlaybackSpeed != 1.0f) {
            player.playbackParameters = PlaybackParameters(settings.defaultPlaybackSpeed)
            _uiState.value = _uiState.value.copy(playbackSpeed = settings.defaultPlaybackSpeed)
        }
    }

    private fun loadCurrentItem() {
        val streamUrl = playbackRepo.getStreamUrl(
            itemId = currentItemId,
            maxHeight = currentStreamingQuality,
            audioStreamIndex = currentAudioStreamIndex,
            subtitleStreamIndex = currentSubtitleStreamIndex
        )
        val mediaItem = MediaItem.fromUri(streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()

        // Always auto-resume from saved position (skip if effectively complete: past 90% of runtime).
        viewModelScope.launch {
            libraryRepo.getItemDetail(currentItemId).onSuccess { item ->
                val positionTicks = item.userData?.playbackPositionTicks ?: 0
                val durationTicks = item.runTimeTicks ?: 0
                val positionMs = positionTicks / 10_000
                val notNearEnd = durationTicks == 0L || positionTicks < durationTicks * 9 / 10
                if (positionMs > 0 && notNearEnd) {
                    player.seekTo(positionMs)
                }
                player.play()
            } ?: run {
                player.play()
            }
        }

        viewModelScope.launch {
            playbackRepo.reportPlaybackStart(currentItemId)
        }

        loadMediaTracks()
        loadIntroTimestamps()
        loadAdjacentEpisodes()
    }

    private fun loadMediaTracks() {
        viewModelScope.launch {
            playbackRepo.getMediaTracks(currentItemId).onSuccess { tracks ->
                val audioTracks = tracks.filter { it.type == TrackType.AUDIO }
                val subtitleTracks = tracks.filter { it.type == TrackType.SUBTITLE }

                val defaultAudioIndex = audioTracks.indexOfFirst { it.isDefault }
                    .takeIf { it >= 0 } ?: if (audioTracks.isNotEmpty()) 0 else -1
                val defaultSubtitleIndex = subtitleTracks.indexOfFirst { it.isDefault }

                _uiState.value = _uiState.value.copy(
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                    selectedAudioIndex = defaultAudioIndex,
                    selectedSubtitleIndex = defaultSubtitleIndex
                )
            }
        }
    }

    private fun loadIntroTimestamps() {
        viewModelScope.launch {
            playbackRepo.getIntroTimestamps(currentItemId).onSuccess { timestamps ->
                introTimestamps = timestamps
            }
        }
    }

    fun skipIntro() {
        val endMs = introTimestamps?.introEndMs ?: return
        player.seekTo(endMs)
        introSkipped = true
        _uiState.value = _uiState.value.copy(showSkipIntro = false)
    }

    /**
     * BACK while the Skip Intro overlay is up should mean "go away" — same semantic
     * as pressing skip but without the seek. Once dismissed it won't re-appear for
     * this episode, matching the user's intent of "I saw it, I don't want it."
     */
    fun dismissSkipIntro() {
        introSkipped = true
        _uiState.value = _uiState.value.copy(showSkipIntro = false)
    }

    fun skipCredits() {
        creditsSkipped = true
        _uiState.value = _uiState.value.copy(showSkipCredits = false)
        if (autoPlayNext && !upNextDismissed && _uiState.value.nextEpisode != null) {
            playNextEpisode()
        }
    }

    fun dismissSkipCredits() {
        creditsSkipped = true
        _uiState.value = _uiState.value.copy(showSkipCredits = false)
    }

    private fun loadAdjacentEpisodes() {
        viewModelScope.launch {
            val sid = resolvedSeriesId
                ?: libraryRepo.getItemDetail(currentItemId).getOrNull()?.seriesId
                ?: return@launch
            resolvedSeriesId = sid

            val nextDef = async { libraryRepo.getNextEpisode(sid, currentItemId).getOrNull() }
            val prevDef = async { libraryRepo.getPreviousEpisode(sid, currentItemId).getOrNull() }
            val next = nextDef.await()
            val prev = prevDef.await()
            _uiState.value = _uiState.value.copy(
                nextEpisode = next ?: _uiState.value.nextEpisode,
                previousEpisode = prev
            )
        }
    }

    private fun startPositionTracking() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentPos = player.currentPosition.coerceAtLeast(0)
                val duration = player.duration.coerceAtLeast(0)

                // Skip intro/credits visibility. The intro overlay must only show
                // while we're actually playing the intro chapter — gating on
                // introStartMs prevents a false fire during a cold open or recap
                // that precedes the title sequence.
                val intro = introTimestamps
                val introStart = intro?.introStartMs ?: 0L
                val showIntro = intro?.introEndMs != null
                    && !introSkipped
                    && currentPos >= introStart
                    && currentPos < intro.introEndMs
                val showCredits = intro?.creditsStartMs != null
                    && !creditsSkipped
                    && currentPos >= intro.creditsStartMs

                _uiState.value = _uiState.value.copy(
                    currentPosition = currentPos,
                    duration = duration,
                    showSkipIntro = showIntro,
                    showSkipCredits = showCredits
                )

                // Up Next logic — suppressed when the user armed "stop at end of episode".
                val state = _uiState.value
                if (duration > 0 && currentPos >= duration - 30_000
                    && state.nextEpisode != null
                    && autoPlayNext
                    && !upNextDismissed
                    && !state.sleepTimerEndOfEpisode
                ) {
                    if (!state.showUpNext) {
                        _uiState.value = state.copy(showUpNext = true, upNextCountdown = 30)
                    } else {
                        val remaining = ((duration - currentPos) / 1000).toInt().coerceAtLeast(0)
                        _uiState.value = state.copy(upNextCountdown = remaining)

                        if (remaining <= 0) {
                            playNextEpisode()
                        }
                    }
                }

                // Report progress every 10 seconds
                if (currentPos % 10000 < 1000) {
                    playbackRepo.reportPlaybackProgress(
                        itemId = currentItemId,
                        positionTicks = currentPos * 10000,
                        isPaused = !player.isPlaying
                    )
                }
            }
        }
    }

    fun selectAudioTrack(index: Int) {
        val tracks = _uiState.value.audioTracks
        if (index < 0 || index >= tracks.size) return
        if (index == _uiState.value.selectedAudioIndex) {
            _uiState.value = _uiState.value.copy(showTrackDialog = null)
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedAudioIndex = index,
            showTrackDialog = null
        )
        // Jellyfin's stream index is the global one within the source file (which
        // interleaves video/audio/subtitle); we hand that to the server, not to
        // ExoPlayer, because direct-play and HLS-transcode both rely on the URL.
        currentAudioStreamIndex = tracks[index].index
        reloadStreamPreservingPosition()
    }

    fun selectSubtitleTrack(index: Int) {
        _uiState.value = _uiState.value.copy(
            selectedSubtitleIndex = index,
            showTrackDialog = null
        )
        currentSubtitleStreamIndex = if (index < 0) {
            null
        } else {
            val tracks = _uiState.value.subtitleTracks
            tracks.getOrNull(index)?.index
        }
        reloadStreamPreservingPosition()
    }

    /**
     * Rebuild and reload the active stream URL while preserving playback position.
     * Used whenever the user changes audio, subtitle, or quality — all three need
     * to round-trip through Jellyfin because the file-level multiplexing or transcode
     * pipeline determines what the player actually receives.
     */
    private fun reloadStreamPreservingPosition() {
        val resumePosition = player.currentPosition.coerceAtLeast(0L)
        val streamUrl = playbackRepo.getStreamUrl(
            itemId = currentItemId,
            maxHeight = currentStreamingQuality,
            audioStreamIndex = currentAudioStreamIndex,
            subtitleStreamIndex = currentSubtitleStreamIndex
        )
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        if (resumePosition > 0) player.seekTo(resumePosition)
        player.play()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _uiState.value = _uiState.value.copy(
            playbackSpeed = speed,
            showSpeedDialog = false
        )
    }

    fun showAudioDialog() {
        _uiState.value = _uiState.value.copy(showTrackDialog = TrackDialogType.AUDIO)
    }

    fun showSubtitleDialog() {
        _uiState.value = _uiState.value.copy(showTrackDialog = TrackDialogType.SUBTITLE)
    }

    fun showSpeedDialog() {
        _uiState.value = _uiState.value.copy(showSpeedDialog = true)
    }

    fun showMoreSheet() {
        _uiState.value = _uiState.value.copy(showMoreSheet = true)
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(
            showTrackDialog = null,
            showSpeedDialog = false,
            showSleepTimerDialog = false,
            showQualityDialog = false,
            showMoreSheet = false
        )
    }

    fun showQualityDialog() {
        _uiState.value = _uiState.value.copy(showQualityDialog = true)
    }

    fun setStreamingQuality(quality: Int) {
        if (quality == currentStreamingQuality) {
            _uiState.value = _uiState.value.copy(showQualityDialog = false)
            return
        }
        currentStreamingQuality = quality
        _uiState.value = _uiState.value.copy(
            streamingQuality = quality,
            showQualityDialog = false
        )
        reloadStreamPreservingPosition()
    }

    fun showSleepTimerDialog() {
        _uiState.value = _uiState.value.copy(showSleepTimerDialog = true)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalMs = minutes * 60_000L
        _uiState.value = _uiState.value.copy(
            sleepTimerRemainingMs = totalMs,
            sleepTimerEndOfEpisode = false,
            showSleepTimerDialog = false
        )
        sleepTimerJob = viewModelScope.launch {
            val expiresAt = System.currentTimeMillis() + totalMs
            while (isActive) {
                val remaining = expiresAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    player.pause()
                    _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = 0L)
                    break
                }
                _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = remaining)
                delay(1000)
            }
        }
    }

    fun setSleepTimerEndOfEpisode() {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            sleepTimerRemainingMs = 0L,
            sleepTimerEndOfEpisode = true,
            showSleepTimerDialog = false
        )
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            sleepTimerRemainingMs = 0L,
            sleepTimerEndOfEpisode = false,
            showSleepTimerDialog = false
        )
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            controlsHideJob?.cancel()
        } else {
            player.play()
            restartHideTimer()
        }
    }

    fun seekForward() {
        player.seekTo(player.currentPosition + seekForwardMs)
    }

    fun seekBack() {
        player.seekTo((player.currentPosition - seekBackMs).coerceAtLeast(0))
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    /**
     * Relative seek by [deltaMs] (negative seeks backward). Used by the focused-TV
     * seek bar where the call site computes a ramped step size, rather than relying
     * on the fixed seekForwardMs / seekBackMs settings used by the chrome buttons.
     */
    fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs)
            .coerceAtLeast(0L)
            .coerceAtMost(player.duration.coerceAtLeast(0L))
        player.seekTo(target)
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }

    fun setLocked(locked: Boolean) {
        controlsHideJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isLocked = locked,
            // Locking should also clear any open chrome immediately; unlocking
            // surfaces the chrome so the user can confirm they're back in control.
            showControls = !locked,
            showTrackDialog = null,
            showSpeedDialog = false,
            showSleepTimerDialog = false,
            showQualityDialog = false,
            showMoreSheet = false
        )
        if (!locked) restartHideTimer()
    }

    fun showControls() {
        // Skip the StateFlow emit when controls are already visible — d-pad nav on TV
        // can fire this dozens of times per row traversal and each emit recomposes the
        // player chrome. Still kick the hide timer so navigation keeps the chrome alive.
        if (!_uiState.value.showControls) {
            _uiState.value = _uiState.value.copy(showControls = true)
        }
        restartHideTimer()
    }

    fun hideControls() {
        controlsHideJob?.cancel()
        _uiState.value = _uiState.value.copy(showControls = false)
    }

    private fun restartHideTimer() {
        controlsHideJob?.cancel()
        // TV needs a longer window — d-pad navigation across the bottom row takes several
        // seconds, whereas touch users tap once and expect the chrome to fade quickly.
        val hideDelayMs = if (isTV) 7000L else 2500L
        controlsHideJob = viewModelScope.launch {
            delay(hideDelayMs)
            _uiState.value = _uiState.value.copy(showControls = false)
        }
    }

    fun playNextEpisode() {
        if (nextEpisodeTriggered) return
        val nextEp = _uiState.value.nextEpisode ?: return
        transitionToEpisode(nextEp)
    }

    fun playPreviousEpisode() {
        if (nextEpisodeTriggered) return
        val prevEp = _uiState.value.previousEpisode ?: return
        transitionToEpisode(prevEp)
    }

    private fun transitionToEpisode(episode: BaseItemDto) {
        nextEpisodeTriggered = true

        viewModelScope.launch {
            playbackRepo.reportPlaybackStopped(
                itemId = currentItemId,
                positionTicks = player.currentPosition * 10000
            )
        }

        currentItemId = episode.id
        creditsSkipped = false
        introSkipped = false
        upNextDismissed = false
        introTimestamps = null
        nextEpisodeTriggered = false
        // Stream indices are per-file; a new episode has its own track layout, so
        // start fresh and let the server pick defaults on the first load.
        currentAudioStreamIndex = null
        currentSubtitleStreamIndex = null

        _uiState.value = _uiState.value.copy(
            title = episode.name ?: "",
            currentPosition = 0L,
            duration = 0L,
            nextEpisode = null,
            previousEpisode = null,
            showUpNext = false,
            upNextCountdown = 30,
            showSkipIntro = false,
            showSkipCredits = false,
            audioTracks = emptyList(),
            subtitleTracks = emptyList(),
            selectedAudioIndex = -1,
            selectedSubtitleIndex = -1,
            backdropUrl = imageRepo.getBackdropUrl(episode.id)
        )

        player.stop()
        player.clearMediaItems()
        loadCurrentItem()
    }

    fun cancelUpNext() {
        upNextDismissed = true
        _uiState.value = _uiState.value.copy(showUpNext = false)
    }

    fun stopPlayback() {
        viewModelScope.launch {
            playbackRepo.reportPlaybackStopped(
                itemId = currentItemId,
                positionTicks = player.currentPosition * 10000
            )
        }
        player.stop()
    }

    override fun onCleared() {
        stopPlayback()
        sleepTimerJob?.cancel()
        mediaSession.release()
        player.release()
        super.onCleared()
    }
}
