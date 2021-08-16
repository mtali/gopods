package com.colisa.podplay.util

import android.os.Build

object Utils {
    fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}