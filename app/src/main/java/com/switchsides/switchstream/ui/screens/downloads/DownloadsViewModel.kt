package com.switchsides.switchstream.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.SettingsManager
import com.switchsides.switchstream.data.db.DownloadedMedia
import com.switchsides.switchstream.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val downloads: List<DownloadedMedia> = emptyList(),
    val downloadLocationTreeUri: String = "",
    val downloadsWifiOnly: Boolean = false
)

class DownloadsViewModel(
    private val downloadRepo: DownloadRepository,
    private val serverUrl: String = "",
    private val accessToken: String = "",
    private val settingsManager: SettingsManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepo.getAllDownloads().collect { downloads ->
                // Surface active/queued/failed first, completed last; within each, newest first.
                val sorted = downloads.sortedWith(
                    compareBy<DownloadedMedia> { order(it) }.thenByDescending { it.dateAdded }
                )
                _uiState.value = _uiState.value.copy(downloads = sorted)
            }
        }

        settingsManager?.let { sm ->
            viewModelScope.launch {
                sm.settings.collect { s ->
                    _uiState.value = _uiState.value.copy(
                        downloadLocationTreeUri = s.downloadLocationTreeUri,
                        downloadsWifiOnly = s.downloadsWifiOnly
                    )
                }
            }
        }
    }

    private fun order(d: DownloadedMedia): Int = when (d.downloadState) {
        com.switchsides.switchstream.data.db.DownloadState.DOWNLOADING -> 0
        com.switchsides.switchstream.data.db.DownloadState.QUEUED -> 1
        com.switchsides.switchstream.data.db.DownloadState.FAILED -> 2
        com.switchsides.switchstream.data.db.DownloadState.COMPLETE -> 3
    }

    fun deleteDownload(itemId: String) {
        viewModelScope.launch { downloadRepo.deleteDownload(itemId) }
    }

    fun cancelDownload(itemId: String) {
        viewModelScope.launch { downloadRepo.cancelDownload(itemId) }
    }

    fun retryDownload(item: DownloadedMedia) {
        if (serverUrl.isBlank()) return
        viewModelScope.launch {
            val quality = settingsManager?.settings?.first()?.downloadQuality ?: 0
            val streamUrl = if (quality == 0) {
                "$serverUrl/Videos/${item.itemId}/stream?static=true&mediaSourceId=${item.itemId}"
            } else {
                val bitrate = when {
                    quality >= 1080 -> 8_000_000
                    quality >= 720 -> 3_000_000
                    quality >= 480 -> 1_000_000
                    else -> 800_000
                }
                "$serverUrl/Videos/${item.itemId}/master.m3u8" +
                    "?mediaSourceId=${item.itemId}" +
                    "&maxHeight=$quality" +
                    "&maxStreamingBitrate=$bitrate" +
                    "&videoCodec=h264" +
                    "&audioCodec=aac" +
                    "&api_key=$accessToken"
            }
            downloadRepo.startDownload(
                itemId = item.itemId,
                title = item.title,
                streamUrl = streamUrl,
                thumbnailUrl = item.thumbnailUrl,
                mediaType = item.mediaType,
                seriesName = item.seriesName,
                accessToken = accessToken,
                silent = false
            )
        }
    }
}
