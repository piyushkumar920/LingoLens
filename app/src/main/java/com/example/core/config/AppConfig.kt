package com.example.core.config

/**
 * Global application configuration parameters.
 */
object AppConfig {
    const val APP_VERSION = "1.0.0"
    const val DATABASE_NAME = "vision_translator_db"
    const val PREFERENCES_NAME = "vision_translator_prefs"

    // Supported engine flags
    const val DEFAULT_SOURCE_LANG = "en"
    const val DEFAULT_TARGET_LANG = "es"

    // Timeouts and limits
    const val NETWORK_TIMEOUT_MS = 15_000L
    const val SPEECH_RECORDING_TIMEOUT_MS = 30_000L
    const val OCR_THROTTLE_MS = 600L
}
