package com.example.switchstream.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.switchstream.data.PlaybackSettings
import com.example.switchstream.data.SettingsManager
import com.example.switchstream.data.model.MediaTrackInfo
import com.example.switchstream.data.model.TrackType
import com.example.switchstream.data.repository.LibraryRepository
import com.example.switchstream.data.repository.PlaybackRepository
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
    val upNextCountdown: Int = 30
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

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val mediaSession: MediaSession = MediaSession.Builder(context, player).build()

    private var seekForwardMs: Long = 10_000L
    private var seekBackMs: Long = 10_000L
    private var autoPlayNext: Boolean = true
    private var upNextDismissed: Boolean = false

    var onPlayNextEpisode: ((UUID) -> Unit)? = null

    init {
        loadSettings()
        setupPlayer()
        loadMediaTracks()
        loadNextEpisode()
        startPositionTracking()
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

        // Resume from saved position, then play
        viewModelScope.launch {
            libraryRepo.getItemDetail(itemId).onSuccess { item ->
                val positionTicks = item.userData?.playbackPositionTicks ?: 0
                if (positionTicks > 0) {
                    player.seekTo(positionTicks / 10_000)
                }
            }
            player.play()
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

                _uiState.value = _uiState.value.copy(
                    currentPosition = currentPos,
                    duration = duration
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

        val track = tracks[index]
        val trackGroups = player.currentTracks.groups
        var audioGroupIndex = 0
        for (group in trackGroups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                if (audioGroupIndex == 0) {
                    val override = TrackSelectionOverride(group.mediaTrackGroup, track.index)
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(override)
                        .build()
                    break
                }
                audioGroupIndex++
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedAudioIndex = index,
            showTrackDialog = null
        )
    }

    fun selectSubtitleTrack(index: Int) {
        if (index == -1) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val tracks = _uiState.value.subtitleTracks
            if (index < 0 || index >= tracks.size) return

            val track = tracks[index]
            // Re-enable subtitle track type
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()

            val trackGroups = player.currentTracks.groups
            for (group in trackGroups) {
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

        _uiState.value = _uiState.value.copy(
            selectedSubtitleIndex = index,
            showTrackDialog = null
        )
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
        } else {
            player.play()
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
        if (_uiState.value.isPlaying) {
            viewModelScope.launch {
                delay(5000)
                if (_uiState.value.isPlaying) {
                    _uiState.value = _uiState.value.copy(showControls = false)
                }
            }
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
