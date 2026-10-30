package com.example.modules.favorites.model

data class FavoritePhrase(
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val category: String = "General",
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
