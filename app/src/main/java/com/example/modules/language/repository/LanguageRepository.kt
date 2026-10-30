package com.example.modules.language.repository

import com.example.core.result.AppResult
import com.example.modules.language.identifier.LanguageIdentifier
import com.example.modules.language.model.Language
import com.example.modules.translation.service.TranslationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface LanguageRepository {
    fun getSupportedLanguages(): Flow<List<Language>>
    val recentLanguages: StateFlow<List<Language>>
    val favoriteLanguages: StateFlow<List<Language>>

    suspend fun findLanguageByCode(code: String): Language?
    suspend fun detectLanguage(text: String): AppResult<Language>
    fun markLanguageUsed(language: Language)
    fun toggleFavorite(language: Language)
    fun searchLanguages(query: String, includeAuto: Boolean = false): List<Language>
    fun swapLanguages(source: Language, target: Language): Pair<Language, Language>
}

class DefaultLanguageRepository(
    private val languageIdentifier: LanguageIdentifier,
    private val translationService: TranslationService
) : LanguageRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Master list of 55+ world languages
    private val allLanguages = listOf(
        Language("en", "English", "English", "🇺🇸", script = "Latin", supportsOffline = true),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳", script = "Devanagari", supportsOffline = true),
        Language("ja", "Japanese", "日本語", "🇯🇵", script = "Japanese", supportsOffline = true),
        Language("zh", "Chinese (Simplified)", "简体中文", "🇨🇳", script = "Chinese", supportsOffline = true),
        Language("es", "Spanish", "Español", "🇪🇸", script = "Latin", supportsOffline = true),
        Language("fr", "French", "Français", "🇫🇷", script = "Latin", supportsOffline = true),
        Language("de", "German", "Deutsch", "🇩🇪", script = "Latin", supportsOffline = true),
        Language("it", "Italian", "Italiano", "🇮🇹", script = "Latin", supportsOffline = true),
        Language("pt", "Portuguese", "Português", "🇧🇷", script = "Latin", supportsOffline = true),
        Language("ko", "Korean", "한국어", "🇰🇷", script = "Korean", supportsOffline = true),
        Language("ru", "Russian", "Русский", "🇷🇺", script = "Cyrillic", supportsOffline = true),
        Language("ar", "Arabic", "العربية", "🇸🇦", script = "Arabic", supportsOffline = true),
        Language("tr", "Turkish", "Türkçe", "🇹🇷", script = "Latin", supportsOffline = true),
        Language("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", script = "Latin", supportsOffline = true),
        Language("nl", "Dutch", "Nederlands", "🇳🇱", script = "Latin", supportsOffline = true),
        Language("pl", "Polish", "Polski", "🇵🇱", script = "Latin", supportsOffline = true),
        Language("sv", "Swedish", "Svenska", "🇸🇪", script = "Latin", supportsOffline = true),
        Language("th", "Thai", "ไทย", "🇹🇭", script = "Thai", supportsOffline = true),
        Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", script = "Latin", supportsOffline = true),
        Language("el", "Greek", "Ελληνικά", "🇬🇷", script = "Greek", supportsOffline = true),
        Language("he", "Hebrew", "עברית", "🇮🇱", script = "Hebrew", supportsOffline = true),
        Language("cs", "Czech", "Čeština", "🇨🇿", script = "Latin", supportsOffline = true),
        Language("da", "Danish", "Dansk", "🇩🇰", script = "Latin", supportsOffline = true),
        Language("fi", "Finnish", "Suomi", "🇫🇮", script = "Latin", supportsOffline = true),
        Language("hu", "Hungarian", "Magyar", "🇭🇺", script = "Latin", supportsOffline = true),
        Language("no", "Norwegian", "Norsk", "🇳🇴", script = "Latin", supportsOffline = true),
        Language("ro", "Romanian", "Română", "🇷🇴", script = "Latin", supportsOffline = true),
        Language("sk", "Slovak", "Slovenčina", "🇸🇰", script = "Latin", supportsOffline = true),
        Language("uk", "Ukrainian", "Українська", "🇺🇦", script = "Cyrillic", supportsOffline = true),
        Language("ms", "Malay", "Bahasa Melayu", "🇲🇾", script = "Latin", supportsOffline = true),
        Language("bn", "Bengali", "বাংলা", "🇧🇩", script = "Bengali", supportsOffline = true),
        Language("ta", "Tamil", "தமிழ்", "🇮🇳", script = "Tamil", supportsOffline = true),
        Language("te", "Telugu", "తెలుగు", "🇮🇳", script = "Telugu", supportsOffline = true),
        Language("mr", "Marathi", "मराठी", "🇮🇳", script = "Devanagari", supportsOffline = true),
        Language("ur", "Urdu", "اردو", "🇵🇰", script = "Arabic", supportsOffline = true),
        Language("fa", "Persian", "فارسی", "🇮🇷", script = "Arabic", supportsOffline = true),
        Language("sw", "Swahili", "Kiswahili", "🇰🇪", script = "Latin", supportsOffline = true),
        Language("tl", "Tagalog (Filipino)", "Tagalog", "🇵🇭", script = "Latin", supportsOffline = true)
    )

    private val _favoriteCodes = MutableStateFlow(setOf("en", "hi", "ja", "es", "zh"))
    private val _recentCodes = MutableStateFlow(listOf("en", "hi", "ja", "es", "fr"))

    private val _recentLanguages = MutableStateFlow<List<Language>>(emptyList())
    override val recentLanguages: StateFlow<List<Language>> = _recentLanguages.asStateFlow()

    private val _favoriteLanguages = MutableStateFlow<List<Language>>(emptyList())
    override val favoriteLanguages: StateFlow<List<Language>> = _favoriteLanguages.asStateFlow()

    init {
        updateDerivedLanguageLists()
    }

    private fun updateDerivedLanguageLists() {
        val downloaded = translationService.getDownloadedModels().value
        val favorites = _favoriteCodes.value

        val mapped = allLanguages.map { lang ->
            lang.copy(
                isDownloadedOffline = downloaded.contains(lang.code),
                isFavorite = favorites.contains(lang.code)
            )
        }

        _favoriteLanguages.value = mapped.filter { it.isFavorite }
        _recentLanguages.value = _recentCodes.value.mapNotNull { code -> mapped.find { it.code == code } }
    }

    override fun getSupportedLanguages(): Flow<List<Language>> {
        return combine(
            translationService.getDownloadedModels(),
            _favoriteCodes
        ) { downloaded, favorites ->
            allLanguages.map { lang ->
                lang.copy(
                    isDownloadedOffline = downloaded.contains(lang.code),
                    isFavorite = favorites.contains(lang.code)
                )
            }
        }
    }

    override suspend fun findLanguageByCode(code: String): Language? {
        if (code.equals(Language.AUTO_CODE, ignoreCase = true)) {
            return Language.AUTO_DETECT
        }
        val downloaded = translationService.getDownloadedModels().value
        val favorites = _favoriteCodes.value
        val found = allLanguages.find { it.code.equals(code, ignoreCase = true) } ?: return null
        return found.copy(
            isDownloadedOffline = downloaded.contains(found.code),
            isFavorite = favorites.contains(found.code)
        )
    }

    override suspend fun detectLanguage(text: String): AppResult<Language> {
        return when (val idResult = languageIdentifier.identifyLanguage(text)) {
            is AppResult.Success -> {
                val detected = findLanguageByCode(idResult.data) ?: Language(
                    code = idResult.data,
                    name = idResult.data.uppercase(),
                    nativeName = idResult.data.uppercase(),
                    flagEmoji = "🌐"
                )
                AppResult.Success(detected)
            }
            is AppResult.Error -> AppResult.Error(idResult.error)
            is AppResult.Loading -> AppResult.Loading
        }
    }

    override fun markLanguageUsed(language: Language) {
        if (language.isAuto) return
        _recentCodes.update { current ->
            (listOf(language.code) + current.filter { it != language.code }).take(6)
        }
        updateDerivedLanguageLists()
    }

    override fun toggleFavorite(language: Language) {
        if (language.isAuto) return
        _favoriteCodes.update { current ->
            if (current.contains(language.code)) current - language.code
            else current + language.code
        }
        updateDerivedLanguageLists()
    }

    override fun searchLanguages(query: String, includeAuto: Boolean): List<Language> {
        val downloaded = translationService.getDownloadedModels().value
        val favorites = _favoriteCodes.value

        val baseList = if (includeAuto) listOf(Language.AUTO_DETECT) + allLanguages else allLanguages
        val enriched = baseList.map { lang ->
            lang.copy(
                isDownloadedOffline = downloaded.contains(lang.code),
                isFavorite = favorites.contains(lang.code)
            )
        }

        if (query.isBlank()) return enriched

        val q = query.trim().lowercase()
        return enriched.filter {
            it.name.lowercase().contains(q) ||
            it.nativeName.lowercase().contains(q) ||
            it.code.lowercase().contains(q) ||
            it.script.lowercase().contains(q)
        }
    }

    override fun swapLanguages(source: Language, target: Language): Pair<Language, Language> {
        return if (source.isAuto) {
            // When source is auto, target becomes source, and fallback (e.g. English) becomes target
            val newSource = target
            val newTarget = if (target.code == "en") findLanguageSync("es") else findLanguageSync("en")
            Pair(newSource, newTarget)
        } else {
            Pair(target, source)
        }
    }

    private fun findLanguageSync(code: String): Language {
        return allLanguages.find { it.code == code } ?: allLanguages.first()
    }
}
