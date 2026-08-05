package com.colisa.podplay.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.colisa.podplay.core.common.DATABASE_NAME
import com.colisa.podplay.core.database.daos.PodcastDao
import com.colisa.podplay.core.database.models.EpisodeEntity
import com.colisa.podplay.core.database.models.PodcastEntity
import com.colisa.podplay.core.database.models.PodcastSearchResultEntity
import com.colisa.podplay.core.database.utils.Converters

@Database(
  entities = [PodcastEntity::class, EpisodeEntity::class, PodcastSearchResultEntity::class],
  version = 2,
  exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GoDatabase : RoomDatabase() {
  abstract fun podcastDao(): PodcastDao

  companion object {

    /** Adds per episode artwork. Existing rows fall back to the podcast image. */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Episode ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
      }
    }

    @Volatile
    private var instance: GoDatabase? = null

    fun getInstance(context: Context): GoDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          GoDatabase::class.java,
          DATABASE_NAME,
        ).addMigrations(MIGRATION_1_2)
          .build()
          .also { instance = it }
      }
    }
  }
}
