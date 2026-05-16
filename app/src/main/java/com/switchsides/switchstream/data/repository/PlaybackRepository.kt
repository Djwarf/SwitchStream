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
     * Returns intro/credits timestamps in ms by scanning Jellyfin chapter markers.
     *
     * Matching rules — designed so the Skip Intro overlay only ever fires on the
     * real opening sequence and Skip Credits only on the real closing credits:
     *
     * - The intro chapter is the FIRST chapter whose name contains an intro keyword
     *   AND whose start is within the first 40 % of the episode runtime. Without
     *   the position guard a mid-episode "Opening of Act 2" would otherwise pose
     *   as the intro.
     * - The credits chapter is the LAST chapter whose name contains a credits
     *   keyword AND whose start is within the last 40 % of the runtime — same
     *   guard against mid-episode references.
     * - The keyword sets cover the chapter-name conventions different rippers use:
     *   "Theme Song", "Title Sequence", "Closing Credits", "Outro", "Tag", etc.
     *
     * Returns null when neither side fires so callers can keep the overlays hidden.
     */
    suspend fun getIntroTimestamps(itemId: UUID): Result<IntroTimestamps?> = runCatching {
        val response = apiClient.userLibraryApi.getItem(
            itemId = itemId,
            userId = userId
        )
        val chapters = response.content.chapters.orEmpty()
        if (chapters.isEmpty()) return@runCatching null

        // Episode duration in ms; without it we can't anchor the position guards,
        // so fall back to using the last chapter's start as an approximation.
        val runtimeMs = (response.content.runTimeTicks ?: 0L) / 10_000
        val approxDurationMs = if (runtimeMs > 0L) runtimeMs else {
            (chapters.last().startPositionTicks ?: 0L) / 10_000
        }
        if (approxDurationMs <= 0L) return@runCatching null

        // Build (name, startMs, endMs) triples — endMs is the next chapter's start
        // (or approxDurationMs for the last chapter).
        data class ChapterSpan(val nameLower: String, val startMs: Long, val endMs: Long)
        val spans = chapters.mapIndexed { i, ch ->
            val start = (ch.startPositionTicks ?: 0L) / 10_000
            val end = chapters.getOrNull(i + 1)?.startPositionTicks?.let { it / 10_000 }
                ?: approxDurationMs
            ChapterSpan(
                nameLower = ch.name?.lowercase().orEmpty(),
                startMs = start,
                endMs = end
            )
        }

        val introKeywords = listOf(
            "intro", "introduction", "opening", "theme", "theme song",
            "title", "title sequence", "titles", "opening titles", "main title"
        )
        val creditsKeywords = listOf(
            "credit", "credits", "end credits", "closing", "closing credits",
            "outro", "ending", "end title", "tag"
        )
        fun ChapterSpan.matchesAny(keywords: List<String>): Boolean =
            keywords.any { nameLower.contains(it) }

        val introWindow = (approxDurationMs * 4 / 10).coerceAtLeast(1L)
        val creditsThreshold = approxDurationMs * 6 / 10

        // First intro-like chapter within the first 40 % of runtime.
        val introSpan = spans.firstOrNull {
            it.startMs <= introWindow && it.matchesAny(introKeywords)
        }
        // Last credits-like chapter that starts in the back 40 % of runtime.
        val creditsSpan = spans.lastOrNull {
            it.startMs >= creditsThreshold && it.matchesAny(creditsKeywords)
        }

        val introStartMs = introSpan?.startMs
        val introEndMs = introSpan?.endMs
        val creditsStartMs = creditsSpan?.startMs

        if (introStartMs == null && creditsStartMs == null) null
        else IntroTimestamps(
            introStartMs = introStartMs,
            introEndMs = introEndMs,
            creditsStartMs = creditsStartMs
        )
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
    val introStartMs: Long? = null,
    val introEndMs: Long? = null,
    val creditsStartMs: Long? = null
)
