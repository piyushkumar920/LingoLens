package com.example.modules.tts.manager

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.modules.tts.model.TtsLanguageStatus
import com.example.modules.tts.model.TtsPlaybackState
import com.example.modules.tts.model.TtsPlaybackStatus
import com.example.modules.tts.model.TtsVoiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * Production-ready, UI-independent manager for Android Text-to-Speech engine.
 *
 * Provides:
 * - Robust lifecycle management (initialization, error handling, clean shutdown)
 * - Utterance progress tracking with start, done, error, range, and stop events
 * - Speech rate (0.5x - 2.0x) and pitch adjustments (0.5f - 2.0f)
 * - Available voice querying, filtering, and selection
 * - Multi-stage language availability detection with automatic safe fallbacks
 * - Pause, resume, stop, and replay controls
 * - System TTS data installation and settings intents
 */
class TextToSpeechManager(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private val tag = "TextToSpeechManager"
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false

    private val _playbackState = MutableStateFlow(TtsPlaybackState(status = TtsPlaybackStatus.INITIALIZING))
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private var currentUtteranceId: String? = null
    private var queuedSpeakRequest: QueuedSpeak? = null

    private data class QueuedSpeak(
        val text: String,
        val languageCode: String,
        val speechRate: Float?,
        val pitch: Float?,
        val voice: TtsVoiceInfo?
    )

    init {
        initTtsEngine()
    }

    private fun initTtsEngine() {
        try {
            _playbackState.update { it.copy(status = TtsPlaybackStatus.INITIALIZING) }
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to instantiate TextToSpeech", e)
            isInitialized = false
            _playbackState.update {
                it.copy(
                    status = TtsPlaybackStatus.ERROR,
                    errorMessage = "Failed to initialize Android Text-to-Speech: ${e.localizedMessage}"
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
            _playbackState.update {
                it.copy(
                    status = TtsPlaybackStatus.IDLE,
                    errorMessage = null
                )
            }
            Log.i(tag, "TextToSpeech engine successfully initialized")

            // Execute any queued speak request from before init finished
            val queued = queuedSpeakRequest
            if (queued != null) {
                queuedSpeakRequest = null
                managerScope.launch {
                    speak(
                        text = queued.text,
                        languageCode = queued.languageCode,
                        speechRate = queued.speechRate,
                        pitch = queued.pitch,
                        voice = queued.voice
                    )
                }
            }
        } else {
            isInitialized = false
            _playbackState.update {
                it.copy(
                    status = TtsPlaybackStatus.ERROR,
                    errorMessage = "TTS engine initialization failed (status code: $status)"
                )
            }
            Log.e(tag, "TextToSpeech engine onInit failed with status $status")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _playbackState.update {
                    it.copy(
                        status = TtsPlaybackStatus.PLAYING,
                        isSpeaking = true,
                        isPaused = false,
                        errorMessage = null
                    )
                }
            }

            override fun onDone(utteranceId: String?) {
                _playbackState.update {
                    it.copy(
                        status = TtsPlaybackStatus.IDLE,
                        isSpeaking = false,
                        isPaused = false,
                        progress = 1.0f
                    )
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                handleUtteranceError(utteranceId, -1)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                handleUtteranceError(utteranceId, errorCode)
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val currentText = _playbackState.value.currentlySpokenText
                val totalLength = currentText?.length ?: 1
                val progress = if (totalLength > 0) (start.toFloat() / totalLength).coerceIn(0f, 1f) else 0f
                _playbackState.update {
                    it.copy(
                        progress = progress,
                        startCharIndex = start,
                        endCharIndex = end
                    )
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _playbackState.update {
                    it.copy(
                        status = if (it.isPaused) TtsPlaybackStatus.PAUSED else TtsPlaybackStatus.STOPPED,
                        isSpeaking = false
                    )
                }
            }
        })
    }

    private fun handleUtteranceError(utteranceId: String?, errorCode: Int) {
        val message = when (errorCode) {
            TextToSpeech.ERROR_SYNTHESIS -> "TTS synthesis failed"
            TextToSpeech.ERROR_SERVICE -> "TTS service connection error"
            TextToSpeech.ERROR_OUTPUT -> "Audio output device error"
            TextToSpeech.ERROR_NETWORK -> "Network required for online TTS voice"
            TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timed out fetching voice stream"
            TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid TTS request payload"
            TextToSpeech.ERROR_NOT_INSTALLED_YET -> "Voice data not downloaded yet"
            else -> "Speech output encountered an error (code $errorCode)"
        }
        Log.w(tag, "Utterance error [$utteranceId]: $message")
        _playbackState.update {
            it.copy(
                status = TtsPlaybackStatus.ERROR,
                isSpeaking = false,
                isPaused = false,
                errorMessage = message
            )
        }
    }

    /**
     * Speaks the given text in the specified language code with safe automatic fallbacks.
     */
    fun speak(
        text: String,
        languageCode: String,
        speechRate: Float? = null,
        pitch: Float? = null,
        voice: TtsVoiceInfo? = null
    ): AppResult<Unit> {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return AppResult.Success(Unit)
        }

        if (!isInitialized || tts == null) {
            // Queue request to play once initialized
            queuedSpeakRequest = QueuedSpeak(cleanText, languageCode, speechRate, pitch, voice)
            return AppResult.Loading
        }

        val rate = speechRate ?: _playbackState.value.speechRate
        val currentPitch = pitch ?: _playbackState.value.pitch

        // Apply speech rate & pitch
        tts?.setSpeechRate(rate.coerceIn(0.25f, 3.0f))
        tts?.setPitch(currentPitch.coerceIn(0.25f, 2.5f))

        // Resolve best locale with multi-stage fallback
        val (resolvedLocale, fallbackWarning) = resolveLocaleWithFallback(languageCode)

        var setLangResult = tts?.setLanguage(resolvedLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (setLangResult < TextToSpeech.LANG_AVAILABLE) {
            // Attempt emergency fallback to English (US) or Default
            val defaultLocale = Locale.getDefault()
            val fallbackLocale = if (tts?.isLanguageAvailable(defaultLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED >= TextToSpeech.LANG_AVAILABLE) {
                defaultLocale
            } else {
                Locale.US
            }
            tts?.setLanguage(fallbackLocale)
        }

        // Apply voice if specified and compatible
        if (voice != null) {
            val matchingVoice = getInstalledAndroidVoices().firstOrNull { it.name == voice.name }
            if (matchingVoice != null) {
                tts?.voice = matchingVoice
            }
        }

        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        _playbackState.update {
            it.copy(
                status = TtsPlaybackStatus.PLAYING,
                isSpeaking = true,
                isPaused = false,
                currentlySpokenText = cleanText,
                languageCode = languageCode,
                activeLocale = resolvedLocale,
                speechRate = rate,
                pitch = currentPitch,
                currentVoice = voice,
                progress = 0f,
                startCharIndex = 0,
                endCharIndex = 0,
                errorMessage = null,
                warningMessage = fallbackWarning,
                lastSpokenText = cleanText,
                lastSpokenLanguageCode = languageCode
            )
        }

        val speakResult = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return if (speakResult == TextToSpeech.SUCCESS) {
            AppResult.Success(Unit)
        } else {
            val err = "Failed to queue speech in TTS engine."
            _playbackState.update { it.copy(status = TtsPlaybackStatus.ERROR, errorMessage = err, isSpeaking = false) }
            AppResult.Error(DomainError.TtsUnavailableError(languageCode, err))
        }
    }

    /**
     * Replays the last spoken text.
     */
    fun replay(): AppResult<Unit> {
        val lastText = _playbackState.value.lastSpokenText ?: _playbackState.value.currentlySpokenText
        val lastLang = _playbackState.value.lastSpokenLanguageCode ?: _playbackState.value.languageCode ?: "en"
        return if (!lastText.isNullOrBlank()) {
            speak(lastText, lastLang)
        } else {
            AppResult.Success(Unit)
        }
    }

    /**
     * Pauses current playback (by storing pause position and stopping playback).
     */
    fun pause() {
        if (_playbackState.value.isSpeaking) {
            _playbackState.update {
                it.copy(
                    isPaused = true,
                    isSpeaking = false,
                    status = TtsPlaybackStatus.PAUSED
                )
            }
            tts?.stop()
        }
    }

    /**
     * Resumes playback from the paused state.
     */
    fun resume(): AppResult<Unit> {
        val state = _playbackState.value
        val text = state.currentlySpokenText ?: state.lastSpokenText
        val lang = state.languageCode ?: state.lastSpokenLanguageCode ?: "en"

        if (!text.isNullOrBlank() && state.isPaused) {
            // Resume remaining text or replay
            val remainingText = if (state.startCharIndex in 1 until text.length) {
                text.substring(state.startCharIndex)
            } else {
                text
            }
            return speak(remainingText, lang)
        }
        return AppResult.Success(Unit)
    }

    /**
     * Stops current speech and resets playback state.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(tag, "Error during tts.stop()", e)
        }
        _playbackState.update {
            it.copy(
                status = TtsPlaybackStatus.STOPPED,
                isSpeaking = false,
                isPaused = false,
                progress = 0f
            )
        }
    }

    /**
     * Adjusts the global speech rate.
     */
    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.25f, 3.0f)
        _playbackState.update { it.copy(speechRate = clamped) }
        tts?.setSpeechRate(clamped)
    }

    /**
     * Adjusts the global voice pitch.
     */
    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.25f, 2.5f)
        _playbackState.update { it.copy(pitch = clamped) }
        tts?.setPitch(clamped)
    }

    /**
     * Sets preferred voice.
     */
    fun setVoice(voice: TtsVoiceInfo?) {
        _playbackState.update { it.copy(currentVoice = voice) }
        if (voice != null) {
            val matching = getInstalledAndroidVoices().firstOrNull { it.name == voice.name }
            if (matching != null) {
                tts?.voice = matching
            }
        }
    }

    /**
     * Checks if a language is available in the on-device TTS engine.
     */
    fun checkLanguageAvailability(languageCode: String): TtsLanguageStatus {
        if (!isInitialized || tts == null) return TtsLanguageStatus.UNINITIALIZED
        val locale = Locale.forLanguageTag(languageCode)
        return try {
            when (tts?.isLanguageAvailable(locale)) {
                TextToSpeech.LANG_AVAILABLE -> TtsLanguageStatus.AVAILABLE
                TextToSpeech.LANG_COUNTRY_AVAILABLE -> TtsLanguageStatus.COUNTRY_AVAILABLE
                TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> TtsLanguageStatus.COUNTRY_VAR_AVAILABLE
                TextToSpeech.LANG_MISSING_DATA -> TtsLanguageStatus.MISSING_DATA
                TextToSpeech.LANG_NOT_SUPPORTED -> TtsLanguageStatus.NOT_SUPPORTED
                else -> TtsLanguageStatus.NOT_SUPPORTED
            }
        } catch (e: Exception) {
            TtsLanguageStatus.NOT_SUPPORTED
        }
    }

    /**
     * Returns all installed voices for the system, optionally filtered by language code.
     */
    fun getAvailableVoices(languageCode: String? = null): List<TtsVoiceInfo> {
        if (!isInitialized || tts == null) return emptyList()
        val targetLocale = languageCode?.let { Locale.forLanguageTag(it) }

        return try {
            val voices = getInstalledAndroidVoices()
            voices.mapNotNull { voice ->
                if (targetLocale == null || voice.locale.language.equals(targetLocale.language, ignoreCase = true)) {
                    val quality = when (voice.quality) {
                        Voice.QUALITY_VERY_HIGH -> "Very High Quality"
                        Voice.QUALITY_HIGH -> "High Quality"
                        Voice.QUALITY_NORMAL -> "Normal Quality"
                        Voice.QUALITY_LOW -> "Low Quality"
                        else -> "Standard"
                    }
                    val latency = when (voice.latency) {
                        Voice.LATENCY_VERY_LOW -> "Very Low Latency"
                        Voice.LATENCY_LOW -> "Low Latency"
                        Voice.LATENCY_NORMAL -> "Normal Latency"
                        else -> "Standard Latency"
                    }
                    TtsVoiceInfo(
                        name = voice.name,
                        locale = voice.locale,
                        languageDisplayName = "${voice.locale.displayLanguage} (${voice.locale.displayCountry})",
                        quality = quality,
                        latency = latency,
                        requiresNetwork = voice.isNetworkConnectionRequired,
                        isInstalled = !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to retrieve voices", e)
            emptyList()
        }
    }

    private fun getInstalledAndroidVoices(): Set<Voice> {
        return try {
            tts?.voices ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Resolves the best supported locale for a language code with multi-step fallbacks.
     */
    private fun resolveLocaleWithFallback(languageCode: String): Pair<Locale, String?> {
        val targetLocale = Locale.forLanguageTag(languageCode)
        if (isLocaleAvailable(targetLocale)) {
            return Pair(targetLocale, null)
        }

        // 1. Try base language without country
        val baseLocale = Locale.forLanguageTag(targetLocale.language)
        if (isLocaleAvailable(baseLocale)) {
            return Pair(baseLocale, null)
        }

        // 2. Try common regional variants for the language
        val regionalLocales = getInstalledAndroidVoices()
            .map { it.locale }
            .filter { it.language.equals(targetLocale.language, ignoreCase = true) }

        if (regionalLocales.isNotEmpty()) {
            val firstMatch = regionalLocales.first()
            return Pair(firstMatch, "Using ${firstMatch.displayCountry} voice for ${targetLocale.displayLanguage}")
        }

        // 3. Fallback to system default or English
        val deviceLocale = Locale.getDefault()
        if (isLocaleAvailable(deviceLocale)) {
            return Pair(
                deviceLocale,
                "${targetLocale.displayLanguage} voice not installed. Playing in ${deviceLocale.displayLanguage}."
            )
        }

        return Pair(
            Locale.US,
            "${targetLocale.displayLanguage} voice not found. Playing in English."
        )
    }

    private fun isLocaleAvailable(locale: Locale): Boolean {
        if (!isInitialized || tts == null) return false
        val status = try {
            tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        } catch (e: Exception) {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
        return status >= TextToSpeech.LANG_AVAILABLE
    }

    /**
     * Creates an intent to launch system TTS voice data installation.
     */
    fun createInstallVoiceDataIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Creates an intent to launch device Text-to-Speech settings.
     */
    fun createTtsSettingsIntent(): Intent {
        return Intent("com.android.settings.TTS_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Releases the TTS engine and cleans up all background resources.
     */
    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(tag, "Error during TTS shutdown", e)
        } finally {
            tts = null
            isInitialized = false
            _playbackState.update { TtsPlaybackState(status = TtsPlaybackStatus.STOPPED) }
            Log.i(tag, "TextToSpeechManager released")
        }
    }
}
