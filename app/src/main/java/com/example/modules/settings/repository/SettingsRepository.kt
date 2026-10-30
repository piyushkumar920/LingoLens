package com.example.modules.settings.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.modules.settings.model.AppTextSize
import com.example.modules.settings.model.AppThemeMode
import com.example.modules.settings.model.FlashPreference
import com.example.modules.settings.model.FreezeFrameBehavior
import com.example.modules.settings.model.TranslationStyle
import com.example.modules.settings.model.UserSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

interface SettingsRepository {
    val settings: StateFlow<UserSettings>

    // TRANSLATION
    suspend fun updateLanguages(sourceLang: String, targetLang: String)
    suspend fun setAutoDetect(enabled: Boolean)
    suspend fun setAutoSpeakTranslation(enabled: Boolean)
    suspend fun setTranslationStyle(style: TranslationStyle)

    // CAMERA
    suspend fun setLiveCameraTranslation(enabled: Boolean)
    suspend fun setCameraOverlayOpacity(opacity: Float)
    suspend fun setCameraTextSize(size: Float)
    suspend fun setFreezeFrameBehavior(behavior: FreezeFrameBehavior)
    suspend fun setFlashPreference(preference: FlashPreference)

    // VOICE
    suspend fun updateSpeechRate(rate: Float)
    suspend fun updateSpeechPitch(pitch: Float)
    suspend fun setAutoSpeakVoice(enabled: Boolean)
    suspend fun setSelectedVoiceName(voiceName: String?)

    // OFFLINE
    suspend fun setOfflinePreferred(preferred: Boolean)
    suspend fun setAutoUpdateModels(enabled: Boolean)

    // APPEARANCE
    suspend fun updateTheme(themeMode: AppThemeMode)
    suspend fun setTextSize(textSize: AppTextSize)
    suspend fun setHighContrastMode(enabled: Boolean)

    // PRIVACY
    suspend fun setHistoryLoggingEnabled(enabled: Boolean)

    // AI
    suspend fun setAiContextEnabled(enabled: Boolean)
    suspend fun setAiExplainEnabled(enabled: Boolean)

    // Compatibility alias
    suspend fun setAutoPlayTts(enabled: Boolean) = setAutoSpeakTranslation(enabled)
}

class DefaultSettingsRepository(
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    private val prefs: SharedPreferences? = context?.getSharedPreferences("vision_translator_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadInitialSettings())
    override val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadInitialSettings(): UserSettings {
        if (prefs == null) return UserSettings()

        val themeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val styleStr = prefs.getString(KEY_TRANSLATION_STYLE, TranslationStyle.STANDARD.name) ?: TranslationStyle.STANDARD.name
        val freezeStr = prefs.getString(KEY_FREEZE_BEHAVIOR, FreezeFrameBehavior.TAP_TO_FREEZE.name) ?: FreezeFrameBehavior.TAP_TO_FREEZE.name
        val flashStr = prefs.getString(KEY_FLASH_PREF, FlashPreference.AUTO.name) ?: FlashPreference.AUTO.name
        val textSizeStr = prefs.getString(KEY_TEXT_SIZE, AppTextSize.DEFAULT.name) ?: AppTextSize.DEFAULT.name

        return UserSettings(
            // TRANSLATION
            defaultSourceLanguage = prefs.getString(KEY_SOURCE_LANG, "en") ?: "en",
            defaultTargetLanguage = prefs.getString(KEY_TARGET_LANG, "es") ?: "es",
            autoDetectLanguage = prefs.getBoolean(KEY_AUTO_DETECT, true),
            autoSpeakTranslation = prefs.getBoolean(KEY_AUTO_SPEAK_TRANSLATION, false),
            translationStyle = runCatching { TranslationStyle.valueOf(styleStr) }.getOrDefault(TranslationStyle.STANDARD),

            // CAMERA
            liveCameraTranslation = prefs.getBoolean(KEY_LIVE_CAMERA, true),
            cameraOverlayOpacity = prefs.getFloat(KEY_CAMERA_OPACITY, 0.85f),
            cameraTextSize = prefs.getFloat(KEY_CAMERA_TEXT_SIZE, 14f),
            freezeFrameBehavior = runCatching { FreezeFrameBehavior.valueOf(freezeStr) }.getOrDefault(FreezeFrameBehavior.TAP_TO_FREEZE),
            flashPreference = runCatching { FlashPreference.valueOf(flashStr) }.getOrDefault(FlashPreference.AUTO),

            // VOICE
            speechSpeed = prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
            speechPitch = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f),
            autoSpeakVoice = prefs.getBoolean(KEY_AUTO_SPEAK_VOICE, true),
            selectedVoiceName = prefs.getString(KEY_VOICE_NAME, null),

            // OFFLINE
            offlineModePreferred = prefs.getBoolean(KEY_OFFLINE_PREF, false),
            autoUpdateModels = prefs.getBoolean(KEY_AUTO_UPDATE_MODELS, true),

            // APPEARANCE
            themeMode = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.DARK),
            textSize = runCatching { AppTextSize.valueOf(textSizeStr) }.getOrDefault(AppTextSize.DEFAULT),
            highContrastMode = prefs.getBoolean(KEY_HIGH_CONTRAST, false),

            // PRIVACY
            historyLoggingEnabled = prefs.getBoolean(KEY_HISTORY_LOGGING, true),

            // AI
            aiContextEnabled = prefs.getBoolean(KEY_AI_CONTEXT, true),
            aiExplainEnabled = prefs.getBoolean(KEY_AI_EXPLAIN, true)
        )
    }

    private suspend fun save(updater: (SharedPreferences.Editor) -> Unit) {
        if (prefs == null) return
        withContext(ioDispatcher) {
            val editor = prefs.edit()
            updater(editor)
            editor.apply()
        }
    }

    // TRANSLATION
    override suspend fun updateLanguages(sourceLang: String, targetLang: String) {
        _settings.update { it.copy(defaultSourceLanguage = sourceLang, defaultTargetLanguage = targetLang) }
        save {
            it.putString(KEY_SOURCE_LANG, sourceLang)
            it.putString(KEY_TARGET_LANG, targetLang)
        }
    }

    override suspend fun setAutoDetect(enabled: Boolean) {
        _settings.update { it.copy(autoDetectLanguage = enabled) }
        save { it.putBoolean(KEY_AUTO_DETECT, enabled) }
    }

    override suspend fun setAutoSpeakTranslation(enabled: Boolean) {
        _settings.update { it.copy(autoSpeakTranslation = enabled) }
        save { it.putBoolean(KEY_AUTO_SPEAK_TRANSLATION, enabled) }
    }

    override suspend fun setTranslationStyle(style: TranslationStyle) {
        _settings.update { it.copy(translationStyle = style) }
        save { it.putString(KEY_TRANSLATION_STYLE, style.name) }
    }

    // CAMERA
    override suspend fun setLiveCameraTranslation(enabled: Boolean) {
        _settings.update { it.copy(liveCameraTranslation = enabled) }
        save { it.putBoolean(KEY_LIVE_CAMERA, enabled) }
    }

    override suspend fun setCameraOverlayOpacity(opacity: Float) {
        _settings.update { it.copy(cameraOverlayOpacity = opacity) }
        save { it.putFloat(KEY_CAMERA_OPACITY, opacity) }
    }

    override suspend fun setCameraTextSize(size: Float) {
        _settings.update { it.copy(cameraTextSize = size) }
        save { it.putFloat(KEY_CAMERA_TEXT_SIZE, size) }
    }

    override suspend fun setFreezeFrameBehavior(behavior: FreezeFrameBehavior) {
        _settings.update { it.copy(freezeFrameBehavior = behavior) }
        save { it.putString(KEY_FREEZE_BEHAVIOR, behavior.name) }
    }

    override suspend fun setFlashPreference(preference: FlashPreference) {
        _settings.update { it.copy(flashPreference = preference) }
        save { it.putString(KEY_FLASH_PREF, preference.name) }
    }

    // VOICE
    override suspend fun updateSpeechRate(rate: Float) {
        _settings.update { it.copy(speechSpeed = rate) }
        save { it.putFloat(KEY_SPEECH_RATE, rate) }
    }

    override suspend fun updateSpeechPitch(pitch: Float) {
        _settings.update { it.copy(speechPitch = pitch) }
        save { it.putFloat(KEY_SPEECH_PITCH, pitch) }
    }

    override suspend fun setAutoSpeakVoice(enabled: Boolean) {
        _settings.update { it.copy(autoSpeakVoice = enabled) }
        save { it.putBoolean(KEY_AUTO_SPEAK_VOICE, enabled) }
    }

    override suspend fun setSelectedVoiceName(voiceName: String?) {
        _settings.update { it.copy(selectedVoiceName = voiceName) }
        save {
            if (voiceName != null) {
                it.putString(KEY_VOICE_NAME, voiceName)
            } else {
                it.remove(KEY_VOICE_NAME)
            }
        }
    }

    // OFFLINE
    override suspend fun setOfflinePreferred(preferred: Boolean) {
        _settings.update { it.copy(offlineModePreferred = preferred) }
        save { it.putBoolean(KEY_OFFLINE_PREF, preferred) }
    }

    override suspend fun setAutoUpdateModels(enabled: Boolean) {
        _settings.update { it.copy(autoUpdateModels = enabled) }
        save { it.putBoolean(KEY_AUTO_UPDATE_MODELS, enabled) }
    }

    // APPEARANCE
    override suspend fun updateTheme(themeMode: AppThemeMode) {
        _settings.update { it.copy(themeMode = themeMode) }
        save { it.putString(KEY_THEME_MODE, themeMode.name) }
    }

    override suspend fun setTextSize(textSize: AppTextSize) {
        _settings.update { it.copy(textSize = textSize) }
        save { it.putString(KEY_TEXT_SIZE, textSize.name) }
    }

    override suspend fun setHighContrastMode(enabled: Boolean) {
        _settings.update { it.copy(highContrastMode = enabled) }
        save { it.putBoolean(KEY_HIGH_CONTRAST, enabled) }
    }

    // PRIVACY
    override suspend fun setHistoryLoggingEnabled(enabled: Boolean) {
        _settings.update { it.copy(historyLoggingEnabled = enabled) }
        save { it.putBoolean(KEY_HISTORY_LOGGING, enabled) }
    }

    // AI
    override suspend fun setAiContextEnabled(enabled: Boolean) {
        _settings.update { it.copy(aiContextEnabled = enabled) }
        save { it.putBoolean(KEY_AI_CONTEXT, enabled) }
    }

    override suspend fun setAiExplainEnabled(enabled: Boolean) {
        _settings.update { it.copy(aiExplainEnabled = enabled) }
        save { it.putBoolean(KEY_AI_EXPLAIN, enabled) }
    }

    companion object {
        private const val KEY_SOURCE_LANG = "key_default_source_lang"
        private const val KEY_TARGET_LANG = "key_default_target_lang"
        private const val KEY_AUTO_DETECT = "key_auto_detect"
        private const val KEY_AUTO_SPEAK_TRANSLATION = "key_auto_speak_translation"
        private const val KEY_TRANSLATION_STYLE = "key_translation_style"

        private const val KEY_LIVE_CAMERA = "key_live_camera"
        private const val KEY_CAMERA_OPACITY = "key_camera_opacity"
        private const val KEY_CAMERA_TEXT_SIZE = "key_camera_text_size"
        private const val KEY_FREEZE_BEHAVIOR = "key_freeze_behavior"
        private const val KEY_FLASH_PREF = "key_flash_pref"

        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_AUTO_SPEAK_VOICE = "key_auto_speak_voice"
        private const val KEY_VOICE_NAME = "key_voice_name"

        private const val KEY_OFFLINE_PREF = "key_offline_pref"
        private const val KEY_AUTO_UPDATE_MODELS = "key_auto_update_models"

        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_TEXT_SIZE = "key_text_size"
        private const val KEY_HIGH_CONTRAST = "key_high_contrast"

        private const val KEY_HISTORY_LOGGING = "key_history_logging"

        private const val KEY_AI_CONTEXT = "key_ai_context"
        private const val KEY_AI_EXPLAIN = "key_ai_explain"
    }
}
