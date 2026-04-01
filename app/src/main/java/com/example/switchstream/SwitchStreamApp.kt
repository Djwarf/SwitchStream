package com.example.switchstream

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.switchstream.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SwitchStreamApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e("SwitchStream", "Uncaught exception", throwable)
        }

        container = AppContainer(this)

        // Apply saved offline mode setting
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val settings = container.settingsManager.settings.first()
            if (settings.offlineMode) {
                container.networkMonitor.setOfflineMode(true)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.3)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                    .build()
            }
            .crossfade(100)
            .build()
    }
}
