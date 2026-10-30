package com.example.modules.translation.model

enum class TranslationEngine {
    ON_DEVICE_ML_KIT,
    CLOUD_SERVICE
}

data class TranslationRequest(
    val sourceText: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val preferredEngine: TranslationEngine = TranslationEngine.ON_DEVICE_ML_KIT,
    val contextNotes: String? = null
)

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val engineUsed: TranslationEngine,
    val isOffline: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val alternativeTranslations: List<String> = emptyList(),
    val culturalInsight: String? = null
)
