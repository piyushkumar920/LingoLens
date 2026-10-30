package com.example.modules.language.model

import com.example.modules.ocr.model.OcrScript

/**
 * Centralized Language model representing all metadata, capabilities, and offline states.
 */
data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val script: String = "Latin",
    val supportsOcr: Boolean = true,
    val supportsTranslation: Boolean = true,
    val supportsSpeech: Boolean = true,
    val supportsTts: Boolean = true,
    val supportsOffline: Boolean = true,
    val isDownloadedOffline: Boolean = false,
    val isFavorite: Boolean = false
) {
    val isAuto: Boolean get() = code.equals(AUTO_CODE, ignoreCase = true)
    val isAutoDetect: Boolean get() = isAuto

    val displayName: String get() = name
    val flag: String get() = flagEmoji

    val ocrScript: OcrScript
        get() = OcrScript.fromLanguageCode(code)

    companion object {
        const val AUTO_CODE = "auto"

        val AUTO_DETECT = Language(
            code = AUTO_CODE,
            name = "Auto Detect",
            nativeName = "Detect Language",
            flagEmoji = "🌐",
            script = "Universal",
            supportsOcr = true,
            supportsTranslation = true,
            supportsSpeech = true,
            supportsTts = false,
            supportsOffline = false
        )

        val ENGLISH = Language(
            code = "en",
            name = "English",
            nativeName = "English",
            flagEmoji = "🇺🇸",
            script = "Latin"
        )

        val SPANISH = Language(
            code = "es",
            name = "Spanish",
            nativeName = "Español",
            flagEmoji = "🇪🇸",
            script = "Latin"
        )
    }
}
