package com.dude555afk.xeno.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dude555afk.xeno.XenoApplication

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

        const val PRIVATE_DATABASE_NAME = "xeno-private.db"

        fun get(context: Context): XenoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    XenoDatabase::class.java,
                    databaseName(context)
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }

        /**
         * Only the :private process may call this. Closing and deleting its
         * dedicated DB ensures tabs/history from a previous private session
         * cannot be restored after a clean exit or the next private launch.
         */
        fun clearPrivateDatabase(context: Context) {
            val app = context.applicationContext
            if ((app as? XenoApplication)?.isPrivateProcess != true) return
            synchronized(this) {
                instance?.close()
                instance = null
                app.deleteDatabase(PRIVATE_DATABASE_NAME)
            }
        }

        private fun databaseName(context: Context): String =
            if ((context.applicationContext as? XenoApplication)?.isPrivateProcess == true) {
                PRIVATE_DATABASE_NAME
            } else {
                "xeno.db"
            }
    }
}
