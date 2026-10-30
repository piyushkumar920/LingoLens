package com.example.modules.conversation.model

/**
 * Identifier for the two participants in a dual-language conversation.
 */
enum class ConversationSpeaker {
    PERSON_A,
    PERSON_B;

    val other: ConversationSpeaker
        get() = if (this == PERSON_A) PERSON_B else PERSON_A
}

/**
 * A single translated message turn in a two-way conversation.
 */
data class ConversationMessage(
    val id: String,
    val speaker: ConversationSpeaker,
    val originalText: String,
    val translatedText: String,
    val originalLanguageCode: String,
    val targetLanguageCode: String,
    val originalLanguageName: String = "",
    val targetLanguageName: String = "",
    val originalFlagEmoji: String = "🌐",
    val targetFlagEmoji: String = "🌐",
    val timestamp: Long = System.currentTimeMillis(),
    val isAudioPlayed: Boolean = false
)

/**
 * The ongoing two-way conversation session state.
 */
data class ConversationSession(
    val sessionId: String,
    val personALanguage: String = "hi",
    val personBLanguage: String = "en",
    val messages: List<ConversationMessage> = emptyList(),
    val isAutoPlayAudio: Boolean = true
)
