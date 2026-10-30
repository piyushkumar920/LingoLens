package com.example.modules.translation.engine

import android.util.Log
import com.example.core.result.AppResult
import com.example.modules.language.model.Language
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.TextDetectionResult
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.model.TranslatedBlock
import com.example.modules.translation.model.TranslatedLine
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.model.VisualTranslationState
import com.example.modules.translation.repository.TranslationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance visual translation engine.
 * Connects OCR detection results to the Translation pipeline.
 *
 * Implements:
 * - In-memory translation caching (LRU / hash-based per text + language pair)
 * - Stationary text detection to eliminate duplicate translations
 * - Asynchronous debounced processing to avoid camera preview hitching
 * - Concurrency control with cancellation of stale frames
 */
class VisualTranslationEngine(
    private val translationRepository: TranslationRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val tag = "VisualTranslationEngine"

    // Translation cache: "sourceText:::sourceLang:::targetLang" -> translatedText
    private val translationCache = ConcurrentHashMap<String, String>()

    // Detected language cache: "sourceText" -> detectedLangCode
    private val detectedLangCache = ConcurrentHashMap<String, String>()

    private val _state = MutableStateFlow(VisualTranslationState())
    val state: StateFlow<VisualTranslationState> = _state.asStateFlow()

    private var activeTranslationJob: Job? = null
    private var lastProcessedResultId: Long = 0
    private var lastProcessedSignature: Int = 0

    private fun normalizeForCache(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    private fun buildCacheKey(text: String, sourceLang: String, targetLang: String): String {
        return "${normalizeForCache(text)}:::${sourceLang.lowercase()}:::${targetLang.lowercase()}"
    }

    /**
     * Clear all cached translations.
     */
    fun clearCache() {
        translationCache.clear()
        detectedLangCache.clear()
        lastProcessedSignature = 0
    }

    /**
     * Synchronously looks up cached translation if available.
     */
    fun getCachedTranslation(text: String, sourceLang: String, targetLang: String): String? {
        return translationCache[buildCacheKey(text, sourceLang, targetLang)]
    }

    /**
     * Process OCR results in real-time with debouncing and selective delta translation.
     */
    fun processOcrResult(
        detectionResult: TextDetectionResult,
        sourceLanguage: Language,
        targetLanguage: Language,
        scope: CoroutineScope,
        debounceMs: Long = 150L
    ) {
        if (detectionResult.isEmpty || detectionResult.blocks.isEmpty()) {
            _state.value = _state.value.copy(
                blocks = emptyList(),
                sourceWidth = detectionResult.sourceWidth,
                sourceHeight = detectionResult.sourceHeight,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                isTranslating = false
            )
            return
        }

        val signature = detectionResult.contentSignature
        val currentBlocks = detectionResult.blocks
        val allCached = currentBlocks.all { block ->
            val srcCode = if (sourceLanguage.code == "auto" && !block.detectedLanguage.isNullOrBlank()) block.detectedLanguage!! else sourceLanguage.code
            translationCache.containsKey(buildCacheKey(block.text, srcCode, targetLanguage.code)) ||
            translationCache.containsKey(buildCacheKey(block.text, sourceLanguage.code, targetLanguage.code))
        }

        if (signature == lastProcessedSignature && allCached && !_state.value.isTranslating) {
            // Text is identical and already fully cached, skip processing entirely for zero lag!
            return
        }
        lastProcessedSignature = signature

        val pipelineStart = System.currentTimeMillis()

        // Cancel previous pending frame translation if it hasn't completed
        activeTranslationJob?.cancel()
        activeTranslationJob = scope.launch(defaultDispatcher) {
            if (debounceMs > 0) {
                delay(debounceMs)
            }

            val previousTranslatedBlocks = _state.value.blocks.associateBy { it.id }

            // 1. Initial immediate mapping (use cache or previous translation to avoid UI flicker)
            val initialBlocks = currentBlocks.map { rawBlock ->
                val blockId = "${rawBlock.text.hashCode()}_${rawBlock.boundingBox?.left?.toInt() ?: 0}"
                val detectedSrc = rawBlock.detectedLanguage ?: detectedLangCache[rawBlock.text.trim()]
                val effectiveSrc = if (sourceLanguage.code == "auto" && !detectedSrc.isNullOrBlank()) detectedSrc else sourceLanguage.code

                val cachedTranslation = translationCache[buildCacheKey(rawBlock.text, effectiveSrc, targetLanguage.code)]
                    ?: translationCache[buildCacheKey(rawBlock.text, sourceLanguage.code, targetLanguage.code)]

                val prev = previousTranslatedBlocks[blockId]

                val translatedText = cachedTranslation
                    ?: prev?.translatedText
                    ?: ""

                val isStillTranslating = translatedText.isBlank() && rawBlock.text.isNotBlank()

                val blockAngle = rawBlock.lines.firstOrNull()?.angle ?: 0f
                val blockConfidence = rawBlock.lines.firstOrNull()?.confidence

                TranslatedBlock(
                    id = blockId,
                    originalText = rawBlock.text,
                    translatedText = translatedText,
                    sourceLanguageCode = sourceLanguage.code,
                    targetLanguageCode = targetLanguage.code,
                    boundingBox = rawBlock.boundingBox ?: com.example.modules.ocr.model.BoundingBox(0f, 0f, 0f, 0f),
                    lines = rawBlock.lines.map { line ->
                        TranslatedLine(
                            originalText = line.text,
                            translatedText = null,
                            boundingBox = line.boundingBox,
                            confidence = line.confidence
                        )
                    },
                    angle = blockAngle,
                    confidence = blockConfidence,
                    isTranslating = isStillTranslating,
                    detectedLanguageCode = detectedSrc
                )
            }

            // Publish instantaneous state with available cached translations
            withContext(Dispatchers.Main.immediate) {
                _state.value = _state.value.copy(
                    blocks = initialBlocks,
                    sourceWidth = detectionResult.sourceWidth,
                    sourceHeight = detectionResult.sourceHeight,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    isTranslating = initialBlocks.any { it.isTranslating }
                )
            }

            // 2. Fetch missing translations concurrently
            val missingBlocks = initialBlocks.filter { it.translatedText.isBlank() && it.originalText.isNotBlank() }
            if (missingBlocks.isNotEmpty()) {
                val updatedBlocks = initialBlocks.toMutableList()

                coroutineScope {
                    val deferredList = missingBlocks.map { block ->
                        async {
                            translateSingleBlock(block, sourceLanguage.code, targetLanguage.code)
                        }
                    }

                    val results = deferredList.awaitAll()
                    val resultMap = results.associateBy { it.id }

                    for (i in updatedBlocks.indices) {
                        val updated = resultMap[updatedBlocks[i].id]
                        if (updated != null) {
                            updatedBlocks[i] = updated
                        }
                    }
                }

                withContext(Dispatchers.Main.immediate) {
                    _state.value = _state.value.copy(
                        blocks = updatedBlocks,
                        isTranslating = false
                    )
                }
            }

            val totalDuration = System.currentTimeMillis() - pipelineStart
            Log.d("VISION_CAMERA", "Total pipeline duration: ${totalDuration}ms (OCR: ${detectionResult.processingTimeMs}ms, blocks: ${currentBlocks.size})")
        }
    }

    /**
     * Direct one-shot translation for a frozen frame or captured image.
     */
    suspend fun translateFrameDirect(
        detectionResult: TextDetectionResult,
        sourceLanguage: Language,
        targetLanguage: Language
    ): List<TranslatedBlock> = withContext(defaultDispatcher) {
        if (detectionResult.isEmpty || detectionResult.blocks.isEmpty()) {
            return@withContext emptyList()
        }

        val rawBlocks = detectionResult.blocks
        coroutineScope {
            val deferreds = rawBlocks.map { rawBlock ->
                async {
                    val blockId = "${rawBlock.text.hashCode()}_${rawBlock.boundingBox?.left?.toInt() ?: 0}"
                    val blockAngle = rawBlock.lines.firstOrNull()?.angle ?: 0f
                    val blockConfidence = rawBlock.lines.firstOrNull()?.confidence
                    val initialBlock = TranslatedBlock(
                        id = blockId,
                        originalText = rawBlock.text,
                        translatedText = "",
                        sourceLanguageCode = sourceLanguage.code,
                        targetLanguageCode = targetLanguage.code,
                        boundingBox = rawBlock.boundingBox ?: com.example.modules.ocr.model.BoundingBox(0f, 0f, 0f, 0f),
                        lines = rawBlock.lines.map { line ->
                            TranslatedLine(
                                originalText = line.text,
                                boundingBox = line.boundingBox,
                                confidence = line.confidence
                            )
                        },
                        angle = blockAngle,
                        confidence = blockConfidence
                    )
                    translateSingleBlock(initialBlock, sourceLanguage.code, targetLanguage.code)
                }
            }
            deferreds.awaitAll()
        }
    }

    private suspend fun translateSingleBlock(
        block: TranslatedBlock,
        sourceLangCode: String,
        targetLangCode: String
    ): TranslatedBlock {
        val effectiveSource = if (sourceLangCode.equals("auto", ignoreCase = true) && !block.detectedLanguageCode.isNullOrBlank()) {
            block.detectedLanguageCode!!
        } else {
            sourceLangCode
        }

        val cacheKey = buildCacheKey(block.originalText, effectiveSource, targetLangCode)
        val fallbackCacheKey = buildCacheKey(block.originalText, sourceLangCode, targetLangCode)
        val cached = translationCache[cacheKey] ?: translationCache[fallbackCacheKey]
        if (cached != null) {
            return block.copy(
                translatedText = cached,
                isTranslating = false,
                hasError = false,
                detectedLanguageCode = block.detectedLanguageCode ?: detectedLangCache[block.originalText.trim()]
            )
        }

        val request = TranslationRequest(
            sourceText = block.originalText,
            sourceLanguageCode = effectiveSource,
            targetLanguageCode = targetLangCode
        )

        return when (val res = translationRepository.translate(request)) {
            is AppResult.Success -> {
                val translated = res.data.translatedText
                val detectedLang = res.data.sourceLanguageCode
                translationCache[cacheKey] = translated
                translationCache[fallbackCacheKey] = translated
                if (detectedLang.isNotBlank()) {
                    detectedLangCache[block.originalText.trim()] = detectedLang
                }
                block.copy(
                    translatedText = translated,
                    isTranslating = false,
                    hasError = false,
                    detectedLanguageCode = detectedLang
                )
            }
            is AppResult.Error -> {
                Log.w(tag, "Failed to translate block: ${res.error.message}")
                block.copy(
                    translatedText = block.originalText, // Fallback to original
                    isTranslating = false,
                    hasError = true
                )
            }
            is AppResult.Loading -> block.copy(isTranslating = false)
        }
    }
}
