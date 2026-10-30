package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationType

@Entity(
    tableName = "translation_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["translationType"]),
        Index(value = ["isFavorite"]),
        Index(value = ["sourceLanguage", "targetLanguage"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val translationType: String = TranslationType.TEXT.name,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val explanation: String? = null
) {
    fun toDomain(): TranslationHistoryItem {
        val domainType = TranslationType.fromString(translationType)
        return TranslationHistoryItem(
            id = id,
            sourceText = sourceText,
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            timestamp = timestamp,
            translationType = domainType,
            isFavorite = isFavorite,
            explanation = explanation
        )
    }

    companion object {
        fun fromDomain(item: TranslationHistoryItem): HistoryEntity {
            return HistoryEntity(
                id = item.id,
                sourceText = item.sourceText,
                translatedText = item.translatedText,
                sourceLanguage = item.sourceLanguage,
                targetLanguage = item.targetLanguage,
                translationType = item.translationType.name,
                isFavorite = item.isFavorite,
                timestamp = item.timestamp,
                explanation = item.explanation
            )
        }
    }
}

