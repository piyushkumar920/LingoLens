package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_phrases ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorite_phrases WHERE category = :category ORDER BY timestamp DESC")
    fun getFavoritesByCategory(category: String): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: FavoriteEntity): Long

    @Query("DELETE FROM favorite_phrases WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM favorite_phrases WHERE sourceText = :sourceText AND targetLanguage = :targetLanguage")
    suspend fun deleteBySourceAndTarget(sourceText: String, targetLanguage: String)

    @Query("SELECT COUNT(*) FROM favorite_phrases WHERE sourceText = :sourceText AND targetLanguage = :targetLanguage")
    suspend fun countMatching(sourceText: String, targetLanguage: String): Int
}
