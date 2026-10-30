package com.example.modules.ocr.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.modules.camera.analyzer.FrameConsumer
import com.example.modules.ocr.model.BoundingBox
import com.example.modules.ocr.model.OcrScript
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.RecognizedElement
import com.example.modules.ocr.model.RecognizedLine
import com.example.modules.ocr.model.TextDetectionResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Production-ready ML Kit Text Recognition repository.
 */
class DefaultOcrRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OcrRepository {

    private val tag = "DefaultOcrRepository"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentScript = MutableStateFlow(OcrScript.LATIN)
    override val currentScript: StateFlow<OcrScript> = _currentScript.asStateFlow()

    private val _latestLiveDetection = MutableStateFlow(TextDetectionResult())
    override val latestLiveDetection: StateFlow<TextDetectionResult> = _latestLiveDetection.asStateFlow()

    private val recognizers = ConcurrentHashMap<OcrScript, TextRecognizer>()
    private var lastSignature: Int = 0

    private fun getRecognizer(script: OcrScript): TextRecognizer {
        return recognizers.computeIfAbsent(script) {
            when (it) {
                OcrScript.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                OcrScript.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                OcrScript.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                OcrScript.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                OcrScript.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            }
        }
    }

    override fun setScript(script: OcrScript) {
        if (_currentScript.value != script) {
            _currentScript.value = script
            Log.d(tag, "Switched ML Kit OCR Script to: ${script.displayName}")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override suspend fun processFrame(imageProxy: ImageProxy): AppResult<TextDetectionResult> = withContext(ioDispatcher) {
        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()
            val width = if (rotationDegrees == 90 || rotationDegrees == 270) bitmap.height else bitmap.width
            val height = if (rotationDegrees == 90 || rotationDegrees == 270) bitmap.width else bitmap.height

            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            val script = _currentScript.value

            executeRecognition(inputImage, script, width, height, rotationDegrees)
        } catch (e: Exception) {
            Log.e(tag, "Failed to process frame", e)
            AppResult.Error(DomainError.OcrProcessingError("Frame processing failed: ${e.localizedMessage}", e))
        } finally {
            try {
                imageProxy.close()
            } catch (e: Exception) {}
        }
    }

    override suspend fun processBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int
    ): AppResult<TextDetectionResult> = withContext(ioDispatcher) {
        val width = if (rotationDegrees == 90 || rotationDegrees == 270) bitmap.height else bitmap.width
        val height = if (rotationDegrees == 90 || rotationDegrees == 270) bitmap.width else bitmap.height

        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        val script = _currentScript.value

        executeRecognition(inputImage, script, width, height, rotationDegrees)
    }

    override suspend fun processImageUri(
        context: Context,
        uri: Uri
    ): AppResult<TextDetectionResult> = withContext(ioDispatcher) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val script = _currentScript.value
            executeRecognition(inputImage, script, inputImage.width, inputImage.height, inputImage.rotationDegrees)
        } catch (e: Exception) {
            Log.e(tag, "Failed to load image from URI", e)
            AppResult.Error(DomainError.OcrProcessingError("Cannot load image: ${e.localizedMessage}", e))
        }
    }

    private suspend fun executeRecognition(
        inputImage: InputImage,
        script: OcrScript,
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegrees: Int
    ): AppResult<TextDetectionResult> = suspendCancellableCoroutine { continuation ->
        val startTime = System.currentTimeMillis()
        val recognizer = getRecognizer(script)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val processingTime = System.currentTimeMillis() - startTime
                Log.d("VISION_OCR", "OCR duration: ${processingTime}ms (text len: ${visionText.text.length}, blocks: ${visionText.textBlocks.size})")
                val result = parseVisionText(
                    visionText = visionText,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    rotationDegrees = rotationDegrees,
                    script = script,
                    processingTimeMs = processingTime
                )

                if (continuation.isActive) {
                    continuation.resume(AppResult.Success(result))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "ML Kit text recognition failed", exception)
                if (continuation.isActive) {
                    continuation.resume(
                        AppResult.Error(
                            DomainError.OcrProcessingError(
                                exception.localizedMessage ?: "OCR recognition failed",
                                exception
                            )
                        )
                    )
                }
            }
    }

    private fun parseVisionText(
        visionText: Text,
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegrees: Int,
        script: OcrScript,
        processingTimeMs: Long
    ): TextDetectionResult {
        val blocks = visionText.textBlocks.map { block ->
            val lines = block.lines.map { line ->
                val elements = line.elements.map { element ->
                    RecognizedElement(
                        text = element.text,
                        boundingBox = BoundingBox.fromRect(element.boundingBox),
                        confidence = element.confidence,
                        cornerPoints = element.cornerPoints?.map { PointF(it.x.toFloat(), it.y.toFloat()) } ?: emptyList()
                    )
                }

                RecognizedLine(
                    text = line.text,
                    boundingBox = BoundingBox.fromRect(line.boundingBox),
                    confidence = line.confidence,
                    elements = elements,
                    cornerPoints = line.cornerPoints?.map { PointF(it.x.toFloat(), it.y.toFloat()) } ?: emptyList(),
                    angle = line.angle
                )
            }

            RecognizedBlock(
                text = block.text,
                lines = lines,
                boundingBox = BoundingBox.fromRect(block.boundingBox),
                detectedLanguage = block.recognizedLanguage,
                cornerPoints = block.cornerPoints?.map { PointF(it.x.toFloat(), it.y.toFloat()) } ?: emptyList()
            )
        }

        return TextDetectionResult(
            fullText = visionText.text,
            blocks = blocks,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            rotationDegrees = rotationDegrees,
            script = script,
            processingTimeMs = processingTimeMs,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun createFrameConsumer(): FrameConsumer {
        return FrameConsumer { imageProxy ->
            repositoryScope.launch {
                when (val result = processFrame(imageProxy)) {
                    is AppResult.Success -> {
                        val newResult = result.data
                        val newSignature = newResult.contentSignature
                        // Avoid repeated identical results causing unnecessary recompositions
                        if (newSignature != lastSignature || newResult.isEmpty != _latestLiveDetection.value.isEmpty) {
                            lastSignature = newSignature
                            _latestLiveDetection.update { newResult }
                        }
                    }
                    is AppResult.Error -> {
                        Log.w(tag, "Frame analysis returned error: ${result.error}")
                    }
                    is AppResult.Loading -> { /* no-op */ }
                }
            }
        }
    }

    override fun clearLiveDetection() {
        lastSignature = 0
        _latestLiveDetection.value = TextDetectionResult()
    }

    override fun close() {
        recognizers.values.forEach { recognizer ->
            try {
                recognizer.close()
            } catch (e: Exception) {
                Log.e(tag, "Error closing TextRecognizer", e)
            }
        }
        recognizers.clear()
    }
}
