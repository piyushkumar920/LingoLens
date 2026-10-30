package com.example.modules.camera.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.modules.camera.analyzer.FrameAnalysisPipeline
import com.example.modules.camera.analyzer.FrameConsumer
import com.example.modules.camera.model.CameraFacing
import com.example.modules.camera.model.CameraUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Production-grade CameraX Manager responsible for hardware lifecycle binding,
 * capture pipeline orchestration, and analysis stream maintenance.
 */
class CameraXManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val tag = "CameraXManager"

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    val frameAnalysisPipeline = FrameAnalysisPipeline(minIntervalMs = 250L)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val captureExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    /**
     * Binds CameraX Preview, ImageCapture, and ImageAnalysis to the given [LifecycleOwner]
     * and [PreviewView].
     */
    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onInitialized: (() -> Unit)? = null
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                rebindCamera()
                onInitialized?.invoke()
            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize ProcessCameraProvider", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to access camera: ${e.localizedMessage}")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun rebindCamera() {
        val provider = cameraProvider ?: return
        val lifecycleOwner = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return

        try {
            provider.unbindAll()

            val lensFacing = when (_uiState.value.facing) {
                CameraFacing.BACK -> CameraSelector.LENS_FACING_BACK
                CameraFacing.FRONT -> CameraSelector.LENS_FACING_FRONT
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // 1. Preview UseCase
            preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // 2. ImageCapture UseCase
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // 3. ImageAnalysis UseCase (optimized with Keep Latest strategy)
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor, frameAnalysisPipeline)
                }

            // Bind to lifecycle
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            // Observe hardware capabilities & states
            cameraInfo?.let { info ->
                val hasFlash = info.hasFlashUnit()
                _uiState.update {
                    it.copy(
                        hasFlashUnit = true,
                        errorMessage = null
                    )
                }

                info.zoomState.observe(lifecycleOwner) { zoom ->
                    _uiState.update { current ->
                        val hwMax = zoom.maxZoomRatio
                        val effectiveMax = maxOf(hwMax, 5.0f)
                        val effectiveMin = minOf(zoom.minZoomRatio, 1.0f)
                        current.copy(
                            minZoomRatio = effectiveMin,
                            maxZoomRatio = effectiveMax
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind camera use cases", e)
            _uiState.update {
                it.copy(errorMessage = "Camera error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Switch between rear and front camera lenses.
     */
    fun switchCamera() {
        val nextFacing = if (_uiState.value.facing == CameraFacing.BACK) {
            CameraFacing.FRONT
        } else {
            CameraFacing.BACK
        }
        _uiState.update { it.copy(facing = nextFacing, isTorchOn = false) }
        rebindCamera()
    }

    /**
     * Toggles hardware torch/flash if supported on current lens or updates flashlight state.
     */
    fun toggleTorch() {
        val targetState = !_uiState.value.isTorchOn
        _uiState.update { it.copy(isTorchOn = targetState, hasFlashUnit = true) }
        val info = cameraInfo
        val control = cameraControl

        if (control != null && info != null && info.hasFlashUnit()) {
            try {
                control.enableTorch(targetState)
            } catch (e: Exception) {
                Log.w(tag, "Torch enable failed on hardware", e)
            }
        }
    }

    /**
     * Adjusts the camera zoom ratio directly and synchronizes UI state.
     */
    fun setZoomRatio(ratio: Float) {
        val clamped = ratio.coerceIn(1.0f, 5.0f)
        _uiState.update { it.copy(zoomRatio = clamped) }
        try {
            val hwMax = cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1.0f
            val hwMin = cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f
            if (hwMax > 1.05f) {
                val hwClamped = clamped.coerceIn(hwMin, hwMax)
                cameraControl?.setZoomRatio(hwClamped)
            }
        } catch (e: Exception) {
            Log.w(tag, "CameraControl setZoomRatio failed", e)
        }
    }

    /**
     * Adjusts zoom smoothly via linear zoom factor (0.0 to 1.0).
     */
    fun setLinearZoom(linear: Float) {
        val control = cameraControl ?: return
        control.setLinearZoom(linear.coerceIn(0f, 1f))
    }

    /**
     * Triggers Auto-Focus and Auto-Exposure on a specific touch point in the viewfinder.
     */
    fun focusOnPoint(x: Float, y: Float, previewView: PreviewView) {
        val control = cameraControl ?: return
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            control.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e(tag, "Focus and metering failed", e)
        }
    }

    /**
     * Captures a high quality still photo as a Bitmap with rotation correction.
     */
    fun captureImage(
        onSuccess: (Bitmap) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("ImageCapture is not initialized or camera is closed"))
            return
        }

        _uiState.update { it.copy(isCapturing = true) }

        try {
            capture.takePicture(
                captureExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val rotationDegrees = image.imageInfo.rotationDegrees
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            if (bitmap != null) {
                                val rotatedBitmap = if (rotationDegrees != 0) {
                                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                    Bitmap.createBitmap(
                                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                    )
                                } else {
                                    bitmap
                                }

                                scope.launch(Dispatchers.Main) {
                                    _uiState.update { it.copy(isCapturing = false) }
                                    onSuccess(rotatedBitmap)
                                }
                            } else {
                                scope.launch(Dispatchers.Main) {
                                    _uiState.update { it.copy(isCapturing = false) }
                                    onError(IllegalStateException("Failed to decode captured image bitmap"))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error processing captured ImageProxy", e)
                            scope.launch(Dispatchers.Main) {
                                _uiState.update { it.copy(isCapturing = false) }
                                onError(e)
                            }
                        } finally {
                            try {
                                image.close()
                            } catch (e: Exception) {
                                Log.w(tag, "Error closing ImageProxy", e)
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(tag, "Image capture failed: ${exception.message}", exception)
                        scope.launch(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    isCapturing = false,
                                    errorMessage = "Camera capture failed: ${exception.localizedMessage}"
                                )
                            }
                            onError(exception)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Exception during takePicture", e)
            _uiState.update { it.copy(isCapturing = false, errorMessage = "Capture error: ${e.localizedMessage}") }
            onError(e)
        }
    }

    /**
     * Freezes the current frame with an optional Bitmap representation.
     */
    fun freezeFrame(bitmap: Bitmap?) {
        frameAnalysisPipeline.setEnabled(false)
        _uiState.update {
            it.copy(
                isFrozen = true,
                frozenBitmap = bitmap
            )
        }
    }

    /**
     * Resumes live camera viewfinder and frame analysis stream.
     */
    fun resumeLiveCamera() {
        frameAnalysisPipeline.setEnabled(_uiState.value.autoOcrEnabled)
        _uiState.update {
            it.copy(
                isFrozen = false,
                frozenBitmap = null,
                errorMessage = null
            )
        }
    }

    fun setAutoOcr(enabled: Boolean) {
        _uiState.update { it.copy(autoOcrEnabled = enabled) }
        frameAnalysisPipeline.setEnabled(enabled && !_uiState.value.isFrozen)
    }

    fun setFrameConsumer(consumer: FrameConsumer?) {
        frameAnalysisPipeline.setFrameConsumer(consumer)
    }

    /**
     * Clears errors.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Unbinds camera resources.
     */
    fun unbind() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(tag, "Error unbinding CameraProvider", e)
        }
    }

    /**
     * Shuts down executor services and frees native resources.
     */
    fun shutdown() {
        unbind()
        try {
            analysisExecutor.shutdown()
            captureExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down executors", e)
        }
    }
}
