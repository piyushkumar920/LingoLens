package com.example.modules.ocr.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.example.core.result.AppResult
import com.example.modules.camera.analyzer.FrameConsumer
import com.example.modules.ocr.model.OcrResult
import com.example.modules.ocr.model.OcrScript
import com.example.modules.ocr.model.TextDetectionResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Optical Character Recognition abstraction powered by Google ML Kit.
 */
interface OcrRepository {
    val currentScript: StateFlow<OcrScript>
    val latestLiveDetection: StateFlow<TextDetectionResult>

    fun setScript(script: OcrScript)
    suspend fun processFrame(imageProxy: ImageProxy): AppResult<TextDetectionResult>
    suspend fun processBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): AppResult<TextDetectionResult>
    suspend fun processImageUri(context: Context, uri: Uri): AppResult<TextDetectionResult>
    fun createFrameConsumer(): FrameConsumer
    fun clearLiveDetection()
    fun close()
}
