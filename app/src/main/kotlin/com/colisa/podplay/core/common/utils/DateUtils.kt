package com.colisa.podplay.core.common.utils

import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import android.text.format.DateUtils as AndroidDateUtils

object DateUtils {

  private const val ITUNES_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
  private const val RSS_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z"
  private const val WEEK_MS = 7L * AndroidDateUtils.DAY_IN_MILLIS

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

  /**
   * Dates people can read at a glance: "Yesterday" or "3 days ago" within a week,
   * "24 Jul" inside the current year, "Jul 2024" beyond it. A numeric short date
   * like 7/24/26 says very little in a feed list.
   */
  fun formatRelativeDate(date: Date?): String {
    if (date == null) return ""
    val now = System.currentTimeMillis()
    if (abs(now - date.time) <= WEEK_MS) {
      return AndroidDateUtils
        .getRelativeTimeSpanString(date.time, now, AndroidDateUtils.DAY_IN_MILLIS)
        .toString()
    }
    val pattern = if (isSameYear(date, now)) "d MMM" else "MMM yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
  }

  private fun isSameYear(date: Date, now: Long): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = date
    val year = calendar.get(Calendar.YEAR)
    calendar.timeInMillis = now
    return year == calendar.get(Calendar.YEAR)
  }

  /**
   * Feed durations arrive as "00:29:55", "1:35:23" or a plain number of seconds.
   * Normalises to "29:55" or "1:35:23"; a leading "00:" is noise.
   */
  fun formatDuration(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return ""

    value.toLongOrNull()?.let { seconds ->
      return AndroidDateUtils.formatElapsedTime(seconds)
    }

    val parts = value.split(":").mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
      3 -> {
        val (hours, minutes, seconds) = parts
        if (hours > 0) {
          "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
          "%d:%02d".format(minutes, seconds)
        }
      }

      2 -> "%d:%02d".format(parts[0], parts[1])
      else -> value
    }
  }
}
