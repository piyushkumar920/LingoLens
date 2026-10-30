package com.example.modules.speech.repository

import com.example.core.result.AppResult
import kotlinx.coroutines.flow.StateFlow

/**
 * State representing live speech recognition lifecycle and live audio metrics.
 */
data class SpeechRecognitionState(
    val isListening: Boolean = false,
    val isReady: Boolean = false,
    val partialText: String = "",
    val finalResult: String = "",
    val soundLevelDb: Float = 0f,
    val detectedLanguageCode: String? = null,
    val error: String? = null,
    val isUnavailable: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Repository interface for real Android Speech Recognition.
 */
interface SpeechRecognitionRepository {
    val recognitionState: StateFlow<SpeechRecognitionState>
    fun startListening(languageCode: String = "en-US"): AppResult<Unit>
    fun stopListening()
    fun cancel()
    fun isRecognitionAvailable(): Boolean
    fun clearResults()
    fun release()
}
