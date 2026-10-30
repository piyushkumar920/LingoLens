package com.example.modules.tts.repository

import com.example.core.result.AppResult
import com.example.modules.tts.manager.TextToSpeechManager
import com.example.modules.tts.model.TtsLanguageStatus
import com.example.modules.tts.model.TtsPlaybackState
import com.example.modules.tts.model.TtsVoiceInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Text-to-Speech Repository providing clean abstraction over TextToSpeechManager.
 */
interface TextToSpeechRepository {
    val manager: TextToSpeechManager
    val playbackState: StateFlow<TtsPlaybackState>

    suspend fun speak(
        text: String,
        languageCode: String,
        speechRate: Float? = null,
        pitch: Float? = null,
        voice: TtsVoiceInfo? = null
    ): AppResult<Unit>

    fun play(text: String, languageCode: String)
    fun pause()
    fun resume(): AppResult<Unit>
    fun stop()
    fun replay(): AppResult<Unit>
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun setVoice(voice: TtsVoiceInfo?)
    fun checkLanguageAvailability(languageCode: String): TtsLanguageStatus
    fun isLanguageAvailable(languageCode: String): Boolean
    fun getAvailableVoices(languageCode: String? = null): List<TtsVoiceInfo>
    fun release()
}
