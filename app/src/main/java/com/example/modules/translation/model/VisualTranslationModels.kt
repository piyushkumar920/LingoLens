package com.example.modules.translation.model

import com.example.modules.language.model.Language
import com.example.modules.ocr.model.BoundingBox
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.RecognizedLine

/**
 * Display mode for visual translation overlay.
 */
enum class OverlayMode(val displayName: String) {
    TRANSLATED("Translated"),   // Renders translated text badge directly over original text
    ORIGINAL("Original OCR"),     // Renders original OCR detected bounding boxes
    SIDE_BY_SIDE("Split View"), // Renders both original and translation
    OFF("Overlay Off")           // Hides all overlay annotations
}

/**
 * A detected text line along with its translation.
 */
data class TranslatedLine(
    val originalText: String,
    val translatedText: String? = null,
    val boundingBox: BoundingBox? = null,
    val confidence: Float? = null
)

/**
 * A complete detected text block along with its on-device translation and spatial metadata.
 */
data class TranslatedBlock(
    val id: String,
    val originalText: String,
    val translatedText: String,
    val sourceLanguageCode: String,
    val targetLanguageCode: String,
    val boundingBox: BoundingBox,
    val lines: List<TranslatedLine> = emptyList(),
    val angle: Float = 0f,
    val confidence: Float? = null,
    val isTranslating: Boolean = false,
    val hasError: Boolean = false,
    val detectedLanguageCode: String? = null
) {
    val displayText: String
        get() = if (translatedText.isNotBlank()) translatedText else originalText
}

/**
 * State representing visual translation on a frame or image.
 */
data class VisualTranslationState(
    val blocks: List<TranslatedBlock> = emptyList(),
    val sourceLanguage: Language = Language.AUTO_DETECT,
    val targetLanguage: Language = Language.ENGLISH,
    val overlayMode: OverlayMode = OverlayMode.TRANSLATED,
    val isTranslating: Boolean = false,
    val isFrozen: Boolean = false,
    val selectedBlock: TranslatedBlock? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
