package com.example.modules.history.model

enum class TranslationType(val displayName: String) {
    TEXT("Text"),
    CAMERA("Camera"),
    IMAGE("Image"),
    VOICE("Voice"),
    CONVERSATION("Conversation"),
    TRAVEL("Travel");

    companion object {
        fun fromString(value: String?): TranslationType {
            if (value == null) return TEXT
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
        }
    }
}

typealias TranslationMode = TranslationType

data class TranslationHistoryItem(
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val translationType: TranslationType = TranslationType.TEXT,
    val isFavorite: Boolean = false,
    val explanation: String? = null
) {
    /**
     * Backward-compatible property for existing callers expecting `mode`.
     */
    val mode: TranslationType get() = translationType

    constructor(
        id: Long = 0,
        sourceText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String,
        mode: TranslationType,
        isFavorite: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        explanation: String? = null
    ) : this(
        id = id,
        sourceText = sourceText,
        translatedText = translatedText,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        timestamp = timestamp,
        translationType = mode,
        isFavorite = isFavorite,
        explanation = explanation
    )
}

