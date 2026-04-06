package com.switchsides.switchstream.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "switchstream_settings")

data class PlaybackSettings(
    val defaultPlaybackSpeed: Float = 1.0f,
    val seekForwardSeconds: Int = 10,
    val seekBackSeconds: Int = 10,
    val autoPlayNextEpisode: Boolean = true,
    val preferredSubtitleLanguage: String = "",
    val preferredAudioLanguage: String = "",
    val pictureInPictureEnabled: Boolean = false,
    val subtitleFontSize: Int = 16,
    val subtitleBackgroundOpacity: Float = 0.5f,
    val lockLandscapeDuringPlayback: Boolean = true,
    val offlineMode: Boolean = false
)

class SettingsManager(private val context: Context) {

    private companion object {
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SEEK_FORWARD = intPreferencesKey("seek_forward_seconds")
        val SEEK_BACK = intPreferencesKey("seek_back_seconds")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SUBTITLE_LANG = stringPreferencesKey("subtitle_language")
        val AUDIO_LANG = stringPreferencesKey("audio_language")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val SUBTITLE_FONT_SIZE = intPreferencesKey("subtitle_font_size")
        val SUBTITLE_BG_OPACITY = floatPreferencesKey("subtitle_bg_opacity")
        val LOCK_LANDSCAPE = booleanPreferencesKey("lock_landscape")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
    }

    val settings: Flow<PlaybackSettings> = context.settingsDataStore.data.map { prefs ->
        PlaybackSettings(
            defaultPlaybackSpeed = prefs[PLAYBACK_SPEED] ?: 1.0f,
            seekForwardSeconds = prefs[SEEK_FORWARD] ?: 10,
            seekBackSeconds = prefs[SEEK_BACK] ?: 10,
            autoPlayNextEpisode = prefs[AUTO_PLAY_NEXT] ?: true,
            preferredSubtitleLanguage = prefs[SUBTITLE_LANG] ?: "",
            preferredAudioLanguage = prefs[AUDIO_LANG] ?: "",
            pictureInPictureEnabled = prefs[PIP_ENABLED] ?: false,
            subtitleFontSize = prefs[SUBTITLE_FONT_SIZE] ?: 16,
            subtitleBackgroundOpacity = prefs[SUBTITLE_BG_OPACITY] ?: 0.5f,
            lockLandscapeDuringPlayback = prefs[LOCK_LANDSCAPE] ?: true,
            offlineMode = prefs[OFFLINE_MODE] ?: false
        )
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        context.settingsDataStore.edit { it[PLAYBACK_SPEED] = speed }
    }

    suspend fun updateSeekForward(seconds: Int) {
        context.settingsDataStore.edit { it[SEEK_FORWARD] = seconds }
    }

    suspend fun updateSeekBack(seconds: Int) {
        context.settingsDataStore.edit { it[SEEK_BACK] = seconds }
    }

    suspend fun updateAutoPlayNext(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun updateSubtitleLanguage(language: String) {
        context.settingsDataStore.edit { it[SUBTITLE_LANG] = language }
    }

    suspend fun updateAudioLanguage(language: String) {
        context.settingsDataStore.edit { it[AUDIO_LANG] = language }
    }

    suspend fun updatePipEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[PIP_ENABLED] = enabled }
    }

    suspend fun updateSubtitleFontSize(size: Int) {
        context.settingsDataStore.edit { it[SUBTITLE_FONT_SIZE] = size }
    }

    suspend fun updateSubtitleBackgroundOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[SUBTITLE_BG_OPACITY] = opacity }
    }

    suspend fun updateLockLandscape(enabled: Boolean) {
        context.settingsDataStore.edit { it[LOCK_LANDSCAPE] = enabled }
    }

    suspend fun updateOfflineMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[OFFLINE_MODE] = enabled }
    }
}
