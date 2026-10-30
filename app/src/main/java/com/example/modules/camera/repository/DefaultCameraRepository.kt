package com.example.modules.camera.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.modules.camera.analyzer.FrameConsumer
import com.example.modules.camera.manager.CameraXManager
import com.example.modules.camera.model.CameraFacing
import com.example.modules.camera.model.CameraUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Production implementation of [CameraRepository] delegating to [CameraXManager].
 */
class DefaultCameraRepository(
    private val context: Context
) : CameraRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cameraManager by lazy {
        CameraXManager(context.applicationContext, repositoryScope)
    }

    override val cameraUiState: StateFlow<CameraUiState>
        get() = cameraManager.uiState

    override val cameraSettings: StateFlow<CameraSettings> =
        cameraManager.uiState.map { state ->
            CameraSettings(
                isFlashEnabled = state.isTorchOn,
                isFrontCamera = state.facing == CameraFacing.FRONT,
                autoOcrEnabled = state.autoOcrEnabled
            )
        }.stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = CameraSettings()
        )

    override fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraManager.bindToLifecycle(lifecycleOwner, previewView)
    }

    override fun switchCamera() {
        cameraManager.switchCamera()
    }

    override fun toggleTorch() {
        cameraManager.toggleTorch()
    }

    override fun setZoomRatio(ratio: Float) {
        cameraManager.setZoomRatio(ratio)
    }

    override fun setLinearZoom(linear: Float) {
        cameraManager.setLinearZoom(linear)
    }

    override fun focusOnPoint(x: Float, y: Float, previewView: PreviewView) {
        cameraManager.focusOnPoint(x, y, previewView)
    }

    override fun freezeFrame(bitmap: Bitmap?) {
        cameraManager.freezeFrame(bitmap)
    }

    override fun resumeLiveCamera() {
        cameraManager.resumeLiveCamera()
    }

    override fun setAutoOcr(enabled: Boolean) {
        cameraManager.setAutoOcr(enabled)
    }

    override fun setFrameConsumer(consumer: FrameConsumer?) {
        cameraManager.setFrameConsumer(consumer)
    }

    override suspend fun captureStillImage(): AppResult<Bitmap> =
        suspendCancellableCoroutine { continuation ->
            cameraManager.captureImage(
                onSuccess = { bitmap ->
                    if (continuation.isActive) {
                        continuation.resume(AppResult.Success(bitmap))
                    }
                },
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resume(
                            AppResult.Error(
                                DomainError.UnknownError(error.localizedMessage ?: "Camera capture failed")
                            )
                        )
                    }
                }
            )
        }

    override fun unbind() {
        cameraManager.unbind()
    }
}
