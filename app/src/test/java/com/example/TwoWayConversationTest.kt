package com.example

import com.example.core.result.AppResult
import com.example.modules.conversation.model.ConversationMessage
import com.example.modules.conversation.model.ConversationSpeaker
import com.example.modules.conversation.repository.DefaultConversationRepository
import com.example.modules.translation.model.TranslationRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TwoWayConversationTest {

    private lateinit var conversationRepo: DefaultConversationRepository
    private lateinit var fakeTranslationService: FakeTranslationService

    @Before
    fun setUp() {
        conversationRepo = DefaultConversationRepository()
        fakeTranslationService = FakeTranslationService()
    }

    @Test
    fun testInitialSession_DefaultLanguages() {
        val session = conversationRepo.currentSession.value
        assertEquals("hi", session.personALanguage)
        assertEquals("en", session.personBLanguage)
        assertTrue(session.messages.isEmpty())
        assertTrue(session.isAutoPlayAudio)
    }

    @Test
    fun testTwoWayTurnTaking_AddMessages() = runTest {
        // Person A speaks Hindi -> Translated to English
        val resA = conversationRepo.addMessage(
            speaker = ConversationSpeaker.PERSON_A,
            originalText = "नमस्ते, आप कैसे हैं?",
            translatedText = "Hello, how are you?",
            originalLanguage = "hi",
            targetLanguage = "en",
            originalLanguageName = "Hindi",
            targetLanguageName = "English",
            originalFlagEmoji = "🇮🇳",
            targetFlagEmoji = "🇺🇸"
        )
        assertTrue(resA is AppResult.Success)

        // Person B speaks English -> Translated to Hindi
        val resB = conversationRepo.addMessage(
            speaker = ConversationSpeaker.PERSON_B,
            originalText = "I am doing well, thank you!",
            translatedText = "मैं अच्छा कर रहा हूँ, धन्यवाद!",
            originalLanguage = "en",
            targetLanguage = "hi",
            originalLanguageName = "English",
            targetLanguageName = "Hindi",
            originalFlagEmoji = "🇺🇸",
            targetFlagEmoji = "🇮🇳"
        )
        assertTrue(resB is AppResult.Success)

        val messages = conversationRepo.currentSession.value.messages
        assertEquals(2, messages.size)

        // Verify Person A message
        assertEquals(ConversationSpeaker.PERSON_A, messages[0].speaker)
        assertEquals("नमस्ते, आप कैसे हैं?", messages[0].originalText)
        assertEquals("Hello, how are you?", messages[0].translatedText)
        assertEquals("hi", messages[0].originalLanguageCode)
        assertEquals("en", messages[0].targetLanguageCode)

        // Verify Person B message
        assertEquals(ConversationSpeaker.PERSON_B, messages[1].speaker)
        assertEquals("I am doing well, thank you!", messages[1].originalText)
        assertEquals("मैं अच्छा कर रहा हूँ, धन्यवाद!", messages[1].translatedText)
        assertEquals("en", messages[1].originalLanguageCode)
        assertEquals("hi", messages[1].targetLanguageCode)
    }

    @Test
    fun testSwapLanguages() {
        conversationRepo.setLanguages("hi", "en")
        conversationRepo.swapLanguages()

        val session = conversationRepo.currentSession.value
        assertEquals("en", session.personALanguage)
        assertEquals("hi", session.personBLanguage)
    }

    @Test
    fun testToggleAutoPlayAudio() {
        conversationRepo.setAutoPlayAudio(false)
        assertFalse(conversationRepo.currentSession.value.isAutoPlayAudio)

        conversationRepo.setAutoPlayAudio(true)
        assertTrue(conversationRepo.currentSession.value.isAutoPlayAudio)
    }

    @Test
    fun testClearConversation() = runTest {
        conversationRepo.addMessage(
            speaker = ConversationSpeaker.PERSON_A,
            originalText = "Test message",
            translatedText = "Mensaje de prueba",
            originalLanguage = "en",
            targetLanguage = "es"
        )
        assertEquals(1, conversationRepo.currentSession.value.messages.size)

        conversationRepo.clearSession()
        assertTrue(conversationRepo.currentSession.value.messages.isEmpty())
    }
}
