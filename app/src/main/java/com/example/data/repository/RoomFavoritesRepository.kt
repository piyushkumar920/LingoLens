package com.example.data.repository

import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.data.local.dao.FavoritesDao
import com.example.data.local.entity.FavoriteEntity
import com.example.modules.favorites.model.FavoritePhrase
import com.example.modules.favorites.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFavoritesRepository(
    private val favoritesDao: FavoritesDao
) : FavoritesRepository {

    override fun getAllFavorites(): Flow<List<FavoritePhrase>> {
        return favoritesDao.getAllFavorites().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFavoritesByCategory(category: String): Flow<List<FavoritePhrase>> {
        return favoritesDao.getFavoritesByCategory(category).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveFavorite(phrase: FavoritePhrase): AppResult<Long> {
        return try {
            val id = favoritesDao.insert(FavoriteEntity.fromDomain(phrase))
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to save favorite phrase: ${e.message}", e))
        }
    }

    override suspend fun removeFavorite(id: Long): AppResult<Unit> {
        return try {
            favoritesDao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to remove favorite: ${e.message}", e))
        }
    }

    override suspend fun removeFavoriteByText(sourceText: String, targetLanguage: String): AppResult<Unit> {
        return try {
            favoritesDao.deleteBySourceAndTarget(sourceText, targetLanguage)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to remove favorite by text: ${e.message}", e))
        }
    }

    override suspend fun isFavorite(sourceText: String, targetLanguage: String): Boolean {
        return try {
            favoritesDao.countMatching(sourceText, targetLanguage) > 0
        } catch (e: Exception) {
            false
        }
    }
}
