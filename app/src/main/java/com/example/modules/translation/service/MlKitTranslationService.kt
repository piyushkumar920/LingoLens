package com.example.modules.translation.service

import android.util.Log
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * High-performance on-device Translation Service powered by Google ML Kit.
 */
class MlKitTranslationService(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TranslationService {

    private val tag = "VISION_TRANSLATION"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val modelManager = RemoteModelManager.getInstance()

    private val translators = ConcurrentHashMap<String, Translator>()
    private val readyTranslatorPairs = ConcurrentHashMap.newKeySet<String>()
    private val translationCache = ConcurrentHashMap<String, String>()
    private val _downloadedModels = MutableStateFlow<Set<String>>(emptySet())
    private val requestCounter = AtomicInteger(0)

    // Diagnostics state exposed for UI and tests
    override var lastRequestId: Int = 0
        private set
    override var lastResultStatus: String = "NONE"
        private set
    override var lastErrorDetails: String? = null
        private set
    override var translatorLifecycleState: String = "NONE"
        private set
    override var currentPair: String = "none"
        private set
    override var cacheHitCount: Int = 0
        private set
    override var cacheMissCount: Int = 0
        private set
    override var totalRequestsCount: Int = 0
        private set
    override var successfulRequestsCount: Int = 0
        private set
    override var failedRequestsCount: Int = 0
        private set
    override var lastLatencyMs: Long = 0L
        private set

    init {
        refreshDownloadedModels()
    }

    private fun getPairKey(source: String, target: String): String = "${source.lowercase()}_${target.lowercase()}"

    private fun normalizeLanguageCode(code: String): String? {
        val clean = code.trim().lowercase()
        val mapped = when (clean) {
            "zh-cn", "zh-hans", "chinese" -> "zh"
            "hi-in", "hindi" -> "hi"
            "ja-jp", "japanese" -> "ja"
            "ko-kr", "korean" -> "ko"
            "es-es", "spanish" -> "es"
            "fr-fr", "french" -> "fr"
            "de-de", "german" -> "de"
            "it-it", "italian" -> "it"
            "pt-br", "pt-pt", "portuguese" -> "pt"
            "ru-ru", "russian" -> "ru"
            "ar-sa", "arabic" -> "ar"
            "vi-vn", "vietnamese" -> "vi"
            "tr-tr", "turkish" -> "tr"
            else -> clean
        }
        return TranslateLanguage.fromLanguageTag(mapped)
    }

    override fun isLanguageSupported(languageCode: String): Boolean {
        return normalizeLanguageCode(languageCode) != null
    }

    override fun isLanguagePairSupported(sourceLanguageCode: String, targetLanguageCode: String): Boolean {
        val src = normalizeLanguageCode(sourceLanguageCode)
        val tgt = normalizeLanguageCode(targetLanguageCode)
        return src != null && tgt != null && src != tgt
    }

    override suspend fun translate(
        text: String,
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): AppResult<String> = withContext(ioDispatcher) {
        val reqId = requestCounter.incrementAndGet()
        lastRequestId = reqId
        currentPair = "$sourceLanguageCode → $targetLanguageCode"

        val trimmed = text.trim()
        Log.d(tag, "REQUEST START #$reqId: source=$sourceLanguageCode, target=$targetLanguageCode, length=${trimmed.length}")

        if (trimmed.isBlank()) {
            lastResultStatus = "SUCCESS"
            lastErrorDetails = null
            return@withContext AppResult.Success("")
        }

        val srcTag = normalizeLanguageCode(sourceLanguageCode)
        ?: run {
            val err = "Source language '$sourceLanguageCode' is not supported."
            lastResultStatus = "ERROR"
            lastErrorDetails = err
            Log.e(tag, "REQUEST ERROR #$reqId: $err")
            return@withContext AppResult.Error(DomainError.LanguageNotSupportedError(err))
        }

        val tgtTag = normalizeLanguageCode(targetLanguageCode)
        ?: run {
            val err = "Target language '$targetLanguageCode' is not supported."
            lastResultStatus = "ERROR"
            lastErrorDetails = err
            Log.e(tag, "REQUEST ERROR #$reqId: $err")
            return@withContext AppResult.Error(DomainError.LanguageNotSupportedError(err))
        }

        if (srcTag == tgtTag) {
            lastResultStatus = "SUCCESS"
            lastErrorDetails = null
            Log.d(tag, "REQUEST SUCCESS #$reqId (Same language)")
            return@withContext AppResult.Success(trimmed)
        }

        totalRequestsCount++
        val pairKey = getPairKey(srcTag, tgtTag)
        val cacheKey = "${pairKey}::${trimmed.lowercase()}"
        val cached = translationCache[cacheKey]
        if (cached != null) {
            cacheHitCount++
            successfulRequestsCount++
            lastLatencyMs = 0L
            lastResultStatus = "SUCCESS"
            lastErrorDetails = null
            Log.d(tag, "CACHE HIT for $pairKey (Request #$reqId)")
            return@withContext AppResult.Success(cached)
        }
        cacheMissCount++

        // Check if translator already exists in cache or create a new one
        val translator: Translator = translators.computeIfAbsent(pairKey) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(srcTag)
                .setTargetLanguage(tgtTag)
                .build()
            Translation.getClient(options)
        }
        translatorLifecycleState = if (readyTranslatorPairs.contains(pairKey)) "REUSED" else "ACTIVE"

        // 1. If pair was already initialized and verified ready, execute directly (0ms network overhead, pure offline)
        if (readyTranslatorPairs.contains(pairKey)) {
            val directResult = executeTranslationInternal(translator, trimmed, pairKey, reqId)
            if (directResult is AppResult.Success) {
                return@withContext directResult
            }
            // If failed, fall back to model check below
            readyTranslatorPairs.remove(pairKey)
        }

        // 2. Check if models are already downloaded
        val srcDownloaded = isModelEffectivelyDownloaded(srcTag)
        val tgtDownloaded = isModelEffectivelyDownloaded(tgtTag)

        val ensureReady = if (srcDownloaded && tgtDownloaded) {
            Log.d(tag, "MODELS ALREADY INSTALLED for $pairKey (Request #$reqId)")
            readyTranslatorPairs.add(pairKey)
            true
        } else {
            Log.d(tag, "MODEL DOWNLOAD INITIATED for $pairKey (Request #$reqId)")
            val downloadConditions = DownloadConditions.Builder().build()
            val downloadResult = withTimeoutOrNull(20000L) {
                suspendCancellableCoroutine { continuation ->
                    translator.downloadModelIfNeeded(downloadConditions)
                        .addOnSuccessListener {
                            refreshDownloadedModels()
                            readyTranslatorPairs.add(pairKey)
                            if (continuation.isActive) continuation.resume(true)
                        }
                        .addOnFailureListener { e ->
                            Log.w(tag, "Model downloadIfNeeded listener reported: ${e.message}")
                            if (continuation.isActive) continuation.resume(false)
                        }
                }
            } ?: false
            downloadResult
        }

        // 3. If download failed or timed out, attempt execution anyway (model may already exist on disk offline)
        if (!ensureReady) {
            Log.d(tag, "Checking if translator can execute offline without explicit download acknowledgment")
            val offlineAttempt = executeTranslationInternal(translator, trimmed, pairKey, reqId)
            if (offlineAttempt is AppResult.Success) {
                readyTranslatorPairs.add(pairKey)
                return@withContext offlineAttempt
            }

            val err = "Language model for $sourceLanguageCode → $targetLanguageCode is not ready. Please verify connection to download model."
            lastResultStatus = "ERROR"
            lastErrorDetails = err
            Log.e(tag, "REQUEST ERROR #$reqId: $err")
            return@withContext AppResult.Error(DomainError.TranslationError(err))
        }

        // 4. Normal execution
        val result = executeTranslationInternal(translator, trimmed, pairKey, reqId)
        if (result is AppResult.Success) {
            readyTranslatorPairs.add(pairKey)
        }
        return@withContext result
    }

    private suspend fun executeTranslationInternal(
        translator: Translator,
        trimmed: String,
        pairKey: String,
        reqId: Int
    ): AppResult<String> {
        val startTime = System.currentTimeMillis()
        val translationResult = withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine<AppResult<String>> { continuation ->
                translator.translate(trimmed)
                    .addOnSuccessListener { translated ->
                        if (continuation.isActive) {
                            continuation.resume(AppResult.Success(translated))
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(tag, "Translation execution failed for $pairKey", exception)
                        if (continuation.isActive) {
                            continuation.resume(
                                AppResult.Error(
                                    DomainError.TranslationError(
                                        exception.localizedMessage ?: "Translation failed",
                                        exception
                                    )
                                )
                            )
                        }
                    }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        lastLatencyMs = duration
        return when (translationResult) {
            is AppResult.Success -> {
                successfulRequestsCount++
                val cacheKey = "${pairKey}::${trimmed.lowercase()}"
                translationCache[cacheKey] = translationResult.data
                lastResultStatus = "SUCCESS"
                lastErrorDetails = null
                Log.d(tag, "TRANSLATION SUCCESS #$reqId in ${duration}ms")
                translationResult
            }
            is AppResult.Error -> {
                failedRequestsCount++
                lastResultStatus = "ERROR"
                lastErrorDetails = translationResult.error.message
                Log.e(tag, "TRANSLATION FAILURE #$reqId: ${translationResult.error.message}")
                translationResult
            }
            is AppResult.Loading -> AppResult.Loading
            null -> {
                failedRequestsCount++
                val err = "Translation request timed out."
                lastResultStatus = "ERROR"
                lastErrorDetails = err
                Log.e(tag, "REQUEST ERROR #$reqId: $err")
                AppResult.Error(DomainError.TranslationError(err))
            }
        }
    }

    private suspend fun isModelEffectivelyDownloaded(langTag: String): Boolean {
        // English is the built-in base pivot language in ML Kit Translate
        if (langTag == "en") return true
        if (_downloadedModels.value.contains(langTag)) return true
        return isModelDownloaded(langTag)
    }

    override suspend fun isModelDownloaded(languageCode: String): Boolean = withContext(ioDispatcher) {
        val langTag = normalizeLanguageCode(languageCode) ?: return@withContext false
        if (langTag == "en") return@withContext true
        if (_downloadedModels.value.contains(langTag)) return@withContext true

        val model = TranslateRemoteModel.Builder(langTag).build()
        withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { continuation ->
                modelManager.isModelDownloaded(model)
                    .addOnSuccessListener { isDownloaded ->
                        if (isDownloaded) {
                            _downloadedModels.update { it + langTag }
                        }
                        if (continuation.isActive) continuation.resume(isDownloaded)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(false)
                    }
            }
        } ?: false
    }

    override suspend fun downloadModel(
        languageCode: String,
        requireWifi: Boolean
    ): AppResult<Unit> = withContext(ioDispatcher) {
        val langTag = normalizeLanguageCode(languageCode)
            ?: return@withContext AppResult.Error(
                DomainError.LanguageNotSupportedError("Cannot download offline model: '$languageCode' is not supported.")
            )

        val model = TranslateRemoteModel.Builder(langTag).build()
        val conditionsBuilder = DownloadConditions.Builder()
        if (requireWifi) {
            conditionsBuilder.requireWifi()
        }

        suspendCancellableCoroutine<AppResult<Unit>> { continuation ->
            modelManager.download(model, conditionsBuilder.build())
                .addOnSuccessListener {
                    Log.d(tag, "Successfully downloaded model for $languageCode")
                    refreshDownloadedModels()
                    if (continuation.isActive) continuation.resume(AppResult.Success(Unit))
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Failed to download model for $languageCode", exception)
                    if (continuation.isActive) {
                        continuation.resume(
                            AppResult.Error(
                                DomainError.TranslationError(
                                    exception.localizedMessage ?: "Failed to download translation model",
                                    exception
                                )
                            )
                        )
                    }
                }
        }
    }

    override suspend fun deleteModel(languageCode: String): AppResult<Unit> = withContext(ioDispatcher) {
        val langTag = normalizeLanguageCode(languageCode)
            ?: return@withContext AppResult.Error(
                DomainError.LanguageNotSupportedError("Cannot delete model: '$languageCode' is invalid.")
            )

        val model = TranslateRemoteModel.Builder(langTag).build()

        suspendCancellableCoroutine<AppResult<Unit>> { continuation ->
            modelManager.deleteDownloadedModel(model)
                .addOnSuccessListener {
                    Log.d(tag, "Successfully deleted model for $languageCode")
                    refreshDownloadedModels()
                    if (continuation.isActive) continuation.resume(AppResult.Success(Unit))
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Failed to delete model for $languageCode", exception)
                    if (continuation.isActive) {
                        continuation.resume(
                            AppResult.Error(
                                DomainError.TranslationError(
                                    exception.localizedMessage ?: "Failed to delete translation model",
                                    exception
                                )
                            )
                        )
                    }
                }
        }
    }

    override fun getDownloadedModels(): StateFlow<Set<String>> = _downloadedModels.asStateFlow()

    private fun refreshDownloadedModels() {
        serviceScope.launch(ioDispatcher) {
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    val languageCodes = models.map { it.language }.toSet()
                    _downloadedModels.update { languageCodes }
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Failed to list downloaded models", e)
                }
        }
    }

    override fun close() {
        translators.values.forEach { translator ->
            try {
                translator.close()
            } catch (e: Exception) {
                Log.e(tag, "Error closing Translator", e)
            }
        }
        translators.clear()
        readyTranslatorPairs.clear()
        translatorLifecycleState = "CLOSED"
    }
}
