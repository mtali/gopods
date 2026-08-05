package com.colisa.podplay.util

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.colisa.podplay.R

object DialogUtils {
    @JvmStatic
    fun showAboutDialog(context: Context) {
        MaterialDialog(context).show {
            title(R.string.about)
            message(R.string.about_message)
            positiveButton(R.string.action_cool)
        }
    }
}