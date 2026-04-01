package com.example.switchstream.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.switchstream.data.db.AppDatabase
import com.example.switchstream.data.db.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "switchstream_downloads"
        private const val NOTIFICATION_ID_BASE = 10000
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getString("itemId") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Download"
        val streamUrl = inputData.getString("streamUrl") ?: return@withContext Result.failure()
        val filePath = inputData.getString("filePath") ?: return@withContext Result.failure()
        val accessToken = inputData.getString("accessToken") ?: ""
        val silent = inputData.getBoolean("silent", false)

        val dao = AppDatabase.getInstance(applicationContext).downloadDao()
        val notificationId = NOTIFICATION_ID_BASE + itemId.hashCode()

        if (!silent) {
            createNotificationChannel()
            showProgressNotification(title, 0, true, notificationId)
        }

        try {
            // Mark as downloading
            dao.getByItemId(itemId)?.let {
                dao.update(it.copy(downloadState = DownloadState.DOWNLOADING))
            }

            val url = URL(streamUrl)
            val connection = url.openConnection() as HttpURLConnection
            if (accessToken.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "MediaBrowser Token=\"$accessToken\"")
            }
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
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
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (downloadedBytes % (1024 * 1024) < 8192) {
                            val progress = if (totalBytes > 0) {
                                (downloadedBytes * 100 / totalBytes).toInt()
                            } else 0

                            // Update progress atomically
                            val currentTotal = if (totalBytes > 0) totalBytes else downloadedBytes
                            dao.getByItemId(itemId)?.let {
                                dao.update(it.copy(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = currentTotal,
                                    downloadState = DownloadState.DOWNLOADING
                                ))
                            }
                            if (!silent) showProgressNotification(title, progress, false, notificationId)
                        }
                    }
                }
            }

            // Mark complete
            dao.getByItemId(itemId)?.let {
                dao.update(it.copy(
                    downloadState = DownloadState.COMPLETE,
                    downloadedBytes = downloadedBytes,
                    totalBytes = downloadedBytes
                ))
            }

            if (!silent) showDoneNotification(title, "Download complete", android.R.drawable.stat_sys_download_done, notificationId)
            Result.success()
        } catch (e: Exception) {
            dao.getByItemId(itemId)?.let {
                dao.update(it.copy(downloadState = DownloadState.FAILED))
            }
            if (!silent) showDoneNotification(title, "Download failed", android.R.drawable.stat_notify_error, notificationId)
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media download progress"
                setShowBadge(false)
            }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification(title: String, progress: Int, indeterminate: Boolean, id: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (indeterminate) "Starting download..." else "$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setSilent(true)
            .build()
        try { NotificationManagerCompat.from(applicationContext).notify(id, notification) } catch (_: SecurityException) { }
    }

    private fun showDoneNotification(title: String, text: String, icon: Int, id: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(applicationContext).notify(id, notification) } catch (_: SecurityException) { }
    }
}
