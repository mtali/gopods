package com.colisa.podplay.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import com.colisa.podplay.models.PodcastSearchResult
import timber.log.Timber
import java.util.Date

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    @JvmStatic
    fun toTimeStamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    @JvmStatic
    fun stringToLongList(data: String?): List<Long>? {
        return data?.let {
            it.split(",").map { str ->
                try {
                    str.toLong()
                } catch (e: NumberFormatException) {
                    Timber.e(e, "Cannot convert $str to number")
                    null
                }
            }
        }?.filterNotNull()
    }

    @TypeConverter
    @JvmStatic
    fun longListToString(ints: List<Long>?): String? {
        return ints?.joinToString(",")
    }

}

@Database(
    entities = [Podcast::class, Episode::class, PodcastSearchResult::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GoDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    companion object {
        private var instance: GoDatabase? = null
        fun getInstance(context: Context): GoDatabase {
            if (null == instance) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoDatabase::class.java,
                    "GoDatabase"
                ).build()
            }
            return instance as GoDatabase
        }
    }
}