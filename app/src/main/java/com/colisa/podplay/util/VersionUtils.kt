package com.colisa.podplay.util

import android.os.Build

object VersionUtils {
    @JvmStatic
    fun isQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @JvmStatic
    fun isOreoMR1() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

    @JvmStatic
    fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @JvmStatic
    fun isMarshmallow() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}