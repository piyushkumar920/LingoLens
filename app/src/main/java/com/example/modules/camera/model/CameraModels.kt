package com.example.modules.camera.model

import android.graphics.Bitmap

/**
 * Supported camera lens facing orientations.
 */
enum class CameraFacing {
    BACK,
    FRONT
}

/**
 * Camera hardware capabilities determined after binding.
 */
data class CameraCapabilities(
    val hasFlash: Boolean = false,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 5.0f,
    val isInitialized: Boolean = false
)

/**
 * UI State for the Camera viewfinder and capture system.
 */
data class CameraUiState(
    val facing: CameraFacing = CameraFacing.BACK,
    val isTorchOn: Boolean = false,
    val hasFlashUnit: Boolean = true,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 5.0f,
    val isFrozen: Boolean = false,
    val frozenBitmap: Bitmap? = null,
    val isCapturing: Boolean = false,
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null,
    val autoOcrEnabled: Boolean = true
)

/**
 * Coordinates and visibility state for tap-to-focus visual feedback.
 */
data class TapFocusState(
    val x: Float = 0f,
    val y: Float = 0f,
    val isVisible: Boolean = false,
    val timestamp: Long = 0L
)
