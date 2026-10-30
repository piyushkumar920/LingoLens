package com.example

import com.example.core.result.AppResult
import com.example.modules.language.model.Language
import com.example.modules.speech.repository.SpeechRecognitionRepository
import com.example.modules.speech.repository.SpeechRecognitionState
import com.example.modules.translation.model.TranslationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

class FakeSpeechRecognitionRepository : SpeechRecognitionRepository {
    private val _recognitionState = MutableStateFlow(SpeechRecognitionState())
    override val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    var lastStartedLanguage: String? = null
    var startListeningCallCount = 0
    var stopListeningCallCount = 0

    override fun startListening(languageCode: String): AppResult<Unit> {
        startListeningCallCount++
        lastStartedLanguage = languageCode
        _recognitionState.update {
            it.copy(isListening = true, isReady = true, partialText = "", finalResult = "", error = null)
        }
        return AppResult.Success(Unit)
    }

    override fun stopListening() {
        stopListeningCallCount++
        _recognitionState.update { it.copy(isListening = false, isReady = false) }
    }

    override fun cancel() {
        _recognitionState.update {
            it.copy(isListening = false, isReady = false, partialText = "", finalResult = "", error = null)
        }
    }

    override fun isRecognitionAvailable(): Boolean = true

    override fun clearResults() {
        _recognitionState.update { it.copy(partialText = "", finalResult = "", error = null) }
    }

    override fun release() {
        _recognitionState.update { it.copy(isListening = false) }
    }

    // Test simulation helpers
    fun emitPartial(text: String, soundDb: Float = 6f) {
        _recognitionState.update { it.copy(partialText = text, soundLevelDb = soundDb) }
    }

    fun emitFinal(text: String) {
        _recognitionState.update {
            it.copy(isListening = false, isReady = false, soundLevelDb = 0f, finalResult = text)
        }
    }

    fun emitError(error: String) {
        _recognitionState.update {
            it.copy(isListening = false, isReady = false, soundLevelDb = 0f, error = error)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceTranslationSubsystemTest {

    private lateinit var fakeSpeechRepo: FakeSpeechRecognitionRepository
    private lateinit var fakeLanguageId: FakeLanguageIdentifier
    private lateinit var fakeTranslationService: FakeTranslationService

    @Before
    fun setUp() {
        fakeSpeechRepo = FakeSpeechRecognitionRepository()
        fakeLanguageId = FakeLanguageIdentifier()
        fakeTranslationService = FakeTranslationService()
    }

    @Test
    fun testSpeechRecognition_StartAndPartialStreaming() {
        val res = fakeSpeechRepo.startListening("es-ES")
        assertTrue(res is AppResult.Success)
        assertEquals("es-ES", fakeSpeechRepo.lastStartedLanguage)
        assertTrue(fakeSpeechRepo.recognitionState.value.isListening)

        fakeSpeechRepo.emitPartial("Hola cómo", 8.5f)
        assertEquals("Hola cómo", fakeSpeechRepo.recognitionState.value.partialText)
        assertEquals(8.5f, fakeSpeechRepo.recognitionState.value.soundLevelDb, 0.01f)
    }

    @Test
    fun testSpeechRecognition_FinalResultEmission() {
        fakeSpeechRepo.startListening("en-US")
        fakeSpeechRepo.emitFinal("Where is the train station?")

        assertFalse(fakeSpeechRepo.recognitionState.value.isListening)
        assertEquals("Where is the train station?", fakeSpeechRepo.recognitionState.value.finalResult)
    }

    @Test
    fun testSpeechRecognition_ErrorHandling() {
        fakeSpeechRepo.startListening("fr-FR")
        fakeSpeechRepo.emitError("No speech detected. Please speak clearly into the microphone.")

        assertFalse(fakeSpeechRepo.recognitionState.value.isListening)
        assertEquals("No speech detected. Please speak clearly into the microphone.", fakeSpeechRepo.recognitionState.value.error)
    }

    @Test
    fun testVoiceToTranslationPipeline() = runTest {
        // 1. User speaks in French
        fakeSpeechRepo.startListening("fr-FR")
        fakeSpeechRepo.emitFinal("Où est la gare?")

        val recognizedText = fakeSpeechRepo.recognitionState.value.finalResult
        assertEquals("Où est la gare?", recognizedText)

        // 2. Identify language
        fakeLanguageId.stubbedDetection = "fr"
        val idRes = fakeLanguageId.identifyLanguage(recognizedText)
        assertTrue(idRes is AppResult.Success)
        val sourceLang = (idRes as AppResult.Success).data
        assertEquals("fr", sourceLang)

        // 3. Translate to English
        val transReq = TranslationRequest(
            sourceText = recognizedText,
            sourceLanguageCode = sourceLang,
            targetLanguageCode = "en"
        )
        val transRes = fakeTranslationService.translate(
            text = transReq.sourceText,
            sourceLanguageCode = transReq.sourceLanguageCode,
            targetLanguageCode = transReq.targetLanguageCode
        )

        assertTrue(transRes is AppResult.Success)
        val translatedText = (transRes as AppResult.Success).data
        assertTrue(translatedText.contains("Où est la gare?"))
    }
}
