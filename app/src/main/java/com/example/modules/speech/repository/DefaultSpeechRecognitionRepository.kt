package com.example.modules.speech.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Production-ready Android Speech Recognition repository.
 *
 * Interacts directly with Android's [SpeechRecognizer] API on the main thread,
 * handling partial streaming results, live decibel sound levels for visualizers,
 * and comprehensive error codes with clear user-facing diagnostics.
 */
class DefaultSpeechRecognitionRepository(
    private val context: Context
) : SpeechRecognitionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _recognitionState = MutableStateFlow(SpeechRecognitionState())
    override val recognitionState: StateFlow<SpeechRecognitionState> = _recognitionState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _recognitionState.update {
                it.copy(
                    isListening = true,
                    isReady = true,
                    error = null
                )
            }
        }

        override fun onBeginningOfSpeech() {
            _recognitionState.update {
                it.copy(
                    isListening = true,
                    isReady = true
                )
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize decibels: standard Android SpeechRecognizer returns values between -2dB and 10dB
            val clamped = rmsdB.coerceIn(0f, 12f)
            _recognitionState.update {
                it.copy(soundLevelDb = clamped)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Raw audio buffer callback if needed
        }

        override fun onEndOfSpeech() {
            _recognitionState.update {
                it.copy(
                    isReady = false,
                    soundLevelDb = 0f
                )
            }
        }

        override fun onError(errorCode: Int) {
            val (errorMessage, isSilence) = mapSpeechError(errorCode)
            Log.w("SpeechRecognition", "SpeechRecognizer error: $errorCode -> $errorMessage")

            if (errorCode == SpeechRecognizer.ERROR_CLIENT || errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                destroyRecognizerInternal()
            }

            _recognitionState.update { current ->
                current.copy(
                    isListening = false,
                    isReady = false,
                    soundLevelDb = 0f,
                    error = if (isSilence && current.partialText.isNotBlank()) null else errorMessage
                )
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val finalTranscript = matches?.firstOrNull()?.trim() ?: ""

            _recognitionState.update { current ->
                current.copy(
                    isListening = false,
                    isReady = false,
                    soundLevelDb = 0f,
                    finalResult = if (finalTranscript.isNotBlank()) finalTranscript else current.partialText,
                    error = if (finalTranscript.isBlank() && current.partialText.isBlank()) {
                        "No speech detected. Please speak clearly into the microphone."
                    } else null
                )
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim() ?: ""
            if (partial.isNotBlank()) {
                _recognitionState.update {
                    it.copy(
                        partialText = partial,
                        error = null
                    )
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Extended recognizer events
        }
    }

    override fun isRecognitionAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            false
        }
    }

    override fun startListening(languageCode: String): AppResult<Unit> {
        if (!isRecognitionAvailable()) {
            _recognitionState.update {
                it.copy(
                    isListening = false,
                    isUnavailable = true,
                    error = "Speech recognition service is not available on this device."
                )
            }
            return AppResult.Error(
                DomainError.SpeechRecognitionError(
                    message = "Android SpeechRecognizer is unavailable on this device."
                )
            )
        }

        mainHandler.post {
            try {
                // Safely clean up previous recognizer
                destroyRecognizerInternal()

                val recognizer = try {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } catch (t: Throwable) {
                    SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                }
                recognizer.setRecognitionListener(recognitionListener)
                speechRecognizer = recognizer

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                    // Format language tag e.g. "es" -> "es-ES", "zh" -> "zh-CN"
                    val formattedLocale = formatLanguageTag(languageCode)
                    if (formattedLocale.isNotBlank() && formattedLocale != "auto") {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, formattedLocale)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, formattedLocale)
                        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                    }

                    // Silence detection intervals
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }

                _recognitionState.update {
                    it.copy(
                        isListening = true,
                        isReady = false,
                        partialText = "",
                        finalResult = "",
                        soundLevelDb = 0f,
                        error = null,
                        isUnavailable = false
                    )
                }

                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e("SpeechRecognition", "Error starting speech recognition: ${e.message}", e)
                _recognitionState.update {
                    it.copy(
                        isListening = false,
                        isReady = false,
                        error = "Failed to start listening: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }

        return AppResult.Success(Unit)
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w("SpeechRecognition", "Error stopping speech recognizer: ${e.message}")
            }
            _recognitionState.update {
                it.copy(isListening = false, isReady = false, soundLevelDb = 0f)
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w("SpeechRecognition", "Error cancelling speech recognizer: ${e.message}")
            }
            _recognitionState.update {
                it.copy(
                    isListening = false,
                    isReady = false,
                    soundLevelDb = 0f,
                    partialText = "",
                    finalResult = "",
                    error = null
                )
            }
        }
    }

    override fun clearResults() {
        _recognitionState.update {
            it.copy(
                partialText = "",
                finalResult = "",
                error = null,
                soundLevelDb = 0f
            )
        }
    }

    override fun release() {
        mainHandler.post {
            destroyRecognizerInternal()
        }
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("SpeechRecognition", "Error releasing speech recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }

    private fun formatLanguageTag(code: String): String {
        if (code.isBlank() || code.equals("auto", ignoreCase = true)) {
            return Locale.getDefault().toLanguageTag()
        }
        if (code.contains("-") || code.contains("_")) {
            return code.replace("_", "-")
        }
        return when (code.lowercase()) {
            "en" -> "en-US"
            "es" -> "es-ES"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "it" -> "it-IT"
            "pt" -> "pt-BR"
            "zh" -> "zh-CN"
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            "ru" -> "ru-RU"
            "hi" -> "hi-IN"
            "ar" -> "ar-SA"
            "tr" -> "tr-TR"
            "nl" -> "nl-NL"
            "pl" -> "pl-PL"
            "vi" -> "vi-VN"
            "th" -> "th-TH"
            "id" -> "id-ID"
            else -> code
        }
    }

    private fun mapSpeechError(errorCode: Int): Pair<String, Boolean> {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone hardware." to false
            SpeechRecognizer.ERROR_CLIENT -> "Client application error occurred." to false
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required." to false
            SpeechRecognizer.ERROR_NETWORK -> "Network communication error. Please check your internet connection." to false
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out. Please try again." to false
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech was detected. Please try speaking again." to true
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine is currently busy. Please retry." to false
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error. Please try again." to false
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected within time limit." to true
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Selected language is not supported by device speech engine." to false
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Speech language pack is not installed on this device." to false
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech recognition connection was interrupted." to false
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests. Please wait a moment." to false
            else -> "Speech recognition error ($errorCode)." to false
        }
    }
}
