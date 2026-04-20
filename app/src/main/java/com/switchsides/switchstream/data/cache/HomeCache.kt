package com.switchsides.switchstream.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.api.BaseItemDto
import java.io.File

@Serializable
data class HomeCacheData(
    val libraries: List<BaseItemDto> = emptyList(),
    val continueWatching: List<BaseItemDto> = emptyList(),
    val nextUp: List<BaseItemDto> = emptyList(),
    val featuredItems: List<BaseItemDto> = emptyList(),
    val recentlyAdded: List<BaseItemDto> = emptyList(),
    val favorites: List<BaseItemDto> = emptyList(),
    val latestByLibrary: Map<String, List<BaseItemDto>> = emptyMap(),
    val savedAtEpochMs: Long = 0L
)

class HomeCache(context: Context) {
    private val file = File(context.filesDir, "home_cache.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(): HomeCacheData? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) null
            else json.decodeFromString<HomeCacheData>(file.readText())
        }.getOrNull()
    }

    suspend fun save(data: HomeCacheData) {
        withContext(Dispatchers.IO) {
            runCatching { file.writeText(json.encodeToString(data)) }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            runCatching { file.delete() }
        }
    }
}
