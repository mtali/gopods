package com.colisa.podplay

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class GoPreferences(context: Context) {
    private val mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val prefsTheme = context.getString(R.string.theme_pref)
    private val prefsThemeDef = context.getString(R.string.theme_pref_auto)
    private val prefsAccent = context.getString(R.string.accent_pref)
    var theme
        get() = mPrefs.getString(prefsTheme, prefsThemeDef)
        set(value) = mPrefs.edit { putString(prefsTheme, value) }

    var accent
        get() = mPrefs.getInt(prefsAccent, R.color.deep_purple)
        set(value) = mPrefs.edit { putInt(prefsAccent, value) }

}