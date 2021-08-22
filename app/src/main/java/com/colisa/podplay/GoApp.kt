package com.colisa.podplay

import android.app.Application
import com.colisa.podplay.logging.ReleaseTree
import timber.log.Timber


val goPreferences: GoPreferences by lazy {
    GoApp.prefs
}

class GoApp : Application() {

    companion object {
        lateinit var prefs: GoPreferences
    }

    override fun onCreate() {
        super.onCreate()
        setTimber()
        prefs = GoPreferences(applicationContext)
    }

    private fun setTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}