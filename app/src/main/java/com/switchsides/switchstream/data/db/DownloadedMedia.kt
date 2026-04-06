package com.switchsides.switchstream.data.db

enum class DownloadState {
    QUEUED, DOWNLOADING, COMPLETE, FAILED
}

data class DownloadedMedia(
    val itemId: String,
    val title: String,
    val filePath: String = "",
    val thumbnailUrl: String = "",
    val downloadState: DownloadState = DownloadState.QUEUED,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val mediaType: String = "",
    val seriesName: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)
