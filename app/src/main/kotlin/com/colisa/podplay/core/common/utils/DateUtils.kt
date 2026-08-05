package com.colisa.podplay.core.common.utils

import timber.log.Timber
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

  private const val ITUNES_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
  private const val RSS_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z"

  fun parseItunesDate(date: String?): Date? {
    if (date == null) return null
    return try {
      SimpleDateFormat(ITUNES_PATTERN, Locale.US).parse(date)
    } catch (e: ParseException) {
      Timber.e(e, "Failed to parse itunes date: $date")
      null
    }
  }

  /**
   * RSS pubDate. Falls back to the current time so a malformed date on one item
   * does not drop the whole feed.
   */
  fun xmlDateToDate(date: String?): Date {
    val value = date ?: return Date()
    return try {
      SimpleDateFormat(RSS_PATTERN, Locale.US).parse(value) ?: Date()
    } catch (e: ParseException) {
      Timber.e(e, "Failed to parse rss date: $value")
      Date()
    }
  }

  fun dateToShortDate(date: Date?): String {
    if (date == null) return "-"
    return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date)
  }
}
