package com.switchsides.switchstream.data

import android.content.Context
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo

class JellyfinClient(context: Context) {

    private val jellyfin: Jellyfin = createJellyfin {
        clientInfo = ClientInfo(name = "SwitchStream", version = "1.0.0")
        this.context = context
    }

    fun createApi(serverUrl: String): ApiClient {
        return jellyfin.createApi(baseUrl = serverUrl)
    }

    fun createApi(serverUrl: String, accessToken: String): ApiClient {
        return jellyfin.createApi(
            baseUrl = serverUrl,
            accessToken = accessToken
        )
    }
}
