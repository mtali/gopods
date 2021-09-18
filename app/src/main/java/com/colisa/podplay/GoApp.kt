package com.colisa.podplay

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.colisa.podplay.logging.ReleaseTree
import com.colisa.podplay.util.ThemeUtils
import com.prof.rssparser.Parser
import timber.log.Timber
import java.nio.charset.Charset


val goPreferences: GoPreferences by lazy { GoApp.prefs }
val goRssParser: Parser by lazy { GoApp.rssParser }

class GoApp : Application() {

    companion object {
        lateinit var prefs: GoPreferences
        lateinit var rssParser: Parser
    }

    override fun onCreate() {
        super.onCreate()
        setTimber()
        prefs = GoPreferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.getDefaultNightMode(applicationContext))
        rssParser = Parser.Builder()
            .context(applicationContext)
            .charset(Charset.forName("ISO-8859-7"))
            .cacheExpirationMillis(24L * 60L * 60L * 100L) // One day
            .build()
    }

    private fun setTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}