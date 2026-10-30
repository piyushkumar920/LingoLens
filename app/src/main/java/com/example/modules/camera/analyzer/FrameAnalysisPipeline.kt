package com.example.modules.camera.analyzer

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consumer callback interface prepared for the ML Kit OCR pipeline.
 */
fun interface FrameConsumer {
    fun onFrame(imageProxy: ImageProxy)
}

/**
 * Production-grade CameraX ImageAnalysis pipeline.
 *
 * Optimizations:
 * 1. Rate-limiting: Analyzes at most one frame every [minIntervalMs] (default 250ms).
 * 2. Concurrency Lock: Prevents overlapping frame processing with [isProcessing] AtomicBoolean.
 * 3. Memory Safety: Guarantees [ImageProxy.close] is executed in a finally block if the consumer does not claim ownership.
 * 4. Zero blocking: Frame drops occur gracefully when busy.
 */
class FrameAnalysisPipeline(
    private val minIntervalMs: Long = 250L
) : ImageAnalysis.Analyzer {

    private val tag = "FrameAnalysisPipeline"
    private val isProcessing = AtomicBoolean(false)
    private var lastAnalyzedTimestamp = 0L

    @Volatile
    private var isEnabled = true

    @Volatile
    private var frameConsumer: FrameConsumer? = null

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    fun setFrameConsumer(consumer: FrameConsumer?) {
        this.frameConsumer = consumer
    }

    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()

        // Fast skip if disabled, consumer is absent, or within rate-limit window
        if (!isEnabled || frameConsumer == null || (currentTime - lastAnalyzedTimestamp < minIntervalMs)) {
            image.close()
            return
        }

        // Prevent multiple simultaneous frame processing
        if (!isProcessing.compareAndSet(false, true)) {
            image.close()
            return
        }

        lastAnalyzedTimestamp = currentTime

        try {
            frameConsumer?.onFrame(image)
        } catch (e: Exception) {
            Log.e(tag, "Error during frame analysis", e)
        } finally {
            // Note: If the consumer takes ownership of image (as ML Kit sometimes does with InputImage),
            // it can close it, but to prevent leaks in standard usage we ensure image is safely closed.
            try {
                image.close()
            } catch (e: Exception) {
                // Ignore if already closed by consumer
            }
            isProcessing.set(false)
        }
    }

    fun reset() {
        isProcessing.set(false)
        lastAnalyzedTimestamp = 0L
    }
}
