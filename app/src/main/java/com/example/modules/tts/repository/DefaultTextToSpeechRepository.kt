package com.example.modules.tts.repository

import android.content.Context
import com.example.core.result.AppResult
import com.example.modules.tts.manager.TextToSpeechManager
import com.example.modules.tts.model.TtsLanguageStatus
import com.example.modules.tts.model.TtsPlaybackState
import com.example.modules.tts.model.TtsVoiceInfo
import kotlinx.coroutines.flow.StateFlow

class DefaultTextToSpeechRepository(
    context: Context
) : TextToSpeechRepository {

    override val manager: TextToSpeechManager = TextToSpeechManager(context)
    override val playbackState: StateFlow<TtsPlaybackState> = manager.playbackState

    override suspend fun speak(
        text: String,
        languageCode: String,
        speechRate: Float?,
        pitch: Float?,
        voice: TtsVoiceInfo?
    ): AppResult<Unit> {
        return manager.speak(
            text = text,
            languageCode = languageCode,
            speechRate = speechRate,
            pitch = pitch,
            voice = voice
        )
    }

    override fun play(text: String, languageCode: String) {
        manager.speak(text, languageCode)
    }

    override fun pause() {
        manager.pause()
    }

    override fun resume(): AppResult<Unit> {
        return manager.resume()
    }

    override fun stop() {
        manager.stop()
    }

    override fun replay(): AppResult<Unit> {
        return manager.replay()
    }

    override fun setSpeechRate(rate: Float) {
        manager.setSpeechRate(rate)
    }

    override fun setPitch(pitch: Float) {
        manager.setPitch(pitch)
    }

    override fun setVoice(voice: TtsVoiceInfo?) {
        manager.setVoice(voice)
    }

    override fun checkLanguageAvailability(languageCode: String): TtsLanguageStatus {
        return manager.checkLanguageAvailability(languageCode)
    }

    override fun isLanguageAvailable(languageCode: String): Boolean {
        val status = manager.checkLanguageAvailability(languageCode)
        return status == TtsLanguageStatus.AVAILABLE ||
                status == TtsLanguageStatus.COUNTRY_AVAILABLE ||
                status == TtsLanguageStatus.COUNTRY_VAR_AVAILABLE
    }

    override fun getAvailableVoices(languageCode: String?): List<TtsVoiceInfo> {
        return manager.getAvailableVoices(languageCode)
    }

    override fun release() {
        manager.release()
    }
}
