package com.dude555afk.xeno.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SpaceEntity::class,
        TabEntity::class,
        HistoryEntity::class,
        SummaryCacheEntity::class,
        AnswerCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class XenoDatabase : RoomDatabase() {

    abstract fun spaceDao(): SpaceDao
    abstract fun tabDao(): TabDao
    abstract fun historyDao(): HistoryDao
    abstract fun summaryCacheDao(): SummaryCacheDao
    abstract fun answerCacheDao(): AnswerCacheDao

    companion object {
        @Volatile
        private var instance: XenoDatabase? = null

        fun get(context: Context): XenoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XenoDatabase::class.java,
                    "xeno.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
