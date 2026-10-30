package com.example.modules.translation.service

import com.example.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Replaceable Translation Service contract (ML Kit On-Device / Offline Translation Engine).
 */
interface TranslationService {
    suspend fun translate(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): AppResult<String>

    suspend fun isModelDownloaded(languageCode: String): Boolean
    suspend fun downloadModel(languageCode: String, requireWifi: Boolean = false): AppResult<Unit>
    suspend fun deleteModel(languageCode: String): AppResult<Unit>
    fun getDownloadedModels(): StateFlow<Set<String>>
    fun isLanguageSupported(languageCode: String): Boolean
    fun isLanguagePairSupported(sourceLanguageCode: String, targetLanguageCode: String): Boolean
    fun close()

    // Diagnostics properties
    val lastRequestId: Int
    val lastResultStatus: String
    val lastErrorDetails: String?
    val translatorLifecycleState: String
    val currentPair: String
    val cacheHitCount: Int
    val cacheMissCount: Int
    val totalRequestsCount: Int
    val successfulRequestsCount: Int
    val failedRequestsCount: Int
    val lastLatencyMs: Long
}
