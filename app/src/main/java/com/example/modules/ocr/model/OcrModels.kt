package com.example.modules.ocr.model

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

/**
 * Supported ML Kit Text Recognition Scripts.
 */
enum class OcrScript(val displayName: String, val code: String) {
    LATIN("Latin (Default)", "latin"),
    CHINESE("Chinese", "zh"),
    DEVANAGARI("Devanagari (Hindi/Sanskrit)", "hi"),
    JAPANESE("Japanese", "ja"),
    KOREAN("Korean", "ko");

    companion object {
        fun fromLanguageCode(langCode: String): OcrScript {
            return when (langCode.lowercase()) {
                "zh", "zh-cn", "zh-tw", "chinese" -> CHINESE
                "ja", "japanese" -> JAPANESE
                "ko", "korean" -> KOREAN
                "hi", "mr", "ne", "sa", "devanagari", "hindi" -> DEVANAGARI
                else -> LATIN
            }
        }
    }
}

/**
 * Normalized or pixel-space bounding box.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f

    fun toRectF(): RectF = RectF(left, top, right, bottom)
    fun toRect(): Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

    companion object {
        fun fromRect(rect: Rect?): BoundingBox? {
            if (rect == null) return null
            return BoundingBox(
                left = rect.left.toFloat(),
                top = rect.top.toFloat(),
                right = rect.right.toFloat(),
                bottom = rect.bottom.toFloat()
            )
        }
    }
}

/**
 * Fine-grained detected text element (word/symbol).
 */
data class RecognizedElement(
    val text: String,
    val boundingBox: BoundingBox? = null,
    val confidence: Float? = null,
    val cornerPoints: List<PointF> = emptyList()
)

/**
 * Detected single line of text.
 */
data class RecognizedLine(
    val text: String,
    val boundingBox: BoundingBox? = null,
    val confidence: Float? = null,
    val elements: List<RecognizedElement> = emptyList(),
    val cornerPoints: List<PointF> = emptyList(),
    val angle: Float = 0f
)

/**
 * Structural paragraph or block of detected text.
 */
data class RecognizedBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val lines: List<RecognizedLine> = emptyList(),
    val boundingBox: BoundingBox? = null,
    val detectedLanguage: String? = null,
    val cornerPoints: List<PointF> = emptyList()
)

/**
 * Complete outcome of an OCR inference pass.
 */
data class TextDetectionResult(
    val fullText: String = "",
    val blocks: List<RecognizedBlock> = emptyList(),
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val rotationDegrees: Int = 0,
    val script: OcrScript = OcrScript.LATIN,
    val processingTimeMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean get() = fullText.isBlank() && blocks.isEmpty()
    val totalLines: Int get() = blocks.sumOf { it.lines.size }

    // Deduplication signature to prevent redundant Compose recompositions
    val contentSignature: Int
        get() = (fullText.hashCode() * 31 + blocks.size) * 31 + (blocks.firstOrNull()?.text?.hashCode() ?: 0)
}

/**
 * Unified alias for backward compatibility.
 */
typealias OcrResult = TextDetectionResult
