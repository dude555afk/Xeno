package com.dude555afk.xeno.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SpaceEntity::class,
        TabEntity::class,
        HistoryEntity::class,
        SummaryCacheEntity::class,
        AnswerCacheEntity::class,
        ArchivedTabEntity::class,
        PinnedTabEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class XenoDatabase : RoomDatabase() {

    abstract fun spaceDao(): SpaceDao
    abstract fun tabDao(): TabDao
    abstract fun historyDao(): HistoryDao
    abstract fun summaryCacheDao(): SummaryCacheDao
    abstract fun answerCacheDao(): AnswerCacheDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun pinnedTabDao(): PinnedTabDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tabs ADD COLUMN type TEXT NOT NULL DEFAULT 'web'")
                database.execSQL("ALTER TABLE tabs ADD COLUMN query TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE tabs ADD COLUMN desktopSite INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tabs ADD COLUMN zoomLevel INTEGER NOT NULL DEFAULT 100")
                database.execSQL("ALTER TABLE tabs ADD COLUMN blockedAdsCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS archived_tabs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        originalTabId INTEGER NOT NULL,
                        spaceId INTEGER NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        faviconUrl TEXT,
                        type TEXT NOT NULL,
                        query TEXT NOT NULL,
                        desktopSite INTEGER NOT NULL,
                        zoomLevel INTEGER NOT NULL,
                        blockedAdsCount INTEGER NOT NULL,
                        archivedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pinned_tabs (
                        url TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        faviconUrl TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var instance: XenoDatabase? = null

        fun get(context: Context): XenoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XenoDatabase::class.java,
                    "xeno.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
