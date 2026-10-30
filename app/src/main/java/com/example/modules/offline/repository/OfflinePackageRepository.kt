package com.example.modules.offline.repository

import com.example.core.result.AppResult
import com.example.modules.offline.model.DownloadState
import com.example.modules.offline.model.OfflinePackage
import com.example.modules.translation.service.TranslationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface OfflinePackageRepository {
    val packages: StateFlow<List<OfflinePackage>>
    val totalStorageUsedFormatted: String
    val totalStorageUsedBytes: Long
    suspend fun downloadPackage(languageCode: String): AppResult<Unit>
    suspend fun deletePackage(languageCode: String): AppResult<Unit>
    fun isPackageDownloaded(languageCode: String): Boolean
    fun syncWithMlKit()
}

class DefaultOfflinePackageRepository(
    private val translationService: TranslationService? = null
) : OfflinePackageRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val initialPacks = listOf(
        OfflinePackage("en", "English", "🇺🇸", 32_000_000L, "32 MB", DownloadState.DOWNLOADED, 100, isCoreModel = true, offlineAvailability = "Core System Dictionary & Translator"),
        OfflinePackage("es", "Spanish", "🇪🇸", 45_000_000L, "45 MB", DownloadState.DOWNLOADED, 100, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("fr", "French", "🇫🇷", 44_000_000L, "44 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("de", "German", "🇩🇪", 48_000_000L, "48 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("ja", "Japanese", "🇯🇵", 62_000_000L, "62 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Kanji & Kana Offline Neural Model"),
        OfflinePackage("zh", "Chinese (Simplified)", "🇨🇳", 70_000_000L, "70 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Simplified Hanzi Offline Neural Model"),
        OfflinePackage("it", "Italian", "🇮🇹", 41_000_000L, "41 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("pt", "Portuguese", "🇧🇷", 43_000_000L, "43 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("ko", "Korean", "🇰🇷", 58_000_000L, "58 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Hangul Offline Neural Model"),
        OfflinePackage("ru", "Russian", "🇷🇺", 52_000_000L, "52 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Cyrillic Offline Neural Model"),
        OfflinePackage("ar", "Arabic", "🇸🇦", 55_000_000L, "55 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Arabic Script Offline Neural Model"),
        OfflinePackage("hi", "Hindi", "🇮🇳", 50_000_000L, "50 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Devanagari Offline Neural Model"),
        OfflinePackage("vi", "Vietnamese", "🇻🇳", 46_000_000L, "46 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("tr", "Turkish", "🇹🇷", 42_000_000L, "42 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("nl", "Dutch", "🇳🇱", 40_000_000L, "40 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model"),
        OfflinePackage("pl", "Polish", "🇵🇱", 44_000_000L, "44 MB", DownloadState.NOT_DOWNLOADED, 0, offlineAvailability = "Full Offline ML Kit Neural Model")
    )

    private val _packages = MutableStateFlow(initialPacks)
    override val packages: StateFlow<List<OfflinePackage>> = _packages.asStateFlow()

    override val totalStorageUsedBytes: Long
        get() = _packages.value.filter { it.state == DownloadState.DOWNLOADED }.sumOf { it.sizeBytes }

    override val totalStorageUsedFormatted: String
        get() {
            val mb = totalStorageUsedBytes / 1_000_000L
            return "$mb MB"
        }

    init {
        syncWithMlKit()
    }

    override fun syncWithMlKit() {
        if (translationService == null) return
        scope.launch {
            translationService.getDownloadedModels().collect { downloadedCodes ->
                _packages.update { list ->
                    list.map { pkg ->
                        if (downloadedCodes.contains(pkg.languageCode) || pkg.isCoreModel) {
                            pkg.copy(
                                state = DownloadState.DOWNLOADED,
                                progressPercent = 100,
                                installedStorageBytes = pkg.sizeBytes
                            )
                        } else if (pkg.state == DownloadState.DOWNLOADED && !pkg.isCoreModel) {
                            // If it was marked downloaded in ML Kit, keep synced
                            pkg
                        } else {
                            pkg
                        }
                    }
                }
            }
        }
    }

    override suspend fun downloadPackage(languageCode: String): AppResult<Unit> {
        // Update state to DOWNLOADING
        _packages.update { list ->
            list.map { pkg ->
                if (pkg.languageCode == languageCode) {
                    pkg.copy(state = DownloadState.DOWNLOADING, progressPercent = 45)
                } else pkg
            }
        }

        val result = translationService?.downloadModel(languageCode, requireWifi = false)
            ?: AppResult.Success(Unit)

        return when (result) {
            is AppResult.Success -> {
                _packages.update { list ->
                    list.map { pkg ->
                        if (pkg.languageCode == languageCode) {
                            pkg.copy(
                                state = DownloadState.DOWNLOADED,
                                progressPercent = 100,
                                installedStorageBytes = pkg.sizeBytes
                            )
                        } else pkg
                    }
                }
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                _packages.update { list ->
                    list.map { pkg ->
                        if (pkg.languageCode == languageCode) {
                            pkg.copy(state = DownloadState.FAILED, progressPercent = 0)
                        } else pkg
                    }
                }
                result
            }
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override suspend fun deletePackage(languageCode: String): AppResult<Unit> {
        // Protect core model
        if (languageCode == "en") {
            return AppResult.Success(Unit)
        }

        translationService?.deleteModel(languageCode)

        _packages.update { list ->
            list.map { pkg ->
                if (pkg.languageCode == languageCode) {
                    pkg.copy(
                        state = DownloadState.NOT_DOWNLOADED,
                        progressPercent = 0,
                        installedStorageBytes = 0L
                    )
                } else pkg
            }
        }
        return AppResult.Success(Unit)
    }

    override fun isPackageDownloaded(languageCode: String): Boolean {
        return _packages.value.find { it.languageCode == languageCode }?.state == DownloadState.DOWNLOADED
    }
}
