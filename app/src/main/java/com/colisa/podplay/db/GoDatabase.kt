package com.colisa.podplay.db

import android.content.Context
import androidx.room.*
import com.colisa.podplay.models.Episode
import com.colisa.podplay.models.Podcast
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimeStamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(entities = [Podcast::class, Episode::class], version = 1)
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