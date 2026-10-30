package com.example.core.result

/**
 * Comprehensive domain error representations across all Vision Translator modules.
 */
sealed class DomainError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class NetworkError(
        override val message: String = "Network connection is unavailable. Switched to offline translation.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class ModelNotDownloadedError(
        val languageCode: String,
        override val message: String = "Language pack '$languageCode' is not downloaded for offline use.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class PermissionDeniedError(
        val permission: String,
        override val message: String = "Permission required to use this feature: $permission",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class OcrProcessingError(
        override val message: String = "Failed to recognize text from the image.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class SpeechRecognitionError(
        val errorCode: Int? = null,
        override val message: String = "Speech recognition was unable to capture audio.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class TtsUnavailableError(
        val languageCode: String,
        override val message: String = "Text-to-speech voice not installed for '$languageCode'.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class SerializationError(
        override val message: String = "Failed to serialize or parse data.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class DatabaseError(
        override val message: String = "Local storage operation failed.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class TimeoutError(
        override val message: String = "Operation timed out. Please try again.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class LanguageNotSupportedError(
        override val message: String = "Selected language is not supported.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class TranslationError(
        override val message: String = "Translation operation failed.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    data class UnknownError(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)
}
