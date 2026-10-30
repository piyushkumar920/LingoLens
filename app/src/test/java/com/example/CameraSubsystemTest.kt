package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.permission.PermissionManager
import com.example.modules.camera.analyzer.FrameAnalysisPipeline
import com.example.modules.camera.model.CameraFacing
import com.example.modules.camera.repository.DefaultCameraRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CameraSubsystemTest {

    @Test
    fun `verify camera repository default states and lens switching`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraRepo = DefaultCameraRepository(context)

        val initialState = cameraRepo.cameraUiState.first()
        assertEquals(CameraFacing.BACK, initialState.facing)
        assertFalse(initialState.isTorchOn)
        assertFalse(initialState.isFrozen)

        // Test camera switch
        cameraRepo.switchCamera()
        val switchedState = cameraRepo.cameraUiState.first()
        assertEquals(CameraFacing.FRONT, switchedState.facing)

        // Switch back
        cameraRepo.switchCamera()
        val revertedState = cameraRepo.cameraUiState.first()
        assertEquals(CameraFacing.BACK, revertedState.facing)
    }

    @Test
    fun `verify freeze and resume operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraRepo = DefaultCameraRepository(context)

        cameraRepo.freezeFrame(null)
        val frozenState = cameraRepo.cameraUiState.first()
        assertTrue(frozenState.isFrozen)

        cameraRepo.resumeLiveCamera()
        val resumedState = cameraRepo.cameraUiState.first()
        assertFalse(resumedState.isFrozen)
    }

    @Test
    fun `verify frame analysis pipeline configuration and state`() {
        val pipeline = FrameAnalysisPipeline(minIntervalMs = 200L)
        assertTrue(pipeline.isEnabled())

        pipeline.setEnabled(false)
        assertFalse(pipeline.isEnabled())

        pipeline.setEnabled(true)
        assertTrue(pipeline.isEnabled())
    }

    @Test
    fun `verify permission manager camera checks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(PermissionManager.CAMERA_PERMISSION)
        assertEquals("android.permission.CAMERA", PermissionManager.CAMERA_PERMISSION)
    }
}
