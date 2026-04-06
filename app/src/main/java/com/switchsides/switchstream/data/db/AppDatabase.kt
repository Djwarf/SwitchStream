package com.switchsides.switchstream.data.db

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AppDatabase private constructor(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file get() = File(context.filesDir, "downloads.json")
    private val _data = MutableStateFlow(loadFromDisk())
    private val lock = Any()

    private fun loadFromDisk(): List<DownloadedMedia> {
        return try {
            if (file.exists()) {
                json.decodeFromString<List<SerializableDownload>>(file.readText()).map { it.toEntity() }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToDisk(items: List<DownloadedMedia>) {
        try { file.writeText(json.encodeToString(items.map { it.toSerializable() })) } catch (_: Exception) {}
    }

    fun downloadDao(): DownloadDao = DaoImpl()

    private inner class DaoImpl : DownloadDao {
        override fun getAll(): Flow<List<DownloadedMedia>> = _data

        override fun getCompleted(): Flow<List<DownloadedMedia>> =
            _data.map { list -> list.filter { it.downloadState == DownloadState.COMPLETE } }

        override suspend fun getByItemId(itemId: String): DownloadedMedia? =
            _data.value.find { it.itemId == itemId }

        override fun observeByItemId(itemId: String): Flow<DownloadedMedia?> =
            _data.map { list -> list.find { it.itemId == itemId } }

        override suspend fun insert(media: DownloadedMedia) {
            synchronized(lock) {
                val updated = _data.value.filter { it.itemId != media.itemId } + media
                _data.value = updated
                saveToDisk(updated)
            }
        }

        override suspend fun update(media: DownloadedMedia) {
            synchronized(lock) {
                val updated = _data.value.map { if (it.itemId == media.itemId) media else it }
                _data.value = updated
                saveToDisk(updated)
            }
        }

        // Atomic update: read + modify in one step to avoid race conditions
        suspend fun updateByItemId(itemId: String, transform: (DownloadedMedia) -> DownloadedMedia) {
            synchronized(lock) {
                val updated = _data.value.map { if (it.itemId == itemId) transform(it) else it }
                _data.value = updated
                saveToDisk(updated)
            }
        }

        override suspend fun delete(media: DownloadedMedia) {
            synchronized(lock) {
                val updated = _data.value.filter { it.itemId != media.itemId }
                _data.value = updated
                saveToDisk(updated)
            }
        }

        override suspend fun deleteByItemId(itemId: String) {
            synchronized(lock) {
                val updated = _data.value.filter { it.itemId != itemId }
                _data.value = updated
                saveToDisk(updated)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

@Serializable
private data class SerializableDownload(
    val itemId: String,
    val title: String,
    val filePath: String = "",
    val thumbnailUrl: String = "",
    val downloadState: String = "QUEUED",
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val mediaType: String = "",
    val seriesName: String? = null,
    val dateAdded: Long = 0
) {
    fun toEntity() = DownloadedMedia(
        itemId = itemId,
        title = title,
        filePath = filePath,
        thumbnailUrl = thumbnailUrl,
        downloadState = try { DownloadState.valueOf(downloadState) } catch (_: Exception) { DownloadState.QUEUED },
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        mediaType = mediaType,
        seriesName = seriesName,
        dateAdded = dateAdded
    )
}

private fun DownloadedMedia.toSerializable() = SerializableDownload(
    itemId = itemId,
    title = title,
    filePath = filePath,
    thumbnailUrl = thumbnailUrl,
    downloadState = downloadState.name,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    mediaType = mediaType,
    seriesName = seriesName,
    dateAdded = dateAdded
)
