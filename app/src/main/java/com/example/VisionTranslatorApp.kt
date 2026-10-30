package com.example

import android.app.Application
import android.util.Log
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer

class VisionTranslatorApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("VisionTranslatorApp", "Uncaught exception on thread: ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        container = DefaultAppContainer(this)
    }
}
