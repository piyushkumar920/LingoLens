package com.example

import android.graphics.Rect
import com.example.modules.ocr.model.BoundingBox
import com.example.modules.ocr.model.OcrScript
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.RecognizedLine
import com.example.modules.ocr.model.TextDetectionResult
import com.example.modules.ocr.repository.DefaultOcrRepository
import com.example.ui.components.TransformMatrix
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
class OcrSubsystemTest {

    @Test
    fun `verify OCR script detection from language codes`() {
        assertEquals(OcrScript.CHINESE, OcrScript.fromLanguageCode("zh"))
        assertEquals(OcrScript.CHINESE, OcrScript.fromLanguageCode("zh-CN"))
        assertEquals(OcrScript.JAPANESE, OcrScript.fromLanguageCode("ja"))
        assertEquals(OcrScript.KOREAN, OcrScript.fromLanguageCode("ko"))
        assertEquals(OcrScript.DEVANAGARI, OcrScript.fromLanguageCode("hi"))
        assertEquals(OcrScript.DEVANAGARI, OcrScript.fromLanguageCode("hindi"))
        assertEquals(OcrScript.LATIN, OcrScript.fromLanguageCode("en"))
        assertEquals(OcrScript.LATIN, OcrScript.fromLanguageCode("es"))
        assertEquals(OcrScript.LATIN, OcrScript.fromLanguageCode("fr"))
        assertEquals(OcrScript.LATIN, OcrScript.fromLanguageCode("unknown"))
    }

    @Test
    fun `verify bounding box dimensions and conversion`() {
        val rect = Rect(10, 20, 110, 220)
        val box = BoundingBox.fromRect(rect)
        assertNotNull(box)
        assertEquals(10f, box!!.left)
        assertEquals(20f, box.top)
        assertEquals(110f, box.right)
        assertEquals(220f, box.bottom)
        assertEquals(100f, box.width)
        assertEquals(200f, box.height)
        assertEquals(60f, box.centerX)
        assertEquals(120f, box.centerY)
    }

    @Test
    fun `verify transform matrix coordinate mapping and hit detection`() {
        // Source frame 1000x1000 into canvas 500x500 -> scale 0.5, dx 0, dy 0
        val transform = TransformMatrix(scale = 0.5f, dx = 0f, dy = 0f)
        val originalBox = BoundingBox(100f, 100f, 300f, 300f)
        val mappedBox = transform.mapBox(originalBox)

        assertEquals(50f, mappedBox.left)
        assertEquals(50f, mappedBox.top)
        assertEquals(150f, mappedBox.right)
        assertEquals(150f, mappedBox.bottom)

        assertTrue(transform.contains(mappedBox, 100f, 100f))
        assertFalse(transform.contains(mappedBox, 200f, 200f))
    }

    @Test
    fun `verify text detection result line count and content signature`() {
        val line1 = RecognizedLine("Hello", boundingBox = BoundingBox(0f, 0f, 10f, 10f))
        val line2 = RecognizedLine("World", boundingBox = BoundingBox(0f, 10f, 10f, 20f))
        val block1 = RecognizedBlock(text = "Hello\nWorld", lines = listOf(line1, line2))

        val result = TextDetectionResult(
            fullText = "Hello\nWorld",
            blocks = listOf(block1),
            sourceWidth = 1080,
            sourceHeight = 1920
        )

        assertFalse(result.isEmpty)
        assertEquals(2, result.totalLines)
        assertEquals(1, result.blocks.size)
        assertTrue(result.contentSignature != 0)
    }

    @Test
    fun `verify default ocr repository script management`() = runBlocking {
        val ocrRepo = DefaultOcrRepository()
        assertEquals(OcrScript.LATIN, ocrRepo.currentScript.first())

        ocrRepo.setScript(OcrScript.JAPANESE)
        assertEquals(OcrScript.JAPANESE, ocrRepo.currentScript.first())

        ocrRepo.setScript(OcrScript.DEVANAGARI)
        assertEquals(OcrScript.DEVANAGARI, ocrRepo.currentScript.first())

        ocrRepo.clearLiveDetection()
        assertTrue(ocrRepo.latestLiveDetection.first().isEmpty)

        ocrRepo.close()
    }
}
