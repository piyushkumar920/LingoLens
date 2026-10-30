package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.modules.favorites.model.FavoritePhrase

@Entity(tableName = "favorite_phrases")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val category: String = "General",
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): FavoritePhrase {
        return FavoritePhrase(
            id = id,
            sourceText = sourceText,
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            category = category,
            notes = notes,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(phrase: FavoritePhrase): FavoriteEntity {
            return FavoriteEntity(
                id = phrase.id,
                sourceText = phrase.sourceText,
                translatedText = phrase.translatedText,
                sourceLanguage = phrase.sourceLanguage,
                targetLanguage = phrase.targetLanguage,
                category = phrase.category,
                notes = phrase.notes,
                timestamp = phrase.timestamp
            )
        }
    }
}
