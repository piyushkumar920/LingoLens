package com.example.modules.camera.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.example.core.result.AppResult
import com.example.modules.camera.analyzer.FrameConsumer
import com.example.modules.camera.manager.CameraXManager
import com.example.modules.camera.model.CameraFacing
import com.example.modules.camera.model.CameraUiState
import kotlinx.coroutines.flow.StateFlow

data class CameraSettings(
    val isFlashEnabled: Boolean = false,
    val isFrontCamera: Boolean = false,
    val autoOcrEnabled: Boolean = true
)

/**
 * Clean architectural abstraction for the camera subsystem.
 */
interface CameraRepository {
    val cameraUiState: StateFlow<CameraUiState>
    val cameraSettings: StateFlow<CameraSettings>

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun switchCamera()
    fun toggleTorch()
    fun setZoomRatio(ratio: Float)
    fun setLinearZoom(linear: Float)
    fun focusOnPoint(x: Float, y: Float, previewView: PreviewView)
    fun freezeFrame(bitmap: Bitmap?)
    fun resumeLiveCamera()
    fun setAutoOcr(enabled: Boolean)
    fun setFrameConsumer(consumer: FrameConsumer?)
    suspend fun captureStillImage(): AppResult<Bitmap>
    fun unbind()
}
