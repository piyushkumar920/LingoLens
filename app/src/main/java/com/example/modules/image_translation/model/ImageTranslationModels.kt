package com.example.modules.image_translation.model

import android.graphics.Bitmap
import android.net.Uri
import com.example.core.image.ImageEnhanceMode
import com.example.modules.language.model.Language
import com.example.modules.ocr.model.OcrScript
import com.example.modules.ocr.model.TextDetectionResult
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.model.TranslatedBlock

/**
 * Processing state for Image-based OCR and Translation.
 */
enum class ImageOcrStatus {
    IDLE,
    LOADING_IMAGE,
    RUNNING_OCR,
    TRANSLATING,
    SUCCESS,
    NO_TEXT_FOUND,
    ERROR
}

/**
 * UI State for the Image Translation Studio and viewer.
 */
data class ImageTranslationUiState(
    val imageUri: Uri? = null,
    val loadedBitmap: Bitmap? = null,
    val displayedBitmap: Bitmap? = null,
    val rotationDegrees: Int = 0,
    val enhanceMode: ImageEnhanceMode = ImageEnhanceMode.ORIGINAL,
    val sourceLanguage: Language = Language.AUTO_DETECT,
    val targetLanguage: Language = Language("es", "Spanish", "Español", "🇪🇸"),
    val script: OcrScript = OcrScript.LATIN,
    val status: ImageOcrStatus = ImageOcrStatus.IDLE,
    val detectionResult: TextDetectionResult? = null,
    val translatedBlocks: List<TranslatedBlock> = emptyList(),
    val selectedBlock: TranslatedBlock? = null,
    val overlayMode: OverlayMode = OverlayMode.TRANSLATED,
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val errorMessage: String? = null,
    val detectedMainLanguage: String? = null,
    val processingTimeMs: Long = 0L
) {
    val hasImage: Boolean get() = loadedBitmap != null
    val isBusy: Boolean get() = status == ImageOcrStatus.LOADING_IMAGE || status == ImageOcrStatus.RUNNING_OCR || status == ImageOcrStatus.TRANSLATING

    val originalFullText: String
        get() = detectionResult?.fullText ?: translatedBlocks.joinToString("\n") { it.originalText }

    val translatedFullText: String
        get() = translatedBlocks.joinToString("\n") { it.translatedText.ifBlank { it.originalText } }
}
