package com.example.modules.diagnostics.model

import androidx.compose.ui.graphics.Color

enum class CapabilityStatus(val label: String, val badgeText: String) {
    READY("✓ READY", "READY"),
    WARNING("⚠ WARNING", "WARNING"),
    ERROR("✕ ERROR", "ERROR"),
    TESTING("⟳ TESTING", "TESTING");

    val color: Color
        get() = when (this) {
            READY -> Color(0xFF10B981)   // Vibrant emerald green
            WARNING -> Color(0xFFF59E0B) // Amber warning
            ERROR -> Color(0xFFEF4444)   // Crimson red
            TESTING -> Color(0xFF3B82F6) // Cyan/Blue
        }
}

data class SystemCapabilityItem(
    val id: String,
    val name: String,
    val status: CapabilityStatus,
    val details: String,
    val latencyMs: Long? = null
)

enum class TestStatus(val label: String) {
    PASS("✓ PASS"),
    FAIL("✕ FAIL"),
    WARNING("⚠ WARNING"),
    RUNNING("⟳ RUNNING"),
    PENDING("• PENDING");

    val color: Color
        get() = when (this) {
            PASS -> Color(0xFF10B981)
            FAIL -> Color(0xFFEF4444)
            WARNING -> Color(0xFFF59E0B)
            RUNNING -> Color(0xFF3B82F6)
            PENDING -> Color(0xFF9CA3AF)
        }
}

data class TestProgressItem(
    val id: String,
    val title: String,
    val description: String,
    val status: TestStatus,
    val details: String = "",
    val latencyMs: Long = 0L,
    val subSteps: List<String> = emptyList()
)

data class DiagnosticErrorLog(
    val id: String,
    val timestamp: Long,
    val feature: String,
    val requestId: Int,
    val sourceLanguage: String,
    val targetLanguage: String,
    val operation: String,
    val latencyMs: Long,
    val exceptionType: String,
    val readableErrorMessage: String,
    val recoveryAction: String,
    val fullStackTrace: String? = null
)

data class PerformanceMetricItem(
    val name: String,
    val currentMs: Long,
    val avgMs: Double,
    val minMs: Long,
    val maxMs: Long,
    val sampleCount: Int,
    val unit: String = "ms"
)

data class TestLabSummary(
    val totalTests: Int = 0,
    val passed: Int = 0,
    val failed: Int = 0,
    val warnings: Int = 0,
    val totalDurationMs: Long = 0L,
    val isCompleted: Boolean = false
)
