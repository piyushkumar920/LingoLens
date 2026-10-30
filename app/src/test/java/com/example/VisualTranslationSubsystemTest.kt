package com.example

import com.example.core.result.AppResult
import com.example.modules.language.identifier.IdentifiedLanguageResult
import com.example.modules.language.identifier.LanguageIdentifier
import com.example.modules.language.model.Language
import com.example.modules.language.repository.DefaultLanguageRepository
import com.example.modules.ocr.model.BoundingBox
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.RecognizedLine
import com.example.modules.ocr.model.TextDetectionResult
import com.example.modules.translation.engine.VisualTranslationEngine
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.repository.DefaultTranslationRepository
import com.example.modules.translation.service.TranslationService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class VisualTestFakeLangId : LanguageIdentifier {
    override suspend fun identifyLanguage(text: String): AppResult<String> = AppResult.Success("fr")
    override suspend fun identifyPossibleLanguages(text: String): AppResult<List<IdentifiedLanguageResult>> =
        AppResult.Success(listOf(IdentifiedLanguageResult("fr", 0.99f)))
    override fun close() {}
}

private class VisualTestFakeTranslationService : TranslationService {
    var callCount = 0
    private val downloadedFlow = MutableStateFlow(setOf("en", "es", "fr"))

    override suspend fun translate(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): AppResult<String> {
        callCount++
        return AppResult.Success("$text [$targetLanguageCode]")
    }

    override suspend fun isModelDownloaded(languageCode: String): Boolean = true
    override suspend fun downloadModel(languageCode: String, requireWifi: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteModel(languageCode: String): AppResult<Unit> = AppResult.Success(Unit)
    override fun getDownloadedModels(): StateFlow<Set<String>> = downloadedFlow
    override fun isLanguageSupported(languageCode: String): Boolean = true
    override fun isLanguagePairSupported(sourceLanguageCode: String, targetLanguageCode: String): Boolean = true
    override fun close() {}

    override val lastRequestId: Int = 1
    override val lastResultStatus: String = "SUCCESS"
    override val lastErrorDetails: String? = null
    override val translatorLifecycleState: String = "READY"
    override val currentPair: String = "en -> fr"
    override val cacheHitCount: Int = 0
    override val cacheMissCount: Int = 0
    override val totalRequestsCount: Int = 0
    override val successfulRequestsCount: Int = 0
    override val failedRequestsCount: Int = 0
    override val lastLatencyMs: Long = 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VisualTranslationSubsystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeLanguageIdentifier: VisualTestFakeLangId
    private lateinit var fakeTranslationService: VisualTestFakeTranslationService
    private lateinit var languageRepository: DefaultLanguageRepository
    private lateinit var translationRepository: DefaultTranslationRepository
    private lateinit var visualTranslationEngine: VisualTranslationEngine

    @Before
    fun setUp() {
        fakeLanguageIdentifier = VisualTestFakeLangId()
        fakeTranslationService = VisualTestFakeTranslationService()
        languageRepository = DefaultLanguageRepository(fakeLanguageIdentifier, fakeTranslationService)
        translationRepository = DefaultTranslationRepository(
            fakeLanguageIdentifier,
            fakeTranslationService,
            languageRepository
        )
        visualTranslationEngine = VisualTranslationEngine(
            translationRepository = translationRepository,
            defaultDispatcher = testDispatcher
        )
    }

    @Test
    fun testVisualTranslationEngine_DirectFrameTranslation() = testScope.runTest {
        val ocrResult = TextDetectionResult(
            fullText = "Bonjour le monde\nBienvenue à Paris",
            blocks = listOf(
                RecognizedBlock(
                    text = "Bonjour le monde",
                    boundingBox = BoundingBox(10f, 20f, 100f, 50f),
                    lines = listOf(
                        RecognizedLine("Bonjour le monde", boundingBox = BoundingBox(10f, 20f, 100f, 50f))
                    )
                ),
                RecognizedBlock(
                    text = "Bienvenue à Paris",
                    boundingBox = BoundingBox(10f, 60f, 120f, 90f),
                    lines = listOf(
                        RecognizedLine("Bienvenue à Paris", boundingBox = BoundingBox(10f, 60f, 120f, 90f))
                    )
                )
            ),
            sourceWidth = 1080,
            sourceHeight = 1920
        )

        val translatedBlocks = visualTranslationEngine.translateFrameDirect(
            detectionResult = ocrResult,
            sourceLanguage = Language("fr", "French", "Français", "🇫🇷"),
            targetLanguage = Language("en", "English", "English", "🇺🇸")
        )

        assertEquals(2, translatedBlocks.size)
        assertEquals("Bonjour le monde [en]", translatedBlocks[0].translatedText)
        assertEquals("Bienvenue à Paris [en]", translatedBlocks[1].translatedText)
        assertEquals("fr", translatedBlocks[0].sourceLanguageCode)
        assertEquals("en", translatedBlocks[0].targetLanguageCode)
    }

    @Test
    fun testVisualTranslationEngine_CachingAvoidsDuplicateApiCalls() = testScope.runTest {
        val ocrResult = TextDetectionResult(
            fullText = "Exit",
            blocks = listOf(
                RecognizedBlock(
                    text = "Exit",
                    boundingBox = BoundingBox(50f, 50f, 150f, 100f)
                )
            ),
            sourceWidth = 1080,
            sourceHeight = 1920
        )

        val sourceLang = Language("en", "English", "English", "🇺🇸")
        val targetLang = Language("es", "Spanish", "Español", "🇪🇸")

        // First translation
        visualTranslationEngine.processOcrResult(
            detectionResult = ocrResult,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            scope = this,
            debounceMs = 0L
        )
        advanceUntilIdle()

        val state1 = visualTranslationEngine.state.value
        assertEquals(1, state1.blocks.size)
        assertEquals("Exit [es]", state1.blocks[0].translatedText)
        val initialCallCount = fakeTranslationService.callCount
        assertTrue(initialCallCount > 0)

        // Second translation of same text
        visualTranslationEngine.processOcrResult(
            detectionResult = ocrResult,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            scope = this,
            debounceMs = 0L
        )
        advanceUntilIdle()

        // Call count should NOT have increased because translation was cached!
        assertEquals(initialCallCount, fakeTranslationService.callCount)
        val cached = visualTranslationEngine.getCachedTranslation("Exit", "en", "es")
        assertEquals("Exit [es]", cached)
    }

    @Test
    fun testVisualTranslationEngine_EmptyOcrClearsState() = testScope.runTest {
        val emptyResult = TextDetectionResult(fullText = "", blocks = emptyList())
        visualTranslationEngine.processOcrResult(
            detectionResult = emptyResult,
            sourceLanguage = Language.AUTO_DETECT,
            targetLanguage = Language.ENGLISH,
            scope = this,
            debounceMs = 0L
        )

        val state = visualTranslationEngine.state.value
        assertTrue(state.blocks.isEmpty())
        assertFalse(state.isTranslating)
    }

    @Test
    fun testOverlayModes() {
        val modes = OverlayMode.values()
        assertTrue(modes.contains(OverlayMode.TRANSLATED))
        assertTrue(modes.contains(OverlayMode.ORIGINAL))
        assertTrue(modes.contains(OverlayMode.SIDE_BY_SIDE))
        assertTrue(modes.contains(OverlayMode.OFF))
    }
}
