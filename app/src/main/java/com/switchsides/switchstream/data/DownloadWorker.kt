package com.switchsides.switchstream.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.switchsides.switchstream.data.db.AppDatabase
import com.switchsides.switchstream.data.db.DownloadState
import com.switchsides.switchstream.data.db.DownloadedMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "switchstream_downloads"
        private const val NOTIFICATION_ID_BASE = 10000
        private const val BUFFER_SIZE = 64 * 1024
        private const val CHUNK_COUNT = 4
        private const val PROGRESS_UPDATE_MS = 500L
        private const val MIN_RANGE_SIZE = 16L * 1024 * 1024 // only parallelize files >16MB
        private const val MAX_ATTEMPTS = 3
    }

    private val dao by lazy { AppDatabase.getInstance(applicationContext).downloadDao() }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString("title") ?: "Download"
        val notificationId = NOTIFICATION_ID_BASE + (inputData.getString("itemId")?.hashCode() ?: 0)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Starting\u2026")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getString("itemId") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: "Download"
        val streamUrl = inputData.getString("streamUrl") ?: return@withContext Result.failure()
        val filePath = inputData.getString("filePath") ?: return@withContext Result.failure()
        val accessToken = inputData.getString("accessToken") ?: ""

        val notificationId = NOTIFICATION_ID_BASE + itemId.hashCode()

        try {
            setForeground(getForegroundInfo())
        } catch (_: Exception) {
            // Foreground may be denied (rare); continue as regular work.
        }

        try {
            updateState(itemId) { it.copy(
                downloadState = DownloadState.DOWNLOADING,
                updatedAtMs = System.currentTimeMillis()
            ) }

            val (totalBytes, rangeSupported) = probe(streamUrl, accessToken)
            val file = File(filePath)
            file.parentFile?.mkdirs()

            val useChunked = rangeSupported && totalBytes >= MIN_RANGE_SIZE

            // Chunked: the final merged .mp4 only exists after the merge step, so discard any remnant
            // from a half-merged prior attempt. .partN files below are the real resume source.
            // Non-range single: can't resume, so also discard.
            if (useChunked || !rangeSupported) {
                if (file.exists()) file.delete()
            }

            val downloaded = AtomicLong(
                if (useChunked) {
                    (0 until CHUNK_COUNT).sumOf {
                        File("$filePath.part$it").takeIf { f -> f.exists() }?.length() ?: 0L
                    }
                } else if (rangeSupported && file.exists()) file.length()
                else 0L
            )

            coroutineScope {
                val progressJob = launch {
                    var lastBytes = 0L
                    var lastTs = System.currentTimeMillis()
                    while (isActive) {
                        delay(PROGRESS_UPDATE_MS)
                        val now = System.currentTimeMillis()
                        val d = downloaded.get()
                        val dt = (now - lastTs).coerceAtLeast(1L)
                        val bps = ((d - lastBytes) * 1000L) / dt
                        lastBytes = d
                        lastTs = now
                        val progress = if (totalBytes > 0) (d * 100 / totalBytes).toInt().coerceIn(0, 100) else 0
                        updateState(itemId) { it.copy(
                            downloadedBytes = d,
                            totalBytes = if (totalBytes > 0) totalBytes else it.totalBytes,
                            bytesPerSec = bps.coerceAtLeast(0),
                            updatedAtMs = now,
                            downloadState = DownloadState.DOWNLOADING
                        ) }
                        showProgressNotification(title, progress, d, totalBytes, bps, notificationId)
                    }
                }

                val success = if (useChunked) {
                    downloadChunked(streamUrl, accessToken, file, totalBytes, downloaded)
                } else {
                    downloadSingle(streamUrl, accessToken, file, downloaded, rangeSupported)
                }

                progressJob.cancelAndJoin()

                if (!success) {
                    // Keep partial .partN / file so next retry can resume.
                    // Final cleanup happens below when we decide to fail permanently.
                    updateState(itemId) { it.copy(
                        downloadState = DownloadState.FAILED,
                        bytesPerSec = 0,
                        updatedAtMs = System.currentTimeMillis()
                    ) }
                    showDoneNotification(title, "Download paused \u2014 will resume", android.R.drawable.stat_notify_error, notificationId)
                    return@coroutineScope
                }

                val finalBytes = file.length()

                // Internal-first + copy-on-finish: if user picked a SAF folder, copy the merged
                // file there and swap the DB path to the content:// URI so playback/deletion
                // routes through the user-visible location.
                val targetTreeUri = inputData.getString("targetTreeUri").orEmpty()
                var finalFilePath = filePath
                if (targetTreeUri.isNotEmpty()) {
                    val copied = copyToSaf(itemId, file, targetTreeUri)
                    if (copied != null) {
                        finalFilePath = copied
                        file.delete()
                    }
                }

                updateState(itemId) { it.copy(
                    downloadState = DownloadState.COMPLETE,
                    downloadedBytes = finalBytes,
                    totalBytes = finalBytes,
                    filePath = finalFilePath,
                    bytesPerSec = 0,
                    updatedAtMs = System.currentTimeMillis()
                ) }
                showDoneNotification(title, "Download complete", android.R.drawable.stat_sys_download_done, notificationId)
            }

            val finalState = dao.getByItemId(itemId)?.downloadState
            when {
                finalState == DownloadState.COMPLETE -> Result.success()
                runAttemptCount < MAX_ATTEMPTS -> Result.retry()
                else -> {
                    // Out of attempts: clean up partial artifacts.
                    if (File(filePath).exists()) File(filePath).delete()
                    cleanupParts(filePath)
                    Result.failure()
                }
            }
        } catch (e: CancellationException) {
            File(filePath).delete()
            cleanupParts(filePath)
            updateState(itemId) { it.copy(
                downloadState = DownloadState.FAILED,
                bytesPerSec = 0,
                updatedAtMs = System.currentTimeMillis()
            ) }
            throw e
        } catch (e: IOException) {
            updateState(itemId) { it.copy(
                downloadState = DownloadState.FAILED,
                bytesPerSec = 0,
                updatedAtMs = System.currentTimeMillis()
            ) }
            if (runAttemptCount < MAX_ATTEMPTS) {
                showDoneNotification(title, "Download paused \u2014 will resume", android.R.drawable.stat_notify_error, notificationId)
                Result.retry()
            } else {
                if (File(filePath).exists()) File(filePath).delete()
                cleanupParts(filePath)
                showDoneNotification(title, "Download failed", android.R.drawable.stat_notify_error, notificationId)
                Result.failure()
            }
        } catch (e: Exception) {
            if (File(filePath).exists()) File(filePath).delete()
            cleanupParts(filePath)
            updateState(itemId) { it.copy(
                downloadState = DownloadState.FAILED,
                bytesPerSec = 0,
                updatedAtMs = System.currentTimeMillis()
            ) }
            showDoneNotification(title, "Download failed", android.R.drawable.stat_notify_error, notificationId)
            Result.failure()
        }
    }

    private fun probe(url: String, token: String): Pair<Long, Boolean> {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 15000
                readTimeout = 15000
                if (token.isNotEmpty()) setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
            }
            conn.connect()
            val len = conn.contentLengthLong
            val ranges = conn.getHeaderField("Accept-Ranges")
            conn.disconnect()
            len to (ranges != null && ranges.contains("bytes", true))
        } catch (_: Exception) {
            -1L to false
        }
    }

    private suspend fun downloadChunked(
        url: String, token: String, file: File, totalBytes: Long, downloaded: AtomicLong
    ): Boolean = coroutineScope {
        val chunkSize = totalBytes / CHUNK_COUNT
        val results = (0 until CHUNK_COUNT).map { i ->
            val start = i * chunkSize
            val end = if (i == CHUNK_COUNT - 1) totalBytes - 1 else (i + 1) * chunkSize - 1
            val partFile = File("${file.absolutePath}.part$i")
            async(Dispatchers.IO) {
                downloadRange(url, token, partFile, start, end, downloaded)
            }
        }.awaitAll()
        if (results.any { !it }) return@coroutineScope false

        FileOutputStream(file).use { out ->
            for (i in 0 until CHUNK_COUNT) {
                val partFile = File("${file.absolutePath}.part$i")
                partFile.inputStream().use { it.copyTo(out, BUFFER_SIZE) }
                partFile.delete()
            }
        }
        true
    }

    private suspend fun downloadRange(
        urlStr: String, token: String, part: File, start: Long, end: Long, downloaded: AtomicLong
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = if (part.exists()) part.length() else 0L
            val chunkLen = end - start + 1
            if (existing >= chunkLen) return@withContext true  // chunk already fully downloaded

            val resumedStart = start + existing
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                if (token.isNotEmpty()) setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
                setRequestProperty("Range", "bytes=$resumedStart-$end")
                connectTimeout = 30000
                readTimeout = 60000
            }
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..206) {
                conn.disconnect()
                return@withContext false
            }
            // If server ignored Range (returned 200), we got the whole body — restart this chunk.
            val appendMode = (code == 206 && existing > 0)
            if (!appendMode && existing > 0) {
                downloaded.addAndGet(-existing)
                part.delete()
            }
            conn.inputStream.use { input ->
                FileOutputStream(part, appendMode).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (!isActive || isStopped) return@withContext false
                        val n = input.read(buffer)
                        if (n == -1) break
                        output.write(buffer, 0, n)
                        downloaded.addAndGet(n.toLong())
                    }
                }
            }
            conn.disconnect()
            true
        } catch (_: IOException) {
            false
        }
    }

    private suspend fun downloadSingle(
        urlStr: String, token: String, file: File, downloaded: AtomicLong, canResume: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val existing = if (canResume && file.exists()) file.length() else 0L
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                if (token.isNotEmpty()) setRequestProperty("Authorization", "MediaBrowser Token=\"$token\"")
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
                connectTimeout = 30000
                readTimeout = 60000
            }
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..206) {
                conn.disconnect()
                return@withContext false
            }
            val appendMode = (code == 206 && existing > 0)
            if (!appendMode && existing > 0) {
                downloaded.addAndGet(-existing)
                file.delete()
            }
            conn.inputStream.use { input ->
                FileOutputStream(file, appendMode).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (!isActive || isStopped) return@withContext false
                        val n = input.read(buffer)
                        if (n == -1) break
                        output.write(buffer, 0, n)
                        downloaded.addAndGet(n.toLong())
                    }
                }
            }
            conn.disconnect()
            true
        } catch (_: IOException) {
            false
        }
    }

    private suspend fun updateState(itemId: String, transform: (DownloadedMedia) -> DownloadedMedia) {
        val existing = dao.getByItemId(itemId) ?: return
        dao.update(transform(existing))
    }

    private fun cleanupParts(filePath: String) {
        for (i in 0 until CHUNK_COUNT) {
            File("${filePath}.part$i").takeIf { it.exists() }?.delete()
        }
    }

    private fun copyToSaf(itemId: String, sourceFile: File, treeUriStr: String): String? {
        return try {
            val tree = DocumentFile.fromTreeUri(applicationContext, Uri.parse(treeUriStr))
            if (tree == null || !tree.canWrite()) return null
            val fileName = "$itemId.mp4"
            tree.findFile(fileName)?.delete()
            val dest = tree.createFile("video/mp4", fileName) ?: return null
            val resolver = applicationContext.contentResolver
            val out = resolver.openOutputStream(dest.uri)
            if (out == null) {
                dest.delete()
                return null
            }
            out.use { sink ->
                sourceFile.inputStream().use { it.copyTo(sink, BUFFER_SIZE) }
            }
            dest.uri.toString()
        } catch (_: Exception) {
            null
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

    private fun showProgressNotification(title: String, progress: Int, done: Long, total: Long, bps: Long, id: Int) {
        val speed = formatSpeed(bps)
        val sizeText = if (total > 0) "${formatBytes(done)} / ${formatBytes(total)}" else formatBytes(done)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$progress%  \u00b7  $sizeText  \u00b7  $speed")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, total <= 0)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
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
            .setSilent(true)
            .build()
        try { NotificationManagerCompat.from(applicationContext).notify(id, notification) } catch (_: SecurityException) { }
    }

    private fun formatBytes(b: Long): String {
        if (b < 1024) return "$b B"
        if (b < 1024 * 1024) return "${b / 1024} KB"
        if (b < 1024L * 1024 * 1024) return "%.1f MB".format(b / 1_048_576.0)
        return "%.2f GB".format(b / 1_073_741_824.0)
    }

    private fun formatSpeed(bps: Long): String {
        if (bps <= 0) return "0 KB/s"
        if (bps < 1024 * 1024) return "${bps / 1024} KB/s"
        return "%.1f MB/s".format(bps / 1_048_576.0)
    }
}
