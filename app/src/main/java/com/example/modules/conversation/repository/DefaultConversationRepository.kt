package com.example.modules.conversation.repository

import com.example.core.result.AppResult
import com.example.modules.conversation.model.ConversationMessage
import com.example.modules.conversation.model.ConversationSession
import com.example.modules.conversation.model.ConversationSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class DefaultConversationRepository : ConversationRepository {

    private val _currentSession = MutableStateFlow(
        ConversationSession(
            sessionId = UUID.randomUUID().toString(),
            personALanguage = "hi",
            personBLanguage = "en",
            messages = emptyList(),
            isAutoPlayAudio = true
        )
    )

    override val currentSession: StateFlow<ConversationSession> = _currentSession.asStateFlow()

    override fun startNewSession(personALanguage: String, personBLanguage: String) {
        _currentSession.value = ConversationSession(
            sessionId = UUID.randomUUID().toString(),
            personALanguage = personALanguage,
            personBLanguage = personBLanguage,
            messages = emptyList(),
            isAutoPlayAudio = _currentSession.value.isAutoPlayAudio
        )
    }

    override suspend fun addMessage(message: ConversationMessage): AppResult<ConversationMessage> {
        _currentSession.update { session ->
            session.copy(messages = session.messages + message)
        }
        return AppResult.Success(message)
    }

    override suspend fun addMessage(
        speaker: ConversationSpeaker,
        originalText: String,
        translatedText: String,
        originalLanguage: String,
        targetLanguage: String,
        originalLanguageName: String,
        targetLanguageName: String,
        originalFlagEmoji: String,
        targetFlagEmoji: String
    ): AppResult<ConversationMessage> {
        val newMessage = ConversationMessage(
            id = UUID.randomUUID().toString(),
            speaker = speaker,
            originalText = originalText,
            translatedText = translatedText,
            originalLanguageCode = originalLanguage,
            targetLanguageCode = targetLanguage,
            originalLanguageName = originalLanguageName,
            targetLanguageName = targetLanguageName,
            originalFlagEmoji = originalFlagEmoji,
            targetFlagEmoji = targetFlagEmoji,
            timestamp = System.currentTimeMillis()
        )
        _currentSession.update { session ->
            session.copy(messages = session.messages + newMessage)
        }
        return AppResult.Success(newMessage)
    }

    override fun setAutoPlayAudio(enabled: Boolean) {
        _currentSession.update { it.copy(isAutoPlayAudio = enabled) }
    }

    override fun setLanguages(personALanguage: String, personBLanguage: String) {
        _currentSession.update {
            it.copy(personALanguage = personALanguage, personBLanguage = personBLanguage)
        }
    }

    override fun swapLanguages() {
        _currentSession.update {
            it.copy(
                personALanguage = it.personBLanguage,
                personBLanguage = it.personALanguage
            )
        }
    }

    override fun removeMessage(id: String) {
        _currentSession.update { session ->
            session.copy(messages = session.messages.filter { it.id != id })
        }
    }

    override fun clearSession() {
        _currentSession.update { it.copy(messages = emptyList()) }
    }
}
