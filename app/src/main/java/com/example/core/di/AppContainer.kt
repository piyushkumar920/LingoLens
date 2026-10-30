package com.example.core.di

import android.content.Context
import com.example.core.config.DefaultSecureSecretManager
import com.example.core.config.SecureSecretManager
import com.example.core.network.DefaultNetworkConnectivityMonitor
import com.example.core.network.NetworkConnectivityMonitor
import com.example.data.local.AppDatabase
import com.example.data.repository.RoomFavoritesRepository
import com.example.data.repository.RoomHistoryRepository
import com.example.modules.camera.repository.CameraRepository
import com.example.modules.camera.repository.DefaultCameraRepository
import com.example.modules.conversation.repository.ConversationRepository
import com.example.modules.conversation.repository.DefaultConversationRepository
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.image_translation.repository.DefaultImageTranslationRepository
import com.example.modules.image_translation.repository.ImageTranslationRepository
import com.example.modules.language.identifier.LanguageIdentifier
import com.example.modules.language.identifier.MlKitLanguageIdentifier
import com.example.modules.language.repository.DefaultLanguageRepository
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.ocr.repository.DefaultOcrRepository
import com.example.modules.ocr.repository.OcrRepository
import com.example.modules.offline.repository.DefaultOfflinePackageRepository
import com.example.modules.offline.repository.OfflinePackageRepository
import com.example.modules.settings.repository.DefaultSettingsRepository
import com.example.modules.settings.repository.SettingsRepository
import com.example.modules.speech.repository.DefaultSpeechRecognitionRepository
import com.example.modules.speech.repository.SpeechRecognitionRepository
import com.example.modules.travel.repository.DefaultTravelRepository
import com.example.modules.travel.repository.TravelRepository
import com.example.modules.translation.repository.DefaultTranslationRepository
import com.example.modules.translation.repository.TranslationRepository
import com.example.modules.translation.service.MlKitTranslationService
import com.example.modules.translation.service.TranslationService
import com.example.modules.tts.repository.DefaultTextToSpeechRepository
import com.example.modules.tts.repository.TextToSpeechRepository

/**
 * Production dependency injection container.
 * Provides singleton and scoped access to all repositories and services cleanly without reflection.
 */
interface AppContainer {
    val secureSecretManager: SecureSecretManager
    val database: AppDatabase
    val historyRepository: HistoryRepository
    val favoritesRepository: FavoritesRepository
    val languageRepository: LanguageRepository
    val translationRepository: TranslationRepository
    val ocrRepository: OcrRepository
    val imageTranslationRepository: ImageTranslationRepository
    val cameraRepository: CameraRepository
    val speechRepository: SpeechRecognitionRepository
    val ttsRepository: TextToSpeechRepository
    val conversationRepository: ConversationRepository
    val travelRepository: TravelRepository
    val offlinePackageRepository: OfflinePackageRepository
    val settingsRepository: SettingsRepository
    val connectivityMonitor: NetworkConnectivityMonitor
    val translationService: TranslationService
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val secureSecretManager: SecureSecretManager by lazy {
        DefaultSecureSecretManager(context)
    }

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val historyRepository: HistoryRepository by lazy {
        RoomHistoryRepository(database.historyDao())
    }

    override val favoritesRepository: FavoritesRepository by lazy {
        RoomFavoritesRepository(database.favoritesDao())
    }

    val languageIdentifier: LanguageIdentifier by lazy {
        MlKitLanguageIdentifier()
    }

    override val translationService: TranslationService by lazy {
        MlKitTranslationService()
    }

    override val languageRepository: LanguageRepository by lazy {
        DefaultLanguageRepository(languageIdentifier, translationService)
    }

    override val translationRepository: TranslationRepository by lazy {
        DefaultTranslationRepository(languageIdentifier, translationService, languageRepository)
    }

    override val ocrRepository: OcrRepository by lazy {
        DefaultOcrRepository()
    }

    override val imageTranslationRepository: ImageTranslationRepository by lazy {
        DefaultImageTranslationRepository(
            ocrRepository = ocrRepository,
            translationRepository = translationRepository,
            languageRepository = languageRepository,
            languageIdentifier = languageIdentifier,
            historyRepository = historyRepository
        )
    }

    override val cameraRepository: CameraRepository by lazy {
        DefaultCameraRepository(context)
    }

    override val speechRepository: SpeechRecognitionRepository by lazy {
        DefaultSpeechRecognitionRepository(context)
    }

    override val ttsRepository: TextToSpeechRepository by lazy {
        DefaultTextToSpeechRepository(context)
    }

    override val conversationRepository: ConversationRepository by lazy {
        DefaultConversationRepository()
    }

    override val travelRepository: TravelRepository by lazy {
        DefaultTravelRepository()
    }

    override val offlinePackageRepository: OfflinePackageRepository by lazy {
        DefaultOfflinePackageRepository(translationService)
    }

    override val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(context)
    }

    override val connectivityMonitor: NetworkConnectivityMonitor by lazy {
        DefaultNetworkConnectivityMonitor(context)
    }
}
