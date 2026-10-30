package com.example

import com.example.modules.diagnostics.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationTestLabEngineTest {

    @Test
    fun testCapabilityStatusColorsAndLabels() {
        assertEquals("✓ READY", CapabilityStatus.READY.label)
        assertEquals("⚠ WARNING", CapabilityStatus.WARNING.label)
        assertEquals("✕ ERROR", CapabilityStatus.ERROR.label)
        assertEquals("⟳ TESTING", CapabilityStatus.TESTING.label)
        assertNotNull(CapabilityStatus.READY.color)
    }

    @Test
    fun testTestStatusLabelsAndColors() {
        assertEquals("✓ PASS", TestStatus.PASS.label)
        assertEquals("✕ FAIL", TestStatus.FAIL.label)
        assertEquals("⚠ WARNING", TestStatus.WARNING.label)
        assertEquals("⟳ RUNNING", TestStatus.RUNNING.label)
        assertEquals("• PENDING", TestStatus.PENDING.label)
        assertNotNull(TestStatus.PASS.color)
    }

    @Test
    fun testDiagnosticErrorLogModel() {
        val log = DiagnosticErrorLog(
            id = "err_1",
            timestamp = System.currentTimeMillis(),
            feature = "CAMERA TRANSLATION",
            requestId = 14,
            sourceLanguage = "en",
            targetLanguage = "hi",
            operation = "OCR → Translation",
            latencyMs = 45L,
            exceptionType = "TranslatorUnavailableException",
            readableErrorMessage = "Translator unavailable",
            recoveryAction = "Translator recreated"
        )
        assertEquals("CAMERA TRANSLATION", log.feature)
        assertEquals(14, log.requestId)
        assertEquals("Translator recreated", log.recoveryAction)
    }

    @Test
    fun testPerformanceMetricItemModel() {
        val metric = PerformanceMetricItem(
            name = "ML Kit Translation Latency",
            currentMs = 38L,
            avgMs = 42.5,
            minMs = 20L,
            maxMs = 85L,
            sampleCount = 10,
            unit = "ms"
        )
        assertEquals("ML Kit Translation Latency", metric.name)
        assertEquals(38L, metric.currentMs)
        assertEquals(42.5, metric.avgMs, 0.01)
        assertEquals(10, metric.sampleCount)
    }

    @Test
    fun testTestLabSummaryModel() {
        val summary = TestLabSummary(
            totalTests = 10,
            passed = 9,
            failed = 0,
            warnings = 1,
            totalDurationMs = 1250L,
            isCompleted = true
        )
        assertEquals(10, summary.totalTests)
        assertEquals(9, summary.passed)
        assertEquals(0, summary.failed)
        assertEquals(1, summary.warnings)
        assertTrue(summary.isCompleted)
    }
}
