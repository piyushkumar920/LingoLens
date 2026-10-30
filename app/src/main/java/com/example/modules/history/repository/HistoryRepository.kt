package com.example.modules.history.repository

import com.example.core.result.AppResult
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationType
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getAllHistory(): Flow<List<TranslationHistoryItem>>
    fun getFilteredHistory(query: String = "", type: TranslationType? = null, favoritesOnly: Boolean = false): Flow<List<TranslationHistoryItem>>
    fun getHistoryByType(type: TranslationType): Flow<List<TranslationHistoryItem>>
    fun getHistoryByMode(mode: TranslationType): Flow<List<TranslationHistoryItem>>
    fun searchHistory(query: String): Flow<List<TranslationHistoryItem>>
    suspend fun getHistoryItemById(id: Long): AppResult<TranslationHistoryItem?>
    suspend fun insertHistoryItem(item: TranslationHistoryItem): AppResult<Long>
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean): AppResult<Unit>
    suspend fun updateExplanation(id: Long, explanation: String): AppResult<Unit>
    suspend fun deleteHistoryItem(id: Long): AppResult<Unit>
    suspend fun clearAllHistory(): AppResult<Unit>
}

