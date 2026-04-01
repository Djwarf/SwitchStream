package com.example.switchstream.di

import android.content.Context
import com.example.switchstream.data.JellyfinClient
import com.example.switchstream.data.SessionManager
import com.example.switchstream.data.SettingsManager
import com.example.switchstream.data.repository.AuthRepository
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.data.repository.LibraryRepository
import com.example.switchstream.data.NetworkMonitor
import com.example.switchstream.data.db.AppDatabase
import com.example.switchstream.data.repository.DownloadRepository
import com.example.switchstream.data.repository.PlaybackRepository
import org.jellyfin.sdk.api.client.ApiClient
import java.util.UUID

class AppContainer(private val context: Context) {

    val jellyfinClient = JellyfinClient(context)
    val sessionManager = SessionManager(context)
    val settingsManager = SettingsManager(context)
    val database = AppDatabase.getInstance(context)
    val networkMonitor = NetworkMonitor(context)
    val downloadRepository = DownloadRepository(context, database)

    var apiClient: ApiClient? = null
        private set

    var userId: UUID? = null
        private set

    var serverUrl: String = ""
        private set

    var accessToken: String = ""
        private set

    fun setServerConnection(serverUrl: String) {
        this.serverUrl = serverUrl
        this.apiClient = jellyfinClient.createApi(serverUrl)
    }

    fun setAuthenticated(serverUrl: String, accessToken: String, userId: UUID) {
        this.serverUrl = serverUrl
        this.userId = userId
        this.accessToken = accessToken
        this.apiClient = jellyfinClient.createApi(serverUrl, accessToken)
    }

    fun createAuthRepository(): AuthRepository {
        return AuthRepository(requireNotNull(apiClient) { "API client not initialized" })
    }

    fun createImageRepository(): ImageRepository {
        return ImageRepository(serverUrl)
    }

    fun createLibraryRepository(): LibraryRepository {
        return LibraryRepository(
            requireNotNull(apiClient) { "API client not initialized" },
            requireNotNull(userId) { "User ID not set" }
        )
    }

    fun createPlaybackRepository(): PlaybackRepository {
        return PlaybackRepository(
            requireNotNull(apiClient) { "API client not initialized" },
            serverUrl,
            requireNotNull(userId) { "User ID not set" }
        )
    }

    fun clear() {
        apiClient = null
        userId = null
        serverUrl = ""
    }
}
