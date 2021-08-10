package com.colisa.podplay

import android.app.Application
import com.colisa.podplay.logging.ReleaseTree
import timber.log.Timber

class PodPlayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}