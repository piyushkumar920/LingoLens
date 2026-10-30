package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.config.AppConfig
import com.example.data.local.dao.FavoritesDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.HistoryEntity

@Database(
    entities = [
        HistoryEntity::class,
        FavoriteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoritesDao(): FavoritesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add explanation column
                db.execSQL("ALTER TABLE translation_history ADD COLUMN explanation TEXT DEFAULT NULL")

                // Add translationType column
                db.execSQL("ALTER TABLE translation_history ADD COLUMN translationType TEXT NOT NULL DEFAULT 'TEXT'")

                // Copy values from previous 'mode' column if available
                try {
                    db.execSQL("UPDATE translation_history SET translationType = mode WHERE mode IS NOT NULL AND mode != ''")
                } catch (ignored: Exception) {
                }

                // Add indexes for optimal query, search, and filter performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_translation_history_timestamp ON translation_history(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_translation_history_translationType ON translation_history(translationType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_translation_history_isFavorite ON translation_history(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_translation_history_sourceLanguage_targetLanguage ON translation_history(sourceLanguage, targetLanguage)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    AppConfig.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

