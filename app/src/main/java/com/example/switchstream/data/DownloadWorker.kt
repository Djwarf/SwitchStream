package com.example.switchstream.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.switchstream.data.db.AppDatabase
import com.example.switchstream.data.db.DownloadState
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString("itemId") ?: return Result.failure()
        val streamUrl = inputData.getString("streamUrl") ?: return Result.failure()
        val filePath = inputData.getString("filePath") ?: return Result.failure()
        val accessToken = inputData.getString("accessToken") ?: ""

        val dao = AppDatabase.getInstance(applicationContext).downloadDao()

        return try {
            dao.getByItemId(itemId)?.let {
                dao.update(it.copy(downloadState = DownloadState.DOWNLOADING))
            }

            val url = URL(streamUrl)
            val connection = url.openConnection() as HttpURLConnection
            if (accessToken.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$accessToken\"")
            }
            connection.connect()

            val totalBytes = connection.contentLengthLong
            val file = File(filePath)
            file.parentFile?.mkdirs()

            var downloadedBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            file.delete()
                            return Result.failure()
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Update progress every 1MB
                        if (downloadedBytes % (1024 * 1024) < 8192) {
                            dao.getByItemId(itemId)?.let {
                                dao.update(
                                    it.copy(
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes
                                    )
                                )
                            }
                        }
                    }
                }
            }

            dao.getByItemId(itemId)?.let {
                dao.update(
                    it.copy(
                        downloadState = DownloadState.COMPLETE,
                        downloadedBytes = downloadedBytes,
                        totalBytes = downloadedBytes
                    )
                )
            }

            Result.success()
        } catch (e: Exception) {
            dao.getByItemId(itemId)?.let {
                dao.update(it.copy(downloadState = DownloadState.FAILED))
            }
            Result.failure()
        }
    }
}
