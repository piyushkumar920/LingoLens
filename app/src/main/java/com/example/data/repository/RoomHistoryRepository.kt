package com.example.data.repository

import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.data.local.dao.HistoryDao
import com.example.data.local.entity.HistoryEntity
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationType
import com.example.modules.history.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHistoryRepository(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<TranslationHistoryItem>> {
        return historyDao.getAllHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFilteredHistory(
        query: String,
        type: TranslationType?,
        favoritesOnly: Boolean
    ): Flow<List<TranslationHistoryItem>> {
        val trimmedQuery = query.trim()
        val typeString = type?.name
        return historyDao.getFilteredHistory(
            query = if (trimmedQuery.isEmpty()) null else trimmedQuery,
            type = typeString,
            favoritesOnly = favoritesOnly
        ).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getHistoryByType(type: TranslationType): Flow<List<TranslationHistoryItem>> {
        return historyDao.getHistoryByType(type.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getHistoryByMode(mode: TranslationType): Flow<List<TranslationHistoryItem>> {
        return historyDao.getHistoryByMode(mode.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun searchHistory(query: String): Flow<List<TranslationHistoryItem>> {
        return historyDao.searchHistory(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getHistoryItemById(id: Long): AppResult<TranslationHistoryItem?> {
        return try {
            val entity = historyDao.getById(id)
            AppResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to fetch history item by id: ${e.message}", e))
        }
    }

    override suspend fun insertHistoryItem(item: TranslationHistoryItem): AppResult<Long> {
        return try {
            val id = historyDao.insert(HistoryEntity.fromDomain(item))
            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to save translation history: ${e.message}", e))
        }
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean): AppResult<Unit> {
        return try {
            historyDao.updateFavorite(id, isFavorite)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to update favorite status: ${e.message}", e))
        }
    }

    override suspend fun updateExplanation(id: Long, explanation: String): AppResult<Unit> {
        return try {
            historyDao.updateExplanation(id, explanation)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to update explanation: ${e.message}", e))
        }
    }

    override suspend fun deleteHistoryItem(id: Long): AppResult<Unit> {
        return try {
            historyDao.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to delete history item: ${e.message}", e))
        }
    }

    override suspend fun clearAllHistory(): AppResult<Unit> {
        return try {
            historyDao.clearAll()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(DomainError.DatabaseError("Failed to clear history: ${e.message}", e))
        }
    }
}

