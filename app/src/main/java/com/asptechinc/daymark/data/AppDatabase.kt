package com.asptechinc.daymark.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag

@Database(
    entities = [Activity::class, Category::class, Tag::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    abstract fun categoryDao(): CategoryDao

    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    Log.i("AppDatabase", "Migrating database from version 2 to 3")
                    db.execSQL("ALTER TABLE activities ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                }
            }

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                Log.i("AppDatabase", "Initialising database")
                val newInstance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "${AppConfig.APP_NAME.lowercase()}_database",
                        ).addMigrations(MIGRATION_2_3)
                        .fallbackToDestructiveMigration(false)
                        .build()
                instance = newInstance
                newInstance
            }
    }
}
