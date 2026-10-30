package com.example.modules.translation.repository

import com.example.core.result.AppResult
import com.example.modules.language.model.Language
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.model.TranslationResult
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level translation orchestration repository.
 */
interface TranslationRepository {
    suspend fun translate(request: TranslationRequest): AppResult<TranslationResult>
    suspend fun isModelDownloaded(languageCode: String): Boolean
    suspend fun downloadModel(languageCode: String, requireWifi: Boolean = false): AppResult<Unit>
    suspend fun deleteModel(languageCode: String): AppResult<Unit>
    fun getDownloadedModels(): StateFlow<Set<String>>
    fun isLanguagePairSupported(sourceCode: String, targetCode: String): Boolean
}
