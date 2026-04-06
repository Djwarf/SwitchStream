package com.switchsides.switchstream.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
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
    val showUpNext: Boolean = false,
    val upNextCountdown: Int = 30,
    val showSkipIntro: Boolean = false,
    val showSkipCredits: Boolean = false,
    val showResumePrompt: Boolean = false,
    val resumePositionMs: Long = 0L
)

enum class TrackDialogType { AUDIO, SUBTITLE }

class PlayerViewModel(
    context: Context,
    private val playbackRepo: PlaybackRepository,
    private val libraryRepo: LibraryRepository,
    private val settingsManager: SettingsManager,
    private val itemId: UUID,
    private val seriesId: UUID? = null,
    title: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState(title = title))
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
    private var upNextDismissed: Boolean = false
    private var introTimestamps: IntroTimestamps? = null
    private var introSkipped: Boolean = false
    private var creditsSkipped: Boolean = false
    private var controlsHideJob: kotlinx.coroutines.Job? = null

    var onPlayNextEpisode: ((UUID) -> Unit)? = null

    init {
        loadSettings()
        setupPlayer()
        loadMediaTracks()
        loadNextEpisode()
        loadIntroTimestamps()
        startPositionTracking()
        restartHideTimer()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings: PlaybackSettings = settingsManager.settings.first()
            seekForwardMs = settings.seekForwardSeconds * 1000L
            seekBackMs = settings.seekBackSeconds * 1000L
            autoPlayNext = settings.autoPlayNextEpisode

            if (settings.defaultPlaybackSpeed != 1.0f) {
                player.playbackParameters = PlaybackParameters(settings.defaultPlaybackSpeed)
                _uiState.value = _uiState.value.copy(playbackSpeed = settings.defaultPlaybackSpeed)
            }
        }
    }

    private fun setupPlayer() {
        val streamUrl = playbackRepo.getStreamUrl(itemId)
        val mediaItem = MediaItem.fromUri(streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()

        // Check for resume position — always start playback, show prompt as overlay
        viewModelScope.launch {
            libraryRepo.getItemDetail(itemId).onSuccess { item ->
                val positionTicks = item.userData?.playbackPositionTicks ?: 0
                val durationTicks = item.runTimeTicks ?: 0
                val positionMs = positionTicks / 10_000
                // Show prompt if > 30s watched and < 90% complete
                if (positionMs > 30_000 && durationTicks > 0 && positionTicks < durationTicks * 9 / 10) {
                    player.seekTo(positionMs)
                    player.pause()
                    _uiState.value = _uiState.value.copy(
                        showResumePrompt = true,
                        resumePositionMs = positionMs
                    )
                } else {
                    if (positionMs > 0 && (durationTicks == 0L || positionTicks < durationTicks * 9 / 10)) {
                        player.seekTo(positionMs)
                    }
                    player.play()
                }
            } ?: run {
                // If detail fetch fails, just play from start
                player.play()
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _uiState.value = _uiState.value.copy(
                    isPlaying = player.isPlaying,
                    duration = player.duration.coerceAtLeast(0)
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        })

        viewModelScope.launch {
            playbackRepo.reportPlaybackStart(itemId)
        }
    }

    private fun loadMediaTracks() {
        viewModelScope.launch {
            playbackRepo.getMediaTracks(itemId).onSuccess { tracks ->
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
            playbackRepo.getIntroTimestamps(itemId).onSuccess { timestamps ->
                introTimestamps = timestamps
            }
        }
    }

    fun resumeFromPosition() {
        player.seekTo(_uiState.value.resumePositionMs)
        player.play()
        _uiState.value = _uiState.value.copy(showResumePrompt = false)
    }

    fun startFromBeginning() {
        player.seekTo(0)
        player.play()
        _uiState.value = _uiState.value.copy(showResumePrompt = false)
    }

    fun skipIntro() {
        val endMs = introTimestamps?.introEndMs ?: return
        player.seekTo(endMs)
        introSkipped = true
        _uiState.value = _uiState.value.copy(showSkipIntro = false)
    }

    fun skipCredits() {
        creditsSkipped = true
        _uiState.value = _uiState.value.copy(showSkipCredits = false)
        // If there's a next episode, play it; otherwise just dismiss
        if (_uiState.value.nextEpisode != null) {
            playNextEpisode()
        }
    }

    private fun loadNextEpisode() {
        if (seriesId == null) return
        viewModelScope.launch {
            libraryRepo.getNextUp(seriesId).onSuccess { nextUpList ->
                val nextEp = nextUpList.firstOrNull()
                // Only set as next if it's a different episode than the current one
                if (nextEp != null && nextEp.id != itemId) {
                    _uiState.value = _uiState.value.copy(nextEpisode = nextEp)
                }
            }
        }
    }

    private fun startPositionTracking() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentPos = player.currentPosition.coerceAtLeast(0)
                val duration = player.duration.coerceAtLeast(0)

                // Skip intro/credits visibility
                val intro = introTimestamps
                val showIntro = intro?.introEndMs != null
                    && !introSkipped
                    && currentPos < intro.introEndMs
                    && currentPos > 2000 // Show after 2s into playback
                val showCredits = intro?.creditsStartMs != null
                    && !creditsSkipped
                    && currentPos >= intro.creditsStartMs

                _uiState.value = _uiState.value.copy(
                    currentPosition = currentPos,
                    duration = duration,
                    showSkipIntro = showIntro,
                    showSkipCredits = showCredits
                )

                // Up Next logic
                val state = _uiState.value
                if (duration > 0 && currentPos >= duration - 30_000
                    && state.nextEpisode != null
                    && autoPlayNext
                    && !upNextDismissed
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
                        itemId = itemId,
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

        // Close dialog immediately
        _uiState.value = _uiState.value.copy(
            selectedAudioIndex = index,
            showTrackDialog = null
        )

        // Apply track override after UI recomposes
        val track = tracks[index]
        viewModelScope.launch {
            delay(100)
            for (group in player.currentTracks.groups) {
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    val override = TrackSelectionOverride(group.mediaTrackGroup, track.index)
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(override)
                        .build()
                    break
                }
            }
        }
    }

    fun selectSubtitleTrack(index: Int) {
        // Close dialog immediately
        _uiState.value = _uiState.value.copy(
            selectedSubtitleIndex = index,
            showTrackDialog = null
        )

        // Apply after UI recomposes
        viewModelScope.launch {
            delay(100)
            if (index == -1) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            } else {
                val tracks = _uiState.value.subtitleTracks
                if (index < 0 || index >= tracks.size) return@launch

                val track = tracks[index]
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()

                for (group in player.currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, track.index)
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(override)
                            .build()
                        break
                    }
                }
            }
        }
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

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(
            showTrackDialog = null,
            showSpeedDialog = false
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

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(showControls = !_uiState.value.showControls)
    }

    fun showControls() {
        _uiState.value = _uiState.value.copy(showControls = true)
        restartHideTimer()
    }

    fun hideControls() {
        controlsHideJob?.cancel()
        _uiState.value = _uiState.value.copy(showControls = false)
    }

    private fun restartHideTimer() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(2500)
            _uiState.value = _uiState.value.copy(showControls = false)
        }
    }

    fun playNextEpisode() {
        val nextEp = _uiState.value.nextEpisode ?: return
        stopPlayback()
        onPlayNextEpisode?.invoke(nextEp.id)
    }

    fun cancelUpNext() {
        upNextDismissed = true
        _uiState.value = _uiState.value.copy(showUpNext = false)
    }

    fun stopPlayback() {
        viewModelScope.launch {
            playbackRepo.reportPlaybackStopped(
                itemId = itemId,
                positionTicks = player.currentPosition * 10000
            )
        }
        player.stop()
    }

    override fun onCleared() {
        stopPlayback()
        mediaSession.release()
        player.release()
        super.onCleared()
    }
}
