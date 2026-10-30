package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("""
        SELECT * FROM translation_history 
        WHERE (:query IS NULL OR :query = '' OR sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%')
          AND (:type IS NULL OR :type = '' OR translationType = :type)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY timestamp DESC
    """)
    fun getFilteredHistory(query: String?, type: String?, favoritesOnly: Boolean): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE translationType = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE translationType = :mode ORDER BY timestamp DESC")
    fun getHistoryByMode(mode: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Query("UPDATE translation_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE translation_history SET explanation = :explanation WHERE id = :id")
    suspend fun updateExplanation(id: Long, explanation: String)

    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()
}

