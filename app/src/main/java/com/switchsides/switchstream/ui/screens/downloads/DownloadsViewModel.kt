package com.switchsides.switchstream.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.db.DownloadedMedia
import com.switchsides.switchstream.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val downloads: List<DownloadedMedia> = emptyList()
)

class DownloadsViewModel(
    private val downloadRepo: DownloadRepository,
    private val serverUrl: String = "",
    private val accessToken: String = ""
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
            val streamUrl = "$serverUrl/Videos/${item.itemId}/stream?static=true&mediaSourceId=${item.itemId}"
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
