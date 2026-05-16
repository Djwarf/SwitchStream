package com.switchsides.switchstream.data.repository

import com.switchsides.switchstream.data.model.MediaTrackInfo
import com.switchsides.switchstream.data.model.TrackType
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.RepeatMode
import java.util.UUID

class PlaybackRepository(
    private val apiClient: ApiClient,
    private val serverUrl: String,
    private val userId: UUID
) {

    /**
     * Build a playback URL for [itemId]. When [maxHeight] is 0 and no audio/subtitle
     * override is set we hand the raw file to the client via `static=true` direct-play —
     * the most efficient path, and the file's multiplexed tracks are available as-is.
     *
     * Once the user picks a specific audio or subtitle stream, however, direct-play
     * can't honour `audioStreamIndex` / `subtitleStreamIndex` (the file is shipped
     * verbatim), so we fall back to the HLS transcode endpoint and tell Jellyfin which
     * source streams to bake in. Subtitles use `subtitleMethod=Embed` to burn them into
     * the video stream — the player has no separate text track in this mode.
     */
    fun getStreamUrl(
        itemId: UUID,
        maxHeight: Int = 0,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): String {
        val hasTrackOverride = audioStreamIndex != null || subtitleStreamIndex != null
        if (maxHeight == 0 && !hasTrackOverride) {
            return "$serverUrl/Videos/$itemId/stream?static=true&mediaSourceId=$itemId"
        }
        val targetHeight = if (maxHeight == 0) 1080 else maxHeight
        val bitrate = when {
            targetHeight >= 1080 -> 8_000_000
            targetHeight >= 720 -> 3_000_000
            targetHeight >= 480 -> 1_000_000
            else -> 800_000
        }
        val sb = StringBuilder("$serverUrl/Videos/$itemId/master.m3u8")
            .append("?mediaSourceId=$itemId")
            .append("&maxHeight=$targetHeight")
            .append("&maxStreamingBitrate=$bitrate")
            .append("&videoCodec=h264")
            .append("&audioCodec=aac")
        audioStreamIndex?.let { sb.append("&audioStreamIndex=$it") }
        subtitleStreamIndex?.let {
            sb.append("&subtitleStreamIndex=$it")
            sb.append("&subtitleMethod=Embed")
        }
        sb.append("&api_key=${apiClient.accessToken.orEmpty()}")
        return sb.toString()
    }

    fun getHlsUrl(itemId: UUID): String {
        return "$serverUrl/Videos/$itemId/master.m3u8?mediaSourceId=$itemId"
    }

    suspend fun getMediaTracks(itemId: UUID): Result<List<MediaTrackInfo>> = runCatching {
        val response = apiClient.mediaInfoApi.getPlaybackInfo(
            itemId = itemId,
            userId = userId
        )
        val mediaSources = response.content.mediaSources.orEmpty()
        val streams = mediaSources.firstOrNull()?.mediaStreams.orEmpty()

        streams.mapNotNull { stream ->
            val trackType = when (stream.type) {
                MediaStreamType.AUDIO -> TrackType.AUDIO
                MediaStreamType.SUBTITLE -> TrackType.SUBTITLE
                else -> return@mapNotNull null
            }
            MediaTrackInfo(
                index = stream.index ?: 0,
                title = stream.displayTitle ?: stream.title ?: stream.language ?: "Track ${stream.index}",
                language = stream.language,
                codec = stream.codec,
                isDefault = stream.isDefault == true,
                type = trackType
            )
        }
    }

    /**
     * Returns intro end timestamp in ms, or null if no intro chapter found.
     * Checks Jellyfin chapter markers for "Introduction" or "Intro" chapters.
     */
    suspend fun getIntroTimestamps(itemId: UUID): Result<IntroTimestamps?> = runCatching {
        val response = apiClient.userLibraryApi.getItem(
            itemId = itemId,
            userId = userId
        )
        val chapters = response.content.chapters.orEmpty()

        var introEnd: Long? = null
        var creditsStart: Long? = null

        for ((index, chapter) in chapters.withIndex()) {
            val name = chapter.name?.lowercase() ?: ""
            val startTicks = chapter.startPositionTicks ?: 0

            if (name.contains("intro") || name.contains("introduction") || name.contains("opening")) {
                // Intro end is the start of the next chapter
                val nextChapter = chapters.getOrNull(index + 1)
                introEnd = (nextChapter?.startPositionTicks ?: startTicks)  / 10_000
            }

            if (name.contains("credit") || name.contains("outro") || name.contains("ending")) {
                creditsStart = startTicks / 10_000
            }
        }

        if (introEnd != null || creditsStart != null) {
            IntroTimestamps(introEndMs = introEnd, creditsStartMs = creditsStart)
        } else null
    }

    suspend fun reportPlaybackStart(itemId: UUID): Result<Unit> = runCatching {
        apiClient.playStateApi.reportPlaybackStart(
            data = PlaybackStartInfo(
                itemId = itemId,
                canSeek = true,
                isPaused = false,
                isMuted = false,
                playMethod = PlayMethod.DIRECT_PLAY,
                repeatMode = RepeatMode.REPEAT_NONE,
                playbackOrder = PlaybackOrder.DEFAULT
            )
        )
        Unit
    }

    suspend fun reportPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean = false
    ): Result<Unit> = runCatching {
        apiClient.playStateApi.reportPlaybackProgress(
            data = PlaybackProgressInfo(
                itemId = itemId,
                positionTicks = positionTicks,
                isPaused = isPaused,
                canSeek = true,
                isMuted = false,
                playMethod = PlayMethod.DIRECT_PLAY,
                repeatMode = RepeatMode.REPEAT_NONE,
                playbackOrder = PlaybackOrder.DEFAULT
            )
        )
        Unit
    }

    suspend fun reportPlaybackStopped(
        itemId: UUID,
        positionTicks: Long
    ): Result<Unit> = runCatching {
        apiClient.playStateApi.reportPlaybackStopped(
            data = PlaybackStopInfo(
                itemId = itemId,
                positionTicks = positionTicks,
                failed = false
            )
        )
        Unit
    }
}

data class IntroTimestamps(
    val introEndMs: Long?,
    val creditsStartMs: Long?
)
