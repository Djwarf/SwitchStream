package com.switchsides.switchstream.data.model

enum class TrackType { AUDIO, SUBTITLE }

data class MediaTrackInfo(
    val index: Int,
    val title: String,
    val language: String?,
    val codec: String?,
    val isDefault: Boolean,
    val type: TrackType
)
