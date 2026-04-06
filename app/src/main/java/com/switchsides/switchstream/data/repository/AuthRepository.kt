package com.switchsides.switchstream.data.repository

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.AuthenticationResult
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.QuickConnectDto
import org.jellyfin.sdk.model.api.QuickConnectResult

class AuthRepository(private val apiClient: ApiClient) {

    suspend fun validateServer(): Result<PublicSystemInfo> = runCatching {
        val response = apiClient.systemApi.getPublicSystemInfo()
        response.content
    }

    suspend fun login(username: String, password: String): Result<AuthenticationResult> = runCatching {
        val response = apiClient.userApi.authenticateUserByName(
            data = AuthenticateUserByName(username = username, pw = password)
        )
        response.content
    }

    suspend fun initiateQuickConnect(): Result<QuickConnectResult> = runCatching {
        val response = apiClient.quickConnectApi.initiateQuickConnect()
        response.content
    }

    suspend fun checkQuickConnect(secret: String): Result<QuickConnectResult> = runCatching {
        val response = apiClient.quickConnectApi.getQuickConnectState(secret = secret)
        response.content
    }

    suspend fun authenticateWithQuickConnect(secret: String): Result<AuthenticationResult> = runCatching {
        val response = apiClient.userApi.authenticateWithQuickConnect(
            data = QuickConnectDto(secret = secret)
        )
        response.content
    }
}
