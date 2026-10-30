package com.example.modules.offline.model

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

enum class OfflineStorageLocation {
    INTERNAL_STORAGE,
    APP_CACHE
}

data class OfflinePackage(
    val languageCode: String,
    val languageName: String,
    val flagEmoji: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val state: DownloadState,
    val progressPercent: Int = 0,
    val isCoreModel: Boolean = false,
    val offlineAvailability: String = "Complete On-Device Translation",
    val storageLocation: OfflineStorageLocation = OfflineStorageLocation.INTERNAL_STORAGE,
    val installedStorageBytes: Long = if (state == DownloadState.DOWNLOADED) sizeBytes else 0L
)
