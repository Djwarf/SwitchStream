package com.example.switchstream.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.CachedUser
import com.example.switchstream.data.SessionManager
import com.example.switchstream.data.repository.AuthRepository
import com.example.switchstream.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverName: String = "",
    val activeTab: LoginTab = LoginTab.QUICK_CONNECT,
    val quickConnectCode: String? = null,
    val quickConnectAvailable: Boolean = true,
    val quickConnectPolling: Boolean = false,
    val cachedUsers: List<CachedUser> = emptyList()
)

enum class LoginTab { PASSWORD, QUICK_CONNECT }

class LoginViewModel(
    private val container: AppContainer,
    private val sessionManager: SessionManager,
    serverName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(serverName = serverName))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var quickConnectJob: Job? = null
    private var quickConnectSecret: String? = null

    private val authRepo: AuthRepository = container.createAuthRepository()

    init {
        loadCachedUsers()
        startQuickConnect()
    }

    private fun loadCachedUsers() {
        viewModelScope.launch {
            val users = sessionManager.getCachedUsers(container.serverUrl)
            _uiState.value = _uiState.value.copy(cachedUsers = users)
        }
    }

    fun switchToCachedUser(user: CachedUser, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            container.setAuthenticated(user.serverUrl, user.authToken, UUID.fromString(user.userId))
            sessionManager.saveSession(
                serverUrl = user.serverUrl,
                authToken = user.authToken,
                userId = user.userId,
                serverName = _uiState.value.serverName,
                username = user.username
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            onSuccess()
        }
    }

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun setTab(tab: LoginTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab, error = null)
        if (tab == LoginTab.QUICK_CONNECT && _uiState.value.quickConnectCode == null) {
            startQuickConnect()
        }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.value = state.copy(error = "Please enter a username")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            authRepo.login(state.username, state.password).fold(
                onSuccess = { result ->
                    val token = result.accessToken ?: ""
                    val userId = result.user?.id ?: UUID.randomUUID()
                    val username = result.user?.name ?: state.username
                    container.setAuthenticated(container.serverUrl, token, userId)
                    sessionManager.saveSession(
                        serverUrl = container.serverUrl,
                        authToken = token,
                        userId = userId.toString(),
                        serverName = state.serverName,
                        username = username
                    )
                    sessionManager.cacheUser(
                        serverUrl = container.serverUrl,
                        authToken = token,
                        userId = userId.toString(),
                        username = username
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Login failed: ${e.message}"
                    )
                }
            )
        }
    }

    private fun startQuickConnect() {
        quickConnectJob?.cancel()
        _uiState.value = _uiState.value.copy(quickConnectPolling = true, error = null)

        quickConnectJob = viewModelScope.launch {
            authRepo.initiateQuickConnect().fold(
                onSuccess = { result ->
                    quickConnectSecret = result.secret
                    _uiState.value = _uiState.value.copy(
                        quickConnectCode = result.code,
                        quickConnectAvailable = true
                    )
                    pollQuickConnect(result.secret!!)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        quickConnectAvailable = false,
                        quickConnectPolling = false,
                        activeTab = LoginTab.PASSWORD,
                        error = "Quick Connect is not available on this server"
                    )
                }
            )
        }
    }

    private suspend fun pollQuickConnect(secret: String) {
        while (viewModelScope.isActive) {
            delay(5000)
            authRepo.checkQuickConnect(secret).fold(
                onSuccess = { result ->
                    if (result.authenticated == true) {
                        authenticateQuickConnect(secret)
                        return
                    }
                },
                onFailure = { /* keep polling */ }
            )
        }
    }

    private suspend fun authenticateQuickConnect(secret: String) {
        authRepo.authenticateWithQuickConnect(secret).fold(
            onSuccess = { result ->
                val token = result.accessToken ?: ""
                val userId = result.user?.id ?: UUID.randomUUID()
                val username = result.user?.name ?: ""
                container.setAuthenticated(container.serverUrl, token, userId)
                sessionManager.saveSession(
                    serverUrl = container.serverUrl,
                    authToken = token,
                    userId = userId.toString(),
                    serverName = _uiState.value.serverName,
                    username = username
                )
                sessionManager.cacheUser(
                    serverUrl = container.serverUrl,
                    authToken = token,
                    userId = userId.toString(),
                    username = username
                )
                _uiState.value = _uiState.value.copy(quickConnectPolling = false)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    quickConnectPolling = false,
                    error = "Quick Connect auth failed: ${e.message}"
                )
            }
        )
    }

    override fun onCleared() {
        quickConnectJob?.cancel()
        super.onCleared()
    }
}
