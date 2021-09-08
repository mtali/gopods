package com.colisa.podplay

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.colisa.podplay.ui.NowPlayingViewModel.NowPlayingEpisode
import com.squareup.moshi.Moshi

class GoPreferences(context: Context) {
    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val moshi = Moshi.Builder().build()

    private val prefsTheme = context.getString(R.string.pref_theme)
    private val prefsThemeDef = context.getString(R.string.pref_theme_auto)
    private val prefsAccent = context.getString(R.string.pref_accent)

    private val prefsRecentEpisode = context.getString(R.string.pref_recent_episode)

    private val prefsFastSeek = context.getString(R.string.pref_fast_seeking)

    var theme
        get() = mPrefs.getString(prefsTheme, prefsThemeDef)
        set(value) = mPrefs.edit { putString(prefsTheme, value) }

    var accent
        get() = mPrefs.getInt(prefsAccent, R.color.deep_purple)
        set(value) = mPrefs.edit { putInt(prefsAccent, value) }

    var latestEpisode: NowPlayingEpisode?
        get() = getObjectForClass(prefsRecentEpisode, NowPlayingEpisode::class.java)
        set(value) = putObjectForClass(prefsRecentEpisode, value, NowPlayingEpisode::class.java)

    var fastSeekingStep: Int
        get() = mPrefs.getInt(prefsFastSeek, 5)
        set(value) = mPrefs.edit { putInt(prefsFastSeek, value) }


    // Saves object into the Preferences using Moshi
    private fun <T : Any> putObjectForClass(key: String, value: T?, clazz: Class<T>) {
        val json = moshi.adapter(clazz).toJson(value)
        mPrefs.edit { putString(key, json) }
    }

    private fun <T : Any> getObjectForClass(key: String, clazz: Class<T>): T? {
        val json = mPrefs.getString(key, null)
        return if (json == null) {
            null
        } else {
            try {
                moshi.adapter(clazz).fromJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

}