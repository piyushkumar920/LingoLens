package com.example

import com.example.core.result.AppResult
import com.example.modules.language.identifier.IdentifiedLanguageResult
import com.example.modules.language.identifier.LanguageIdentifier
import com.example.modules.language.model.Language
import com.example.modules.language.repository.DefaultLanguageRepository
import com.example.modules.translation.model.TranslationEngine
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.repository.DefaultTranslationRepository
import com.example.modules.translation.service.TranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

class FakeLanguageIdentifier(
    var stubbedDetection: String = "es"
) : LanguageIdentifier {
    var identifyCalls = 0

    override suspend fun identifyLanguage(text: String): AppResult<String> {
        identifyCalls++
        return AppResult.Success(stubbedDetection)
    }

    override suspend fun identifyPossibleLanguages(text: String): AppResult<List<IdentifiedLanguageResult>> {
        return AppResult.Success(listOf(IdentifiedLanguageResult(stubbedDetection, 0.95f)))
    }

    override fun close() {}
}

class FakeTranslationService(
    var downloadedModelsSet: Set<String> = setOf("en", "es", "hi")
) : TranslationService {
    private val downloadedFlow = MutableStateFlow(downloadedModelsSet)
    var lastTranslatedText: String? = null

    override suspend fun translate(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): AppResult<String> {
        lastTranslatedText = text
        return AppResult.Success("Translated[$text] from $sourceLanguageCode to $targetLanguageCode")
    }

    override suspend fun isModelDownloaded(languageCode: String): Boolean {
        return downloadedFlow.value.contains(languageCode)
    }

    override suspend fun downloadModel(languageCode: String, requireWifi: Boolean): AppResult<Unit> {
        downloadedFlow.value = downloadedFlow.value + languageCode
        return AppResult.Success(Unit)
    }

    override suspend fun deleteModel(languageCode: String): AppResult<Unit> {
        downloadedFlow.value = downloadedFlow.value - languageCode
        return AppResult.Success(Unit)
    }

    override fun getDownloadedModels(): StateFlow<Set<String>> = downloadedFlow

    override fun isLanguageSupported(languageCode: String): Boolean = true

    override fun isLanguagePairSupported(sourceLanguageCode: String, targetLanguageCode: String): Boolean {
        return sourceLanguageCode != targetLanguageCode
    }

    override fun close() {}

    override val lastRequestId: Int = 1
    override val lastResultStatus: String = "SUCCESS"
    override val lastErrorDetails: String? = null
    override val translatorLifecycleState: String = "READY"
    override val currentPair: String = "en -> es"
    override val cacheHitCount: Int = 0
    override val cacheMissCount: Int = 0
    override val totalRequestsCount: Int = 0
    override val successfulRequestsCount: Int = 0
    override val failedRequestsCount: Int = 0
    override val lastLatencyMs: Long = 0L
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanguageAndTranslationSubsystemTest {

    private lateinit var fakeLanguageIdentifier: FakeLanguageIdentifier
    private lateinit var fakeTranslationService: FakeTranslationService
    private lateinit var languageRepository: DefaultLanguageRepository
    private lateinit var translationRepository: DefaultTranslationRepository

    @Before
    fun setUp() {
        fakeLanguageIdentifier = FakeLanguageIdentifier()
        fakeTranslationService = FakeTranslationService()
        languageRepository = DefaultLanguageRepository(fakeLanguageIdentifier, fakeTranslationService)
        translationRepository = DefaultTranslationRepository(
            fakeLanguageIdentifier,
            fakeTranslationService,
            languageRepository
        )
    }

    @Test
    fun testLanguageModel_AutoDetect() {
        val auto = Language.AUTO_DETECT
        assertTrue(auto.isAuto)
        assertEquals("auto", auto.code)
        assertEquals("🌐", auto.flagEmoji)
    }

    @Test
    fun testLanguageSearch_MatchesNameAndCode() = runTest {
        val searchSpanish = languageRepository.searchLanguages("spanish")
        assertTrue(searchSpanish.any { it.code == "es" })

        val searchCode = languageRepository.searchLanguages("hi")
        assertTrue(searchCode.any { it.code == "hi" })

        val searchNative = languageRepository.searchLanguages("日本語")
        assertTrue(searchNative.any { it.code == "ja" })
    }

    @Test
    fun testLanguageFavoritesAndRecents() = runTest {
        val hindi = languageRepository.findLanguageByCode("hi")
        assertNotNull(hindi)

        languageRepository.markLanguageUsed(hindi!!)
        val recents = languageRepository.recentLanguages.value
        assertEquals("hi", recents.firstOrNull()?.code)

        val french = languageRepository.findLanguageByCode("fr")!!
        assertFalse(french.isFavorite)
        languageRepository.toggleFavorite(french)
        val updatedFrench = languageRepository.findLanguageByCode("fr")!!
        assertTrue(updatedFrench.isFavorite)
    }

    @Test
    fun testLanguageSwap_WithAutoDetectSource() = runTest {
        val source = Language.AUTO_DETECT
        val target = languageRepository.findLanguageByCode("ja")!!

        val (newSource, newTarget) = languageRepository.swapLanguages(source, target)
        assertEquals("ja", newSource.code)
        assertEquals("en", newTarget.code)
    }

    @Test
    fun testLanguageSwap_WithRegularLanguages() = runTest {
        val source = languageRepository.findLanguageByCode("fr")!!
        val target = languageRepository.findLanguageByCode("de")!!

        val (newSource, newTarget) = languageRepository.swapLanguages(source, target)
        assertEquals("de", newSource.code)
        assertEquals("fr", newTarget.code)
    }

    @Test
    fun testAutoDetectTranslationPipeline() = runTest {
        fakeLanguageIdentifier.stubbedDetection = "fr"

        val request = TranslationRequest(
            sourceText = "Bonjour le monde",
            sourceLanguageCode = "auto",
            targetLanguageCode = "en"
        )

        val result = translationRepository.translate(request)
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data

        assertEquals(1, fakeLanguageIdentifier.identifyCalls)
        assertEquals("fr", data.sourceLanguageCode)
        assertEquals("en", data.targetLanguageCode)
        assertEquals(TranslationEngine.ON_DEVICE_ML_KIT, data.engineUsed)
    }

    @Test
    fun testManualTranslationPipeline() = runTest {
        val request = TranslationRequest(
            sourceText = "Hello world",
            sourceLanguageCode = "en",
            targetLanguageCode = "es"
        )

        val result = translationRepository.translate(request)
        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data

        assertEquals(0, fakeLanguageIdentifier.identifyCalls) // Auto-detect not called when source is specified
        assertEquals("en", data.sourceLanguageCode)
        assertEquals("es", data.targetLanguageCode)
        assertTrue(data.translatedText.contains("Hello world"))
    }

    @Test
    fun testModelDownloadAndOfflineStatus() = runTest {
        val isJapaneseDownloadedBefore = translationRepository.isModelDownloaded("ja")
        assertFalse(isJapaneseDownloadedBefore)

        val downloadRes = translationRepository.downloadModel("ja")
        assertTrue(downloadRes is AppResult.Success)

        val isJapaneseDownloadedAfter = translationRepository.isModelDownloaded("ja")
        assertTrue(isJapaneseDownloadedAfter)
    }

    @Test
    fun testMlKitLanguageSelectionAndSwap() = runTest {
        val allLangs = languageRepository.getSupportedLanguages().first()
        val english = allLangs.first { it.code == "en" }
        val spanish = allLangs.first { it.code == "es" }

        val (swappedSource, swappedTarget) = languageRepository.swapLanguages(english, spanish)
        assertEquals("es", swappedSource.code)
        assertEquals("en", swappedTarget.code)

        val searched = languageRepository.searchLanguages("french")
        assertTrue(searched.any { it.code == "fr" })

        val searchedJapanese = languageRepository.searchLanguages("ja")
        assertTrue(searchedJapanese.any { it.code == "ja" })
    }
}
