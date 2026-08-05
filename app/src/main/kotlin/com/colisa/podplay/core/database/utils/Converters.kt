package com.colisa.podplay.core.database.utils

import androidx.room.TypeConverter
import timber.log.Timber
import java.util.Date

object Converters {
  @TypeConverter
  @JvmStatic
  fun fromTimestamp(value: Long?): Date? = if (value == null) null else Date(value)

  @TypeConverter
  @JvmStatic
  fun toTimeStamp(date: Date?): Long? = date?.time

  @TypeConverter
  @JvmStatic
  fun stringToLongList(data: String?): List<Long>? {
    if (data.isNullOrEmpty()) return data?.let { emptyList() }
    return data.split(",").mapNotNull { value ->
      value.toLongOrNull().also {
        if (it == null) Timber.e("Cannot convert $value to number")
      }
    }
  }

  @TypeConverter
  @JvmStatic
  fun longListToString(ids: List<Long>?): String? = ids?.joinToString(",")
}
