package com.example.modules.settings.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TranslationStyle(val displayName: String, val description: String) {
    STANDARD("Standard", "Natural, accurate everyday translation"),
    FORMAL("Formal / Polite", "Respectful honorifics and professional phrasing"),
    CASUAL("Casual / Colloquial", "Informal, conversational tone with idioms"),
    LITERAL("Literal / Word-for-Word", "Faithful syntactic structure for language learning"),
    SIMPLIFIED("Simplified", "Direct, concise phrasing with high clarity")
}

enum class FreezeFrameBehavior(val displayName: String, val description: String) {
    TAP_TO_FREEZE("Tap to Freeze", "Tap screen to pause viewfinder and inspect text"),
    AUTO_ON_STABLE("Auto-freeze When Stable", "Automatically freezes frame when camera is held still"),
    MANUAL_BUTTON("Manual Shutter Button", "Uses dedicated capture button to freeze and translate")
}

enum class FlashPreference(val displayName: String) {
    AUTO("Auto"),
    ON("Always On in Dark"),
    OFF("Always Off")
}

enum class AppTextSize(val displayName: String, val scaleFactor: Float) {
    COMPACT("Compact", 0.9f),
    DEFAULT("Default", 1.0f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra Large", 1.3f)
}

data class UserSettings(
    // TRANSLATION
    val defaultSourceLanguage: String = "en",
    val defaultTargetLanguage: String = "es",
    val autoDetectLanguage: Boolean = true,
    val autoSpeakTranslation: Boolean = false,
    val translationStyle: TranslationStyle = TranslationStyle.STANDARD,

    // CAMERA
    val liveCameraTranslation: Boolean = true,
    val cameraOverlayOpacity: Float = 0.85f,
    val cameraTextSize: Float = 14f,
    val freezeFrameBehavior: FreezeFrameBehavior = FreezeFrameBehavior.TAP_TO_FREEZE,
    val flashPreference: FlashPreference = FlashPreference.AUTO,

    // VOICE
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val autoSpeakVoice: Boolean = true,
    val selectedVoiceName: String? = null,

    // OFFLINE
    val offlineModePreferred: Boolean = false,
    val autoUpdateModels: Boolean = true,

    // APPEARANCE
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val textSize: AppTextSize = AppTextSize.DEFAULT,
    val highContrastMode: Boolean = false,

    // PRIVACY
    val historyLoggingEnabled: Boolean = true,

    // AI
    val aiContextEnabled: Boolean = true,
    val aiExplainEnabled: Boolean = true
) {
    val autoPlayTts: Boolean get() = autoSpeakTranslation
    val speechRate: Float get() = speechSpeed
}
