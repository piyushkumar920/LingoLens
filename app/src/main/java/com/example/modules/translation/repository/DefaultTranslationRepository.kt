package com.example.modules.translation.repository

import android.util.Log
import com.example.core.result.AppResult
import com.example.core.result.DomainError
import com.example.modules.language.identifier.LanguageIdentifier
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.translation.model.TranslationEngine
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.model.TranslationResult
import com.example.modules.translation.service.TranslationService
import kotlinx.coroutines.flow.StateFlow

/**
 * Production implementation of TranslationRepository coordinating Language Identification and Translation Service.
 */
class DefaultTranslationRepository(
    private val languageIdentifier: LanguageIdentifier,
    private val translationService: TranslationService,
    private val languageRepository: LanguageRepository
) : TranslationRepository {

    private val tag = "DefaultTranslationRepo"

    override suspend fun translate(request: TranslationRequest): AppResult<TranslationResult> {
        val trimmed = request.sourceText.trim()
        if (trimmed.isBlank()) {
            return AppResult.Error(DomainError.UnknownError("Source text cannot be empty."))
        }

        // 1. Resolve effective source language code (Auto-Detection vs Explicit)
        val langStart = System.currentTimeMillis()
        val effectiveSourceCode: String = if (request.sourceLanguageCode.equals(Language.AUTO_CODE, ignoreCase = true)) {
            when (val idResult = languageIdentifier.identifyLanguage(trimmed)) {
                is AppResult.Success -> {
                    val detected = idResult.data
                    val langDuration = System.currentTimeMillis() - langStart
                    Log.d("VISION_LANGUAGE", "Language ID duration: ${langDuration}ms for text: '${trimmed.take(20)}' (detected: $detected)")
                    if (detected.isBlank() || detected.equals("und", ignoreCase = true)) {
                        return AppResult.Error(
                            DomainError.LanguageNotSupportedError("Couldn't detect the language. Please select source language manually.")
                        )
                    }
                    Log.d(tag, "Auto-detected source language as: $detected")
                    detected
                }
                is AppResult.Error -> {
                    return AppResult.Error(
                        DomainError.LanguageNotSupportedError("Couldn't detect the language. Please select source language manually.")
                    )
                }
                is AppResult.Loading -> {
                    return AppResult.Error(
                        DomainError.LanguageNotSupportedError("Couldn't detect the language. Please select source language manually.")
                    )
                }
            }
        } else {
            if (request.sourceLanguageCode.isBlank() || request.sourceLanguageCode.equals("und", ignoreCase = true)) {
                return AppResult.Error(
                    DomainError.LanguageNotSupportedError("Couldn't detect the language. Please select source language manually.")
                )
            }
            request.sourceLanguageCode
        }

        // 2. Validate translation engine support
        if (!translationService.isLanguageSupported(effectiveSourceCode)) {
            return AppResult.Error(
                DomainError.LanguageNotSupportedError(
                    "Source language '$effectiveSourceCode' is not supported by on-device translation."
                )
            )
        }

        if (!translationService.isLanguageSupported(request.targetLanguageCode)) {
            return AppResult.Error(
                DomainError.LanguageNotSupportedError(
                    "Target language '${request.targetLanguageCode}' is not supported by on-device translation."
                )
            )
        }

        // 3. Perform on-device translation
        val transStart = System.currentTimeMillis()
        val translationResponse = translationService.translate(trimmed, effectiveSourceCode, request.targetLanguageCode)
        val transDuration = System.currentTimeMillis() - transStart
        Log.d("VISION_TRANSLATION", "Translation duration: ${transDuration}ms for text: '${trimmed.take(20)}' ($effectiveSourceCode -> ${request.targetLanguageCode})")

        return when (val transResult = translationResponse) {
            is AppResult.Success -> {
                // Record usage for recent languages
                languageRepository.findLanguageByCode(effectiveSourceCode)?.let {
                    languageRepository.markLanguageUsed(it)
                }
                languageRepository.findLanguageByCode(request.targetLanguageCode)?.let {
                    languageRepository.markLanguageUsed(it)
                }

                val isSourceDownloaded = translationService.isModelDownloaded(effectiveSourceCode)
                val isTargetDownloaded = translationService.isModelDownloaded(request.targetLanguageCode)

                AppResult.Success(
                    TranslationResult(
                        originalText = trimmed,
                        translatedText = transResult.data,
                        sourceLanguageCode = effectiveSourceCode,
                        targetLanguageCode = request.targetLanguageCode,
                        engineUsed = TranslationEngine.ON_DEVICE_ML_KIT,
                        isOffline = isSourceDownloaded && isTargetDownloaded,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            is AppResult.Error -> {
                Log.e(tag, "Translation failed: ${transResult.error}")
                AppResult.Error(transResult.error)
            }
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override suspend fun isModelDownloaded(languageCode: String): Boolean {
        return translationService.isModelDownloaded(languageCode)
    }

    override suspend fun downloadModel(languageCode: String, requireWifi: Boolean): AppResult<Unit> {
        return translationService.downloadModel(languageCode, requireWifi)
    }

    override suspend fun deleteModel(languageCode: String): AppResult<Unit> {
        return translationService.deleteModel(languageCode)
    }

    override fun getDownloadedModels(): StateFlow<Set<String>> {
        return translationService.getDownloadedModels()
    }

    override fun isLanguagePairSupported(sourceCode: String, targetCode: String): Boolean {
        return translationService.isLanguagePairSupported(sourceCode, targetCode)
    }
}
