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
import java.util.UUID

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

    private companion object {
        const val DEMO_URL = "https://demo.jellyfin.org/stable"
        const val DEMO_USERNAME = "demo"
    }

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun connect(onSuccess: () -> Unit) {
        val raw = _uiState.value.serverUrl.trim()
        if (raw.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a server address")
            return
        }

        // Auto-prepend protocol: https:// for domains, http:// for IP addresses.
        // Detect existing protocol case-insensitively and canonicalise the scheme.
        val lower = raw.lowercase()
        val withProtocol = when {
            lower.startsWith("https://") -> "https://" + raw.substring("https://".length)
            lower.startsWith("http://") -> "http://" + raw.substring("http://".length)
            else -> {
                val host = raw.split(":").first().split("/").first()
                val looksLikeIp = host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")) || host == "localhost"
                if (looksLikeIp) "http://$raw" else "https://$raw"
            }
        }

        val hostPart = withProtocol
            .substringAfter("://")
            .trimEnd('/')
            .substringBefore('/')
            .substringBefore(':')
        if (hostPart.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a server address")
            return
        }

        val url = withProtocol.trimEnd('/')

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

    fun connectAsDemo(onReady: () -> Unit) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            serverUrl = DEMO_URL
        )

        viewModelScope.launch {
            container.setServerConnection(DEMO_URL)
            val authRepo = container.createAuthRepository()

            authRepo.validateServer().fold(
                onSuccess = { info ->
                    val serverName = info.serverName ?: "Jellyfin Demo"
                    sessionManager.saveServerInfo(DEMO_URL, serverName)

                    authRepo.login(DEMO_USERNAME, "").fold(
                        onSuccess = { result ->
                            val token = result.accessToken ?: ""
                            val userId = result.user?.id ?: UUID.randomUUID()
                            val username = result.user?.name ?: DEMO_USERNAME
                            container.setAuthenticated(DEMO_URL, token, userId)
                            sessionManager.saveSession(
                                serverUrl = DEMO_URL,
                                authToken = token,
                                userId = userId.toString(),
                                serverName = serverName,
                                username = username
                            )
                            sessionManager.cacheUser(
                                serverUrl = DEMO_URL,
                                authToken = token,
                                userId = userId.toString(),
                                username = username
                            )
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                serverName = serverName
                            )
                            onReady()
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Demo login failed: ${e.message}"
                            )
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not reach demo server: ${e.message}"
                    )
                }
            )
        }
    }
}
