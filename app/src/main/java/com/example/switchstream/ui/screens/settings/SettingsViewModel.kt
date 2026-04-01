package com.example.switchstream.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.PlaybackSettings
import com.example.switchstream.data.SessionManager
import com.example.switchstream.data.SettingsManager
import com.example.switchstream.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverName: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val playbackSettings: PlaybackSettings = PlaybackSettings()
)

class SettingsViewModel(
    private val sessionManager: SessionManager,
    private val settingsManager: SettingsManager,
    private val appContainer: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                if (session != null) {
                    _uiState.value = _uiState.value.copy(
                        serverName = session.serverName,
                        serverUrl = session.serverUrl,
                        username = session.username
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsManager.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(playbackSettings = settings)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sessionManager.clearSession()
            appContainer.clear()
        }
    }

    fun switchServer() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }

    fun switchUser() {
        viewModelScope.launch {
            // Cache current session before switching
            val currentSession = _uiState.value
            if (currentSession.serverUrl.isNotEmpty()) {
                val session = sessionManager.session.firstOrNull()
                if (session != null) {
                    sessionManager.cacheUser(
                        serverUrl = session.serverUrl,
                        authToken = session.authToken,
                        userId = session.userId,
                        username = session.username
                    )
                }
            }
            sessionManager.clearSession()
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsManager.updatePlaybackSpeed(speed) }
    }

    fun updateSeekForward(seconds: Int) {
        viewModelScope.launch { settingsManager.updateSeekForward(seconds) }
    }

    fun updateSeekBack(seconds: Int) {
        viewModelScope.launch { settingsManager.updateSeekBack(seconds) }
    }

    fun updateAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch { settingsManager.updateAutoPlayNext(enabled) }
    }

    fun updatePipEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.updatePipEnabled(enabled) }
    }

    fun updateSubtitleFontSize(size: Int) {
        viewModelScope.launch { settingsManager.updateSubtitleFontSize(size) }
    }

    fun updateSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch { settingsManager.updateSubtitleBackgroundOpacity(opacity) }
    }

    fun updateLockLandscape(enabled: Boolean) {
        viewModelScope.launch { settingsManager.updateLockLandscape(enabled) }
    }
}
