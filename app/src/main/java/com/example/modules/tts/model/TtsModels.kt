package com.example.modules.tts.model

import java.util.Locale

/**
 * High-level playback status for TTS.
 */
enum class TtsPlaybackStatus {
    IDLE,
    INITIALIZING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

/**
 * Status of voice engine language support on the current device.
 */
enum class TtsLanguageStatus {
    AVAILABLE,
    COUNTRY_AVAILABLE,
    COUNTRY_VAR_AVAILABLE,
    MISSING_DATA,
    NOT_SUPPORTED,
    UNINITIALIZED
}

/**
 * Metadata for an individual Android TTS Voice instance.
 */
data class TtsVoiceInfo(
    val name: String,
    val locale: Locale,
    val languageDisplayName: String,
    val quality: String = "Normal", // "High Quality", "Normal", "Very High"
    val latency: String = "Normal", // "Very Low Latency", "Normal"
    val requiresNetwork: Boolean = false,
    val isInstalled: Boolean = true
)

/**
 * Complete immutable playback and configuration state for TextToSpeech.
 */
data class TtsPlaybackState(
    val status: TtsPlaybackStatus = TtsPlaybackStatus.IDLE,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val currentlySpokenText: String? = null,
    val languageCode: String? = null,
    val activeLocale: Locale? = null,
    val currentVoice: TtsVoiceInfo? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val progress: Float = 0f,
    val startCharIndex: Int = 0,
    val endCharIndex: Int = 0,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
    val lastSpokenText: String? = null,
    val lastSpokenLanguageCode: String? = null
)

/**
 * Preset speech speeds for user selection.
 */
data class TtsSpeedOption(
    val label: String,
    val rate: Float
) {
    companion object {
        val ALL = listOf(
            TtsSpeedOption("0.5x (Slowest)", 0.5f),
            TtsSpeedOption("0.75x (Slow)", 0.75f),
            TtsSpeedOption("1.0x (Normal)", 1.0f),
            TtsSpeedOption("1.25x (Fast)", 1.25f),
            TtsSpeedOption("1.5x (Faster)", 1.5f),
            TtsSpeedOption("2.0x (Fastest)", 2.0f)
        )
    }
}
