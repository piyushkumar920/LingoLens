package com.example.modules.conversation.repository

import com.example.core.result.AppResult
import com.example.modules.conversation.model.ConversationMessage
import com.example.modules.conversation.model.ConversationSession
import com.example.modules.conversation.model.ConversationSpeaker
import kotlinx.coroutines.flow.StateFlow

interface ConversationRepository {
    val currentSession: StateFlow<ConversationSession>

    fun startNewSession(personALanguage: String, personBLanguage: String)
    
    suspend fun addMessage(message: ConversationMessage): AppResult<ConversationMessage>

    suspend fun addMessage(
        speaker: ConversationSpeaker,
        originalText: String,
        translatedText: String,
        originalLanguage: String,
        targetLanguage: String,
        originalLanguageName: String = "",
        targetLanguageName: String = "",
        originalFlagEmoji: String = "🌐",
        targetFlagEmoji: String = "🌐"
    ): AppResult<ConversationMessage>

    fun setAutoPlayAudio(enabled: Boolean)
    fun setLanguages(personALanguage: String, personBLanguage: String)
    fun swapLanguages()
    fun removeMessage(id: String)
    fun clearSession()
}
