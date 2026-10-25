package com.dude555afk.xeno.core.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY position ASC, pinned DESC")
    fun observeAll(): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces ORDER BY position ASC")
    suspend fun all(): List<SpaceEntity>

    @Insert
    suspend fun insert(space: SpaceEntity): Long

    @Update
    suspend fun update(space: SpaceEntity)

    @Delete
    suspend fun delete(space: SpaceEntity)

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun count(): Int
}

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE spaceId = :spaceId ORDER BY position ASC")
    fun observeForSpace(spaceId: Long): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs ORDER BY lastActiveAt DESC")
    suspend fun all(): List<TabEntity>

    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun byId(id: Long): TabEntity?

    @Insert
    suspend fun insert(tab: TabEntity): Long

    @Update
    suspend fun update(tab: TabEntity)

    @Delete
    suspend fun delete(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tabs WHERE spaceId = :spaceId")
    suspend fun deleteForSpace(spaceId: Long)

    /** Called on startup — stale peek tabs are never restored. */
    @Query("DELETE FROM tabs WHERE isPeek = 1")
    suspend fun deletePeekTabs()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<HistoryEntity>>

    /** Substring match for the command palette's fuzzy search. */
    @Query("SELECT * FROM history WHERE title LIKE '%' || :q || '%' OR url LIKE '%' || :q || '%' ORDER BY visitedAt DESC LIMIT :limit")
    suspend fun search(q: String, limit: Int = 20): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun byUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: HistoryEntity): Long

    @Query("UPDATE history SET visitedAt = :at, title = :title WHERE url = :url")
    suspend fun bump(url: String, title: String, at: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface SummaryCacheDao {
    @Query("SELECT * FROM summary_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): SummaryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: SummaryCacheEntity)

    /** Keep the cache bounded — it grows without limit otherwise. */
    @Query("DELETE FROM summary_cache WHERE createdAt < :before")
    suspend fun evictOlderThan(before: Long)
}

@Dao
interface AnswerCacheDao {
    @Query("SELECT * FROM answer_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): AnswerCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: AnswerCacheEntity)

    @Query("DELETE FROM answer_cache WHERE createdAt < :before")
    suspend fun evictOlderThan(before: Long)
}
