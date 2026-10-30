package com.example.modules.image_translation.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.core.image.ImageEnhanceMode
import com.example.core.image.ImageProcessingUtils
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationMode
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.image_translation.model.ImageOcrStatus
import com.example.modules.image_translation.model.ImageTranslationUiState
import com.example.modules.language.identifier.LanguageIdentifier
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.ocr.model.OcrScript
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.TextDetectionResult
import com.example.modules.ocr.repository.OcrRepository
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.model.TranslatedBlock
import com.example.modules.translation.model.TranslatedLine
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.repository.TranslationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class DefaultImageTranslationRepository(
    private val ocrRepository: OcrRepository,
    private val translationRepository: TranslationRepository,
    private val languageRepository: LanguageRepository,
    private val languageIdentifier: LanguageIdentifier? = null,
    private val historyRepository: HistoryRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImageTranslationRepository {

    private val tag = "DefaultImageTranslationRepo"

    private val _uiState = MutableStateFlow(ImageTranslationUiState())
    override val uiState: StateFlow<ImageTranslationUiState> = _uiState.asStateFlow()

    private val translationMemoryCache = ConcurrentHashMap<String, String>()

    override suspend fun loadImage(context: Context, uri: Uri): AppResult<Unit> = withContext(ioDispatcher) {
        _uiState.update {
            it.copy(
                imageUri = uri,
                status = ImageOcrStatus.LOADING_IMAGE,
                errorMessage = null,
                selectedBlock = null,
                zoomScale = 1.0f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }

        val bitmap = ImageProcessingUtils.loadBitmapSafely(context, uri)
        if (bitmap == null) {
            _uiState.update {
                it.copy(
                    status = ImageOcrStatus.ERROR,
                    errorMessage = "Failed to load image from storage. Please verify file permissions or select another image."
                )
            }
            return@withContext AppResult.Error(DomainError.OcrProcessingError("Failed to decode image from $uri"))
        }

        _uiState.update {
            it.copy(
                loadedBitmap = bitmap,
                displayedBitmap = bitmap,
                rotationDegrees = 0,
                enhanceMode = ImageEnhanceMode.ORIGINAL
            )
        }

        processCurrentBitmap()
    }

    override suspend fun setBitmap(bitmap: Bitmap, autoProcess: Boolean): AppResult<Unit> = withContext(ioDispatcher) {
        _uiState.update {
            it.copy(
                imageUri = null,
                loadedBitmap = bitmap,
                displayedBitmap = bitmap,
                rotationDegrees = 0,
                enhanceMode = ImageEnhanceMode.ORIGINAL,
                status = if (autoProcess) ImageOcrStatus.RUNNING_OCR else ImageOcrStatus.IDLE,
                errorMessage = null,
                selectedBlock = null,
                zoomScale = 1.0f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }

        if (autoProcess) {
            processCurrentBitmap()
        } else {
            AppResult.Success(Unit)
        }
    }

    override suspend fun rotateImage(degrees: Float): AppResult<Unit> = withContext(ioDispatcher) {
        val currentLoaded = _uiState.value.loadedBitmap ?: return@withContext AppResult.Error(DomainError.UnknownError("No image loaded"))
        val rotated = ImageProcessingUtils.rotateBitmap(currentLoaded, degrees)
        val enhanced = ImageProcessingUtils.applyEnhancement(rotated, _uiState.value.enhanceMode)

        _uiState.update {
            it.copy(
                loadedBitmap = rotated,
                displayedBitmap = enhanced,
                rotationDegrees = (it.rotationDegrees + degrees.toInt()) % 360
            )
        }

        processCurrentBitmap()
    }

    override suspend fun setEnhancementMode(mode: ImageEnhanceMode): AppResult<Unit> = withContext(ioDispatcher) {
        val currentLoaded = _uiState.value.loadedBitmap ?: return@withContext AppResult.Error(DomainError.UnknownError("No image loaded"))
        val enhanced = ImageProcessingUtils.applyEnhancement(currentLoaded, mode)

        _uiState.update {
            it.copy(
                displayedBitmap = enhanced,
                enhanceMode = mode
            )
        }

        processCurrentBitmap()
    }

    override suspend fun reprocess(
        context: Context?,
        script: OcrScript?,
        enhanceMode: ImageEnhanceMode?,
        rotateDegrees: Float
    ): AppResult<Unit> = withContext(ioDispatcher) {
        var baseBitmap = _uiState.value.loadedBitmap

        if (baseBitmap == null && context != null && _uiState.value.imageUri != null) {
            baseBitmap = ImageProcessingUtils.loadBitmapSafely(context, _uiState.value.imageUri!!)
        }

        if (baseBitmap == null) {
            return@withContext AppResult.Error(DomainError.UnknownError("No image available to process"))
        }

        if (rotateDegrees != 0f) {
            baseBitmap = ImageProcessingUtils.rotateBitmap(baseBitmap, rotateDegrees)
        }

        val targetEnhance = enhanceMode ?: _uiState.value.enhanceMode
        val enhancedBitmap = ImageProcessingUtils.applyEnhancement(baseBitmap, targetEnhance)
        val targetScript = script ?: _uiState.value.script

        _uiState.update {
            it.copy(
                loadedBitmap = baseBitmap,
                displayedBitmap = enhancedBitmap,
                enhanceMode = targetEnhance,
                script = targetScript,
                rotationDegrees = if (rotateDegrees != 0f) (it.rotationDegrees + rotateDegrees.toInt()) % 360 else it.rotationDegrees
            )
        }

        processCurrentBitmap()
    }

    override suspend fun changeLanguages(
        sourceLanguage: Language,
        targetLanguage: Language
    ): AppResult<Unit> = withContext(ioDispatcher) {
        val detection = _uiState.value.detectionResult
        val mappedScript = OcrScript.fromLanguageCode(sourceLanguage.code)

        _uiState.update {
            it.copy(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                script = mappedScript
            )
        }

        if (detection != null && !detection.isEmpty) {
            // Already have OCR result, translate directly without re-running OCR
            translateDetectedBlocks(detection, sourceLanguage, targetLanguage)
        } else if (_uiState.value.displayedBitmap != null) {
            processCurrentBitmap()
        } else {
            AppResult.Success(Unit)
        }
    }

    override fun selectBlock(block: TranslatedBlock?) {
        _uiState.update { it.copy(selectedBlock = block) }
    }

    override fun setOverlayMode(mode: OverlayMode) {
        _uiState.update { it.copy(overlayMode = mode) }
    }

    override fun updateTransform(scale: Float, panX: Float, panY: Float) {
        _uiState.update {
            it.copy(
                zoomScale = scale.coerceIn(1.0f, 6.0f),
                panOffsetX = panX,
                panOffsetY = panY
            )
        }
    }

    override fun resetTransform() {
        _uiState.update {
            it.copy(
                zoomScale = 1.0f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
    }

    override fun clear() {
        _uiState.value = ImageTranslationUiState()
    }

    /**
     * Executes the OCR and translation pipeline on the current displayed bitmap.
     */
    private suspend fun processCurrentBitmap(): AppResult<Unit> = withContext(ioDispatcher) {
        val bitmap = _uiState.value.displayedBitmap ?: return@withContext AppResult.Error(DomainError.UnknownError("Bitmap is null"))
        val script = _uiState.value.script
        val sourceLanguage = _uiState.value.sourceLanguage
        val targetLanguage = _uiState.value.targetLanguage

        _uiState.update {
            it.copy(
                status = ImageOcrStatus.RUNNING_OCR,
                errorMessage = null,
                selectedBlock = null
            )
        }

        val startTime = System.currentTimeMillis()

        // 1. Perform OCR
        ocrRepository.setScript(script)
        val ocrResult = ocrRepository.processBitmap(bitmap)

        when (ocrResult) {
            is AppResult.Error -> {
                Log.e(tag, "OCR processing failed: ${ocrResult.error}")
                _uiState.update {
                    it.copy(
                        status = ImageOcrStatus.ERROR,
                        errorMessage = "Text recognition failed: ${ocrResult.error.message}. Try adjusting rotation or script filter."
                    )
                }
                return@withContext AppResult.Error(ocrResult.error)
            }
            is AppResult.Loading -> return@withContext AppResult.Loading
            is AppResult.Success -> {
                val detection = ocrResult.data
                if (detection.isEmpty || detection.blocks.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            status = ImageOcrStatus.NO_TEXT_FOUND,
                            detectionResult = detection,
                            translatedBlocks = emptyList(),
                            processingTimeMs = System.currentTimeMillis() - startTime
                        )
                    }
                    return@withContext AppResult.Success(Unit)
                }

                _uiState.update {
                    it.copy(
                        detectionResult = detection,
                        status = ImageOcrStatus.TRANSLATING
                    )
                }

                // 2. Translate detected blocks
                translateDetectedBlocks(detection, sourceLanguage, targetLanguage)
            }
        }
    }

    /**
     * Performs multi-language identification and parallel block translation.
     */
    private suspend fun translateDetectedBlocks(
        detection: TextDetectionResult,
        sourceLanguage: Language,
        targetLanguage: Language
    ): AppResult<Unit> = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()

        // 1. Determine main detected language if source is AUTO_DETECT
        var mainDetectedLanguage: String? = null
        if (sourceLanguage.isAuto && languageIdentifier != null && detection.fullText.isNotBlank()) {
            when (val langRes = languageIdentifier.identifyLanguage(detection.fullText)) {
                is AppResult.Success -> {
                    if (langRes.data != "und") {
                        mainDetectedLanguage = langRes.data
                    }
                }
                else -> { /* ignore */ }
            }
        }

        // 2. Translate all blocks concurrently
        val translatedBlocks = coroutineScope {
            detection.blocks.map { block ->
                async {
                    translateSingleBlock(block, sourceLanguage, targetLanguage, mainDetectedLanguage)
                }
            }.awaitAll()
        }

        val totalProcessingTime = System.currentTimeMillis() - startTime

        _uiState.update {
            it.copy(
                status = ImageOcrStatus.SUCCESS,
                translatedBlocks = translatedBlocks,
                detectedMainLanguage = mainDetectedLanguage,
                processingTimeMs = totalProcessingTime
            )
        }

        // 3. Record in translation history
        try {
            if (translatedBlocks.isNotEmpty() && historyRepository != null) {
                val fullOriginal = detection.fullText.take(500)
                val fullTranslated = translatedBlocks.joinToString("\n") { it.translatedText }.take(500)
                val resolvedSourceLang = mainDetectedLanguage ?: sourceLanguage.code

                historyRepository.insertHistoryItem(
                    TranslationHistoryItem(
                        sourceText = fullOriginal,
                        translatedText = fullTranslated,
                        sourceLanguage = resolvedSourceLang,
                        targetLanguage = targetLanguage.code,
                        translationType = com.example.modules.history.model.TranslationType.IMAGE
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to record translation history", e)
        }

        AppResult.Success(Unit)
    }

    private suspend fun translateSingleBlock(
        block: RecognizedBlock,
        sourceLanguage: Language,
        targetLanguage: Language,
        mainDetectedLanguage: String?
    ): TranslatedBlock {
        val rawText = block.text.trim()
        val boundingBox = block.boundingBox ?: com.example.modules.ocr.model.BoundingBox(0f, 0f, 0f, 0f)

        // Determine specific language code for this block
        var blockLanguageCode = if (!sourceLanguage.isAuto) {
            sourceLanguage.code
        } else {
            block.detectedLanguage ?: mainDetectedLanguage ?: "en"
        }

        if (blockLanguageCode == "und") {
            blockLanguageCode = mainDetectedLanguage ?: "en"
        }

        if (rawText.isBlank()) {
            return TranslatedBlock(
                id = "${block.hashCode()}",
                originalText = block.text,
                translatedText = "",
                sourceLanguageCode = blockLanguageCode,
                targetLanguageCode = targetLanguage.code,
                boundingBox = boundingBox,
                lines = block.lines.map {
                    TranslatedLine(
                        originalText = it.text,
                        translatedText = "",
                        boundingBox = it.boundingBox
                    )
                }
            )
        }

        val cacheKey = "${blockLanguageCode}_${targetLanguage.code}_${rawText.hashCode()}"
        val cached = translationMemoryCache[cacheKey]

        if (cached != null) {
            return TranslatedBlock(
                id = "${rawText.hashCode()}_${boundingBox.left.toInt()}",
                originalText = block.text,
                translatedText = cached,
                sourceLanguageCode = blockLanguageCode,
                targetLanguageCode = targetLanguage.code,
                boundingBox = boundingBox,
                lines = block.lines.map {
                    TranslatedLine(
                        originalText = it.text,
                        translatedText = cached,
                        boundingBox = it.boundingBox
                    )
                },
                detectedLanguageCode = blockLanguageCode,
                confidence = block.lines.firstOrNull()?.confidence
            )
        }

        val request = TranslationRequest(
            sourceText = rawText,
            sourceLanguageCode = blockLanguageCode,
            targetLanguageCode = targetLanguage.code
        )

        return when (val res = translationRepository.translate(request)) {
            is AppResult.Success -> {
                val translated = res.data.translatedText
                translationMemoryCache[cacheKey] = translated
                TranslatedBlock(
                    id = "${rawText.hashCode()}_${boundingBox.left.toInt()}",
                    originalText = block.text,
                    translatedText = translated,
                    sourceLanguageCode = blockLanguageCode,
                    targetLanguageCode = targetLanguage.code,
                    boundingBox = boundingBox,
                    lines = block.lines.map {
                        TranslatedLine(
                            originalText = it.text,
                            translatedText = translated,
                            boundingBox = it.boundingBox
                        )
                    },
                    detectedLanguageCode = res.data.sourceLanguageCode.ifBlank { blockLanguageCode },
                    confidence = block.lines.firstOrNull()?.confidence
                )
            }
            is AppResult.Error -> {
                Log.w(tag, "Translation failed for block: $rawText, error: ${res.error}")
                TranslatedBlock(
                    id = "${rawText.hashCode()}_${boundingBox.left.toInt()}",
                    originalText = block.text,
                    translatedText = block.text, // Fallback to original text
                    sourceLanguageCode = blockLanguageCode,
                    targetLanguageCode = targetLanguage.code,
                    boundingBox = boundingBox,
                    lines = block.lines.map {
                        TranslatedLine(
                            originalText = it.text,
                            translatedText = it.text,
                            boundingBox = it.boundingBox
                        )
                    },
                    detectedLanguageCode = blockLanguageCode,
                    confidence = block.lines.firstOrNull()?.confidence,
                    hasError = true
                )
            }
            is AppResult.Loading -> {
                TranslatedBlock(
                    id = "${rawText.hashCode()}_${boundingBox.left.toInt()}",
                    originalText = block.text,
                    translatedText = "",
                    sourceLanguageCode = blockLanguageCode,
                    targetLanguageCode = targetLanguage.code,
                    boundingBox = boundingBox,
                    isTranslating = true
                )
            }
        }
    }
}
