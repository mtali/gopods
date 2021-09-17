package com.colisa.podplay.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.colisa.podplay.R
import com.colisa.podplay.goPreferences

object ThemeUtils {
    @JvmStatic
    val accents = arrayOf(
        Pair(R.color.red, R.style.BaseTheme_Red),
        Pair(R.color.pink, R.style.BaseTheme_Pink),
        Pair(R.color.purple, R.style.BaseTheme_Purple),
        Pair(R.color.deep_purple, R.style.BaseTheme_DeepPurple),
        Pair(R.color.indigo, R.style.BaseTheme_Indigo),
        Pair(R.color.blue, R.style.BaseTheme_Blue),
        Pair(R.color.light_blue, R.style.BaseTheme_LightBlue),
        Pair(R.color.cyan, R.style.BaseTheme_Cyan),
        Pair(R.color.teal, R.style.BaseTheme_Teal),
        Pair(R.color.green, R.style.BaseTheme_Green),
        Pair(R.color.light_green, R.style.BaseTheme_LightGreen),
        Pair(R.color.lime, R.style.BaseTheme_Lime),
        Pair(R.color.yellow, R.style.BaseTheme_Yellow),
        Pair(R.color.amber, R.style.BaseTheme_Amber),
        Pair(R.color.orange, R.style.BaseTheme_Orange),
        Pair(R.color.deep_orange, R.style.BaseTheme_DeepOrange),
        Pair(R.color.brown, R.style.BaseTheme_Brown),
        Pair(R.color.grey, R.style.BaseTheme_Grey),
        Pair(R.color.blue_grey, R.style.BaseTheme_BlueGrey),
        Pair(R.color.red_300, R.style.BaseTheme_Red300),
        Pair(R.color.pink_300, R.style.BaseTheme_Pink300),
        Pair(R.color.purple_300, R.style.BaseTheme_Purple300),
        Pair(R.color.deep_purple_300, R.style.BaseTheme_DeepPurple300),
        Pair(R.color.indigo_300, R.style.BaseTheme_Indigo300),
        Pair(R.color.blue_300, R.style.BaseTheme_Blue300),
        Pair(R.color.light_blue_300, R.style.BaseTheme_LightBlue300),
        Pair(R.color.cyan_300, R.style.BaseTheme_Cyan300),
        Pair(R.color.teal_300, R.style.BaseTheme_Teal300),
        Pair(R.color.green_300, R.style.BaseTheme_Green300),
        Pair(R.color.light_green_300, R.style.BaseTheme_LightGreen300),
        Pair(R.color.lime_300, R.style.BaseTheme_Lime300),
        Pair(R.color.amber_300, R.style.BaseTheme_Amber300),
        Pair(R.color.orange_300, R.style.BaseTheme_Orange300),
        Pair(R.color.deep_orange_300, R.style.BaseTheme_DeepOrange300),
        Pair(R.color.brown_300, R.style.BaseTheme_Brown300),
        Pair(R.color.blue_grey_300, R.style.BaseTheme_BlueGrey300)
    )


    fun getAlphaAccent(context: Context): Int {
        val accent = goPreferences.accent
        var alpha = if (accent == R.color.yellow) {
            200
        } else {
            150
        }
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (uiMode == Configuration.UI_MODE_NIGHT_YES) {
            alpha = 150
        }
        return ColorUtils.setAlphaComponent(resolveThemeAccent(context), alpha)
    }


    @ColorInt
    @JvmStatic
    fun resolveThemeAccent(context: Context): Int {
        var accent = goPreferences.accent

        // Fallback to default color when the pref is f@#$ed (when resources change)
        if (!accents.map { accentId -> accentId.first }.contains(accent)) {
            accent = R.color.deep_purple
            goPreferences.accent = accent
        }
        return ContextCompat.getColor(context, accent)
    }

    @JvmStatic
    fun getAccentedTheme() = try {
        val pair = accents.find { (first) -> first == goPreferences.accent }
        val theme = pair!!.second
        val position = accents.indexOf(pair)
        Pair(theme, position)
    } catch (e: Exception) {
        Pair(R.style.BaseTheme_DeepPurple, 3)
    }

    @JvmStatic
    fun isDeviceLand(resources: Resources) =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}