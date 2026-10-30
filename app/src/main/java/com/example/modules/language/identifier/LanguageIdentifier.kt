package com.example.modules.language.identifier

import com.example.core.result.AppResult

/**
 * Result of fine-grained language identification with confidence score.
 */
data class IdentifiedLanguageResult(
    val languageCode: String,
    val confidence: Float
)

/**
 * Contract for language identification.
 */
interface LanguageIdentifier {
    suspend fun identifyLanguage(text: String): AppResult<String>
    suspend fun identifyPossibleLanguages(text: String): AppResult<List<IdentifiedLanguageResult>>
    fun close()
}
