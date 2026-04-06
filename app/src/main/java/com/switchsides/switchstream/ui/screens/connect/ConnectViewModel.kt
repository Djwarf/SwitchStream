package com.switchsides.switchstream.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.SessionManager
import com.switchsides.switchstream.data.repository.AuthRepository
import com.switchsides.switchstream.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectUiState(
    val serverUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverName: String? = null
)

class ConnectViewModel(
    private val container: AppContainer,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun connect(onSuccess: () -> Unit) {
        val raw = _uiState.value.serverUrl.trimEnd('/')
        if (raw.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a server address")
            return
        }

        // Auto-prepend protocol: https:// for domains, http:// for IP addresses
        val url = if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            val host = raw.split(":").first().split("/").first()
            val looksLikeIp = host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")) || host == "localhost"
            if (looksLikeIp) "http://$raw" else "https://$raw"
        } else raw

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, serverUrl = url)

        viewModelScope.launch {
            container.setServerConnection(url)
            val authRepo = container.createAuthRepository()

            authRepo.validateServer().fold(
                onSuccess = { info ->
                    val name = info.serverName ?: "Jellyfin Server"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        serverName = name
                    )
                    viewModelScope.launch {
                        sessionManager.saveServerInfo(url, name)
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not connect: ${e.message}"
                    )
                }
            )
        }
    }
}
