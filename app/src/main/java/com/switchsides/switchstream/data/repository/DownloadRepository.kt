package com.switchsides.switchstream.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.switchsides.switchstream.data.DownloadWorker
import com.switchsides.switchstream.data.SettingsManager
import com.switchsides.switchstream.data.db.AppDatabase
import com.switchsides.switchstream.data.db.DownloadState
import com.switchsides.switchstream.data.db.DownloadedMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsManager: SettingsManager? = null
) {
    private val dao = database.downloadDao()

    fun getAllDownloads(): Flow<List<DownloadedMedia>> = dao.getAll()

    fun getCompletedDownloads(): Flow<List<DownloadedMedia>> = dao.getCompleted()

    fun observeDownload(itemId: String): Flow<DownloadedMedia?> = dao.observeByItemId(itemId)

    suspend fun getDownload(itemId: String): DownloadedMedia? = dao.getByItemId(itemId)

    suspend fun getLocalFileUri(itemId: String): Uri? {
        val stored = dao.getByItemId(itemId)?.filePath
        if (!stored.isNullOrBlank() && stored.startsWith("content://")) {
            val uri = Uri.parse(stored)
            val doc = DocumentFile.fromSingleUri(context, uri)
            if (doc?.exists() == true) return uri
        }
        val file = File(getDownloadDir(), "$itemId.mp4")
        return if (file.exists()) Uri.fromFile(file) else null
    }

    suspend fun startDownload(
        itemId: String,
        title: String,
        streamUrl: String,
        thumbnailUrl: String,
        mediaType: String,
        seriesName: String? = null,
        accessToken: String,
        silent: Boolean = false
    ) {
        val filePath = File(getDownloadDir(), "$itemId.mp4").absolutePath
        val treeUri = settingsManager?.settings?.first()?.downloadLocationTreeUri.orEmpty()

        dao.insert(
            DownloadedMedia(
                itemId = itemId,
                title = title,
                filePath = filePath,
                thumbnailUrl = thumbnailUrl,
                downloadState = DownloadState.QUEUED,
                mediaType = mediaType,
                seriesName = seriesName,
                updatedAtMs = System.currentTimeMillis()
            )
        )

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    "itemId" to itemId,
                    "title" to title,
                    "streamUrl" to streamUrl,
                    "filePath" to filePath,
                    "accessToken" to accessToken,
                    "silent" to silent,
                    "targetTreeUri" to treeUri
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .addTag("download")
            .addTag("download_$itemId")
            .build()

        // Unique per itemId + REPLACE so re-queuing cancels the prior attempt cleanly.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_$itemId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun cancelDownload(itemId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("download_$itemId")
        deleteStoredArtifacts(itemId)
        dao.deleteByItemId(itemId)
    }

    suspend fun deleteDownload(itemId: String) {
        deleteStoredArtifacts(itemId)
        dao.deleteByItemId(itemId)
    }

    private suspend fun deleteStoredArtifacts(itemId: String) {
        val stored = dao.getByItemId(itemId)?.filePath
        if (!stored.isNullOrBlank() && stored.startsWith("content://")) {
            runCatching {
                DocumentFile.fromSingleUri(context, Uri.parse(stored))?.delete()
            }
        }
        val file = File(getDownloadDir(), "$itemId.mp4")
        if (file.exists()) file.delete()
        for (i in 0 until 4) {
            File(getDownloadDir(), "$itemId.mp4.part$i").takeIf { it.exists() }?.delete()
        }
    }

    private fun getDownloadDir(): File {
        val dir = File(context.getExternalFilesDir(null), "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
