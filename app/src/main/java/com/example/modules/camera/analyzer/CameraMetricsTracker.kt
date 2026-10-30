package com.example.modules.camera.analyzer

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object CameraMetricsTracker {
    private val ocrLatencies = mutableListOf<Long>()
    private val langIdLatencies = mutableListOf<Long>()
    private val translationLatencies = mutableListOf<Long>()
    private val renderLatencies = mutableListOf<Long>()
    private val totalPipelineLatencies = mutableListOf<Long>()

    private val framesProcessedCount = AtomicInteger(0)
    private val framesDroppedCount = AtomicInteger(0)
    private val translationRequestsCount = AtomicInteger(0)
    private val cacheHitsCount = AtomicInteger(0)
    private val duplicateRequestsCount = AtomicInteger(0)

    var modelPrepTimeMs: Long = 45L // Pre-downloaded model check time
    var isModelAlreadyInstalled: Boolean = true

    @Synchronized
    fun recordEvent(
        ocrLatency: Long,
        langIdLatency: Long,
        translationLatency: Long,
        renderLatency: Long,
        totalLatency: Long,
        isCacheHit: Boolean,
        isDuplicate: Boolean
    ) {
        if (ocrLatency > 0) ocrLatencies.add(ocrLatency)
        if (langIdLatency > 0) langIdLatencies.add(langIdLatency)
        if (translationLatency > 0) translationLatencies.add(translationLatency)
        if (renderLatency > 0) renderLatencies.add(renderLatency)
        if (totalLatency > 0) totalPipelineLatencies.add(totalLatency)

        framesProcessedCount.incrementAndGet()
        if (isCacheHit) {
            cacheHitsCount.incrementAndGet()
        } else {
            translationRequestsCount.incrementAndGet()
        }
        if (isDuplicate) {
            duplicateRequestsCount.incrementAndGet()
        }
    }

    @Synchronized
    fun recordDroppedFrame() {
        framesDroppedCount.incrementAndGet()
    }

    @Synchronized
    fun reset() {
        ocrLatencies.clear()
        langIdLatencies.clear()
        translationLatencies.clear()
        renderLatencies.clear()
        totalPipelineLatencies.clear()
        framesProcessedCount.set(0)
        framesDroppedCount.set(0)
        translationRequestsCount.set(0)
        cacheHitsCount.set(0)
        duplicateRequestsCount.set(0)
    }

    val avgOcrLatency: Double
        get() = if (ocrLatencies.isEmpty()) 0.0 else ocrLatencies.average()

    val avgLangIdLatency: Double
        get() = if (langIdLatencies.isEmpty()) 0.0 else langIdLatencies.average()

    val avgTranslationLatency: Double
        get() = if (translationLatencies.isEmpty()) 0.0 else translationLatencies.average()

    val avgRenderLatency: Double
        get() = if (renderLatencies.isEmpty()) 0.0 else renderLatencies.average()

    val avgTotalLatency: Double
        get() = if (totalPipelineLatencies.isEmpty()) 0.0 else totalPipelineLatencies.average()

    val framesProcessed: Int get() = framesProcessedCount.get()
    val framesDropped: Int get() = framesDroppedCount.get()
    val translationRequests: Int get() = translationRequestsCount.get()
    val cacheHits: Int get() = cacheHitsCount.get()
    val duplicateRequests: Int get() = duplicateRequestsCount.get()

    /**
     * Identifies the primary bottleneck stage based on average latencies.
     */
    fun identifyBottleneck(): String {
        val ocr = avgOcrLatency
        val lang = avgLangIdLatency
        val trans = avgTranslationLatency
        val render = avgRenderLatency

        val max = maxOf(ocr, lang, trans, render)
        return when (max) {
            trans -> "ML Kit Translation (Avg: ${trans.toInt()}ms) - On-device neural inference and model lookup."
            ocr -> "ML Kit OCR (Avg: ${ocr.toInt()}ms) - Image text detection pass on camera frames."
            lang -> "Language Identification (Avg: ${lang.toInt()}ms)"
            render -> "Overlay Rendering (Avg: ${render.toInt()}ms)"
            else -> "None / Negligible"
        }
    }
}
