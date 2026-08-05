package com.colisa.podplay.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.colisa.podplay.core.common.DATABASE_NAME
import com.colisa.podplay.core.database.daos.PodcastDao
import com.colisa.podplay.core.database.models.EpisodeEntity
import com.colisa.podplay.core.database.models.PodcastEntity
import com.colisa.podplay.core.database.models.PodcastSearchResultEntity
import com.colisa.podplay.core.database.utils.Converters

@Database(
  entities = [PodcastEntity::class, EpisodeEntity::class, PodcastSearchResultEntity::class],
  version = 1,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GoDatabase : RoomDatabase() {
  abstract fun podcastDao(): PodcastDao

  companion object {
    @Volatile
    private var instance: GoDatabase? = null

    fun getInstance(context: Context): GoDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          GoDatabase::class.java,
          DATABASE_NAME,
        ).build().also { instance = it }
      }
    }
  }
}
