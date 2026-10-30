package com.example.modules.favorites.repository

import com.example.core.result.AppResult
import com.example.modules.favorites.model.FavoritePhrase
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoritePhrase>>
    fun getFavoritesByCategory(category: String): Flow<List<FavoritePhrase>>
    suspend fun saveFavorite(phrase: FavoritePhrase): AppResult<Long>
    suspend fun removeFavorite(id: Long): AppResult<Unit>
    suspend fun removeFavoriteByText(sourceText: String, targetLanguage: String): AppResult<Unit>
    suspend fun isFavorite(sourceText: String, targetLanguage: String): Boolean
}
