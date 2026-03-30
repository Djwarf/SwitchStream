package com.example.switchstream.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "switchstream_session")

data class SessionData(
    val serverUrl: String,
    val authToken: String,
    val userId: String,
    val serverName: String,
    val username: String = ""
)

data class RecentServer(
    val url: String,
    val name: String
)

class SessionManager(private val context: Context) {

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = stringPreferencesKey("user_id")
        val SERVER_NAME = stringPreferencesKey("server_name")
        val USERNAME = stringPreferencesKey("username")
        val RECENT_SERVERS = stringPreferencesKey("recent_servers")
    }

    val session: Flow<SessionData?> = context.dataStore.data.map { prefs ->
        val serverUrl = prefs[SERVER_URL] ?: return@map null
        val authToken = prefs[AUTH_TOKEN] ?: return@map null
        val userId = prefs[USER_ID] ?: return@map null
        val serverName = prefs[SERVER_NAME] ?: ""
        val username = prefs[USERNAME] ?: ""
        SessionData(serverUrl, authToken, userId, serverName, username)
    }

    suspend fun saveSession(
        serverUrl: String,
        authToken: String,
        userId: String,
        serverName: String,
        username: String = ""
    ) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
            prefs[AUTH_TOKEN] = authToken
            prefs[USER_ID] = userId
            prefs[SERVER_NAME] = serverName
            prefs[USERNAME] = username
        }
        addRecentServer(serverUrl, serverName)
    }

    suspend fun saveServerInfo(serverUrl: String, serverName: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
            prefs[SERVER_NAME] = serverName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(AUTH_TOKEN)
            it.remove(USER_ID)
            it.remove(USERNAME)
        }
    }

    val recentServers: Flow<List<RecentServer>> = context.dataStore.data.map { prefs ->
        val json = prefs[RECENT_SERVERS] ?: return@map emptyList()
        json.split("||").mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) RecentServer(parts[0], parts[1]) else null
        }
    }

    private suspend fun addRecentServer(url: String, name: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[RECENT_SERVERS] ?: ""
            val servers = existing.split("||").filter { it.isNotBlank() }.toMutableList()
            servers.removeAll { it.startsWith("$url|") }
            servers.add(0, "$url|$name")
            prefs[RECENT_SERVERS] = servers.take(5).joinToString("||")
        }
    }
}
