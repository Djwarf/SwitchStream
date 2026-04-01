package com.example.switchstream.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.db.DownloadedMedia
import com.example.switchstream.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val downloads: List<DownloadedMedia> = emptyList()
)

class DownloadsViewModel(
    private val downloadRepo: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            downloadRepo.getAllDownloads().collect { downloads ->
                _uiState.value = _uiState.value.copy(downloads = downloads)
            }
        }
    }

    fun deleteDownload(itemId: String) {
        viewModelScope.launch {
            downloadRepo.deleteDownload(itemId)
        }
    }
}
