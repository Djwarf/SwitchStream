package com.example.switchstream

import android.app.Application
import android.util.Log
import com.example.switchstream.di.AppContainer

class SwitchStreamApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e("SwitchStream", "Uncaught exception", throwable)
        }

        container = AppContainer(this)
    }
}
