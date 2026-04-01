package com.example.switchstream.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.switchstream.data.DownloadWorker
import com.example.switchstream.data.db.AppDatabase
import com.example.switchstream.data.db.DownloadState
import com.example.switchstream.data.db.DownloadedMedia
import kotlinx.coroutines.flow.Flow
import java.io.File

class DownloadRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val dao = database.downloadDao()

    fun getAllDownloads(): Flow<List<DownloadedMedia>> = dao.getAll()

    fun getCompletedDownloads(): Flow<List<DownloadedMedia>> = dao.getCompleted()

    fun observeDownload(itemId: String): Flow<DownloadedMedia?> = dao.observeByItemId(itemId)

    suspend fun getDownload(itemId: String): DownloadedMedia? = dao.getByItemId(itemId)

    fun getLocalFileUri(itemId: String): Uri? {
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
        accessToken: String
    ) {
        val filePath = File(getDownloadDir(), "$itemId.mp4").absolutePath

        dao.insert(
            DownloadedMedia(
                itemId = itemId,
                title = title,
                filePath = filePath,
                thumbnailUrl = thumbnailUrl,
                downloadState = DownloadState.QUEUED,
                mediaType = mediaType,
                seriesName = seriesName
            )
        )

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    "itemId" to itemId,
                    "streamUrl" to streamUrl,
                    "filePath" to filePath,
                    "accessToken" to accessToken
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("download_$itemId")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    suspend fun cancelDownload(itemId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("download_$itemId")
        val file = File(getDownloadDir(), "$itemId.mp4")
        if (file.exists()) file.delete()
        dao.deleteByItemId(itemId)
    }

    suspend fun deleteDownload(itemId: String) {
        val file = File(getDownloadDir(), "$itemId.mp4")
        if (file.exists()) file.delete()
        dao.deleteByItemId(itemId)
    }

    private fun getDownloadDir(): File {
        val dir = File(context.getExternalFilesDir(null), "downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
