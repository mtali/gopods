package com.colisa.podplay.util

import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


object DateUtils {
    fun jsonDateToShortDate(jsonDate: String?): String {
        if (jsonDate == null)
            return "-"
        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inFormat.parse(jsonDate)
        val outFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
        return outFormat.format(date!!)
    }

    fun parseItunesDate(date: String?): Date? {
        return date?.let {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                format.parse(date)
            } catch (e: Throwable) {
                Timber.e(e, "Failed to parse itunes date: $date")
                null
            }
        }
    }

    fun xmlDateToDate(date: String?): Date {
        val d = date ?: return Date()
        val inFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault())
        return inFormat.parse(d)!!
    }

    fun dateToShortDate(date: Date?): String {
        if (date == null) {
            return "-"
        }
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
        return outputFormat.format(date)
    }

}