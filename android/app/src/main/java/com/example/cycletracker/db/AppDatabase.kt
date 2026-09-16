package com.example.cycletracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrackEntity::class, TrackPointEntity::class, OfflineRegionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun offlineRegionDao(): OfflineRegionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Prida jen novou tabulku pro offline mapy - existujici trasy (TrackEntity,
        // TrackPointEntity) se timhle vubec nedotknou, takze uzivatel o nic neprijde.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS offline_regions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        minLat REAL NOT NULL,
                        maxLat REAL NOT NULL,
                        minLon REAL NOT NULL,
                        maxLon REAL NOT NULL,
                        zoomMin INTEGER NOT NULL,
                        zoomMax INTEGER NOT NULL,
                        downloadedAt INTEGER NOT NULL,
                        tileCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cycletracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
