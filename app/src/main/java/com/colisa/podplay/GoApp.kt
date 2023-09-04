package com.colisa.podplay

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.colisa.podplay.logging.ReleaseTree
import com.colisa.podplay.util.ThemeUtils
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import okhttp3.OkHttpClient
import timber.log.Timber
import java.nio.charset.Charset


val goPreferences: GoPreferences by lazy { GoApp.prefs }
val goRssParser: RssParser by lazy { GoApp.rssParser }

class GoApp : Application() {

    companion object {
        lateinit var prefs: GoPreferences
        lateinit var rssParser: RssParser
    }

    override fun onCreate() {
        super.onCreate()
        setTimber()
        prefs = GoPreferences(applicationContext)
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.getDefaultNightMode(applicationContext))
        rssParser = RssParserBuilder(
            charset = Charset.forName("ISO-8859-7"),
            callFactory = OkHttpClient()
        ).build()
    }

    private fun setTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}