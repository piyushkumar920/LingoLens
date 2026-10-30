package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.permission.PermissionManager
import com.example.core.result.AppResult
import com.example.modules.favorites.model.FavoritePhrase
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationMode
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.settings.repository.SettingsRepository
import com.example.modules.speech.repository.SpeechRecognitionRepository
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.repository.TranslationRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.animation.AnimatedListeningOrb
import com.example.ui.animation.smoothButtonPress
import com.example.ui.components.LanguageSelectorBar
import com.example.ui.components.PermissionRequiredView
import com.example.ui.components.TtsAudioPlayerStrip
import com.example.ui.components.TtsSpeechButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceScreen(
    languageRepository: LanguageRepository,
    speechRepository: SpeechRecognitionRepository,
    translationRepository: TranslationRepository,
    ttsRepository: TextToSpeechRepository? = null,
    favoritesRepository: FavoritesRepository? = null,
    historyRepository: HistoryRepository? = null,
    settingsRepository: SettingsRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var hasAudioPermission by remember {
        mutableStateOf(PermissionManager.hasAudioPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    val languages by languageRepository.getSupportedLanguages().collectAsState(initial = emptyList())
    val userSettings by (settingsRepository?.settings?.collectAsState(initial = null) ?: remember { mutableStateOf(null) })

    var sourceLang by remember { mutableStateOf(Language.AUTO_DETECT) }
    var targetLang by remember { mutableStateOf(Language("es", "Spanish", "Español", "🇪🇸")) }

    val recognitionState by speechRepository.recognitionState.collectAsState()

    var transcribedSpeech by remember { mutableStateOf("") }
    var translatedResult by remember { mutableStateOf("") }
    var identifiedLanguage by remember { mutableStateOf<Language?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    // Clean up speech recognition on screen exit
    DisposableEffect(Unit) {
        onDispose {
            speechRepository.stopListening()
            ttsRepository?.stop()
        }
    }

    // React to final speech recognition result
    LaunchedEffect(recognitionState.finalResult) {
        val result = recognitionState.finalResult.trim()
        if (result.isNotBlank() && result != transcribedSpeech) {
            transcribedSpeech = result
            translationError = null
            isTranslating = true

            scope.launch {
                val effectiveSourceCode = if (sourceLang.isAuto) {
                    when (val idRes = languageRepository.detectLanguage(result)) {
                        is AppResult.Success -> {
                            val detected = idRes.data
                            identifiedLanguage = detected
                            detected.code
                        }
                        else -> "en"
                    }
                } else {
                    identifiedLanguage = null
                    sourceLang.code
                }

                val translationReq = TranslationRequest(
                    sourceText = result,
                    sourceLanguageCode = effectiveSourceCode,
                    targetLanguageCode = targetLang.code
                )

                when (val transRes = translationRepository.translate(translationReq)) {
                    is AppResult.Success -> {
                        val outputText = transRes.data.translatedText
                        translatedResult = outputText
                        isTranslating = false

                        // Check favorite state
                        favoritesRepository?.let { favRepo ->
                            isFavorite = favRepo.isFavorite(result, targetLang.code)
                        }

                        // Save to history
                        historyRepository?.let { histRepo ->
                            histRepo.insertHistoryItem(
                                TranslationHistoryItem(
                                    sourceText = result,
                                    translatedText = outputText,
                                    sourceLanguage = effectiveSourceCode,
                                    targetLanguage = targetLang.code,
                                    mode = TranslationMode.VOICE
                                )
                            )
                        }

                        // Auto-play TTS if enabled in user settings
                        if (userSettings?.autoPlayTts == true && ttsRepository != null) {
                            ttsRepository.speak(
                                text = outputText,
                                languageCode = targetLang.code
                            )
                        }
                    }
                    is AppResult.Error -> {
                        isTranslating = false
                        translationError = transRes.error.message
                    }
                    is AppResult.Loading -> {
                        // In progress
                    }
                }
            }
        }
    }

    if (!hasAudioPermission) {
        PermissionRequiredView(
            permissionName = "Microphone",
            rationale = "Vision Translator requires microphone permission to listen to speech, transcribe live audio, and translate in real time.",
            onRequestPermission = {
                permissionLauncher.launch(PermissionManager.RECORD_AUDIO_PERMISSION)
            },
            modifier = modifier
        )
        return
    }

    val displayPartial = recognitionState.partialText.ifBlank { transcribedSpeech }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("voice_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Language Selector Bar
        if (languages.isNotEmpty()) {
            LanguageSelectorBar(
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                availableLanguages = languages,
                onSourceLanguageChanged = { newSource ->
                    sourceLang = newSource
                    if (transcribedSpeech.isNotBlank()) {
                        // Re-translate with new source
                        triggerManualTranslation(
                            text = transcribedSpeech,
                            source = newSource,
                            target = targetLang,
                            languageRepository = languageRepository,
                            translationRepository = translationRepository,
                            onStart = { isTranslating = true; translationError = null },
                            onSuccess = { res, detected ->
                                translatedResult = res
                                isTranslating = false
                                identifiedLanguage = detected
                            },
                            onError = { err ->
                                isTranslating = false
                                translationError = err
                            }
                        )
                    }
                },
                onTargetLanguageChanged = { newTarget ->
                    targetLang = newTarget
                    if (transcribedSpeech.isNotBlank()) {
                        // Re-translate with new target
                        triggerManualTranslation(
                            text = transcribedSpeech,
                            source = sourceLang,
                            target = newTarget,
                            languageRepository = languageRepository,
                            translationRepository = translationRepository,
                            onStart = { isTranslating = true; translationError = null },
                            onSuccess = { res, detected ->
                                translatedResult = res
                                isTranslating = false
                                identifiedLanguage = detected
                            },
                            onError = { err ->
                                isTranslating = false
                                translationError = err
                            }
                        )
                    }
                },
                onSwapLanguages = {
                    val (newSource, newTarget) = languageRepository.swapLanguages(sourceLang, targetLang)
                    sourceLang = newSource
                    targetLang = newTarget
                    if (translatedResult.isNotBlank()) {
                        val temp = transcribedSpeech
                        transcribedSpeech = translatedResult
                        translatedResult = temp
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unavailable Recognizer Banner
            if (recognitionState.isUnavailable) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Speech recognition service is not available on this device. Please check system speech settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Speech Transcript Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_transcript_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (sourceLang.isAuto) {
                                    identifiedLanguage?.let { "${it.flagEmoji} Detected (${it.name})" }
                                        ?: "🌐 Auto-Detect"
                                } else {
                                    "${sourceLang.flagEmoji} ${sourceLang.name}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (recognitionState.isListening) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        if (transcribedSpeech.isNotBlank() && ttsRepository != null) {
                            val speechLang = identifiedLanguage?.code ?: if (sourceLang.isAuto) "en" else sourceLang.code
                            TtsSpeechButton(
                                text = transcribedSpeech,
                                languageCode = speechLang,
                                ttsRepository = ttsRepository,
                                size = 36
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recognitionState.isListening && recognitionState.partialText.isBlank() && transcribedSpeech.isBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            AnimatedListeningOrb(
                                isListening = true,
                                soundLevelDb = recognitionState.soundLevelDb,
                                size = 46.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Hearing your voice...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Speak clearly into the microphone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else if (displayPartial.isNotBlank()) {
                        Text(
                            text = displayPartial,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        )
                    } else {
                        Text(
                            text = "Tap the microphone below and speak naturally in ${if (sourceLang.isAuto) "any language" else sourceLang.name}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }

                    if (displayPartial.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Recognized Speech", displayPartial))
                                    Toast.makeText(context, "Original speech copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy original transcript",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Translation Result Card
            if (isTranslating || translatedResult.isNotBlank() || translationError != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_translation_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${targetLang.flagEmoji} ${targetLang.name}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "On-Device ML Kit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Favorite Button
                                if (translatedResult.isNotBlank() && favoritesRepository != null) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                if (isFavorite) {
                                                    favoritesRepository.removeFavoriteByText(transcribedSpeech, targetLang.code)
                                                    isFavorite = false
                                                    Toast.makeText(context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    favoritesRepository.saveFavorite(
                                                        FavoritePhrase(
                                                            sourceText = transcribedSpeech,
                                                            translatedText = translatedResult,
                                                            sourceLanguage = sourceLang.code,
                                                            targetLanguage = targetLang.code,
                                                            category = "Voice"
                                                        )
                                                    )
                                                    isFavorite = true
                                                    Toast.makeText(context, "Saved to Favorites", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = if (isFavorite) "Remove from favorites" else "Save to favorites",
                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                if (translatedResult.isNotBlank() && ttsRepository != null) {
                                    TtsSpeechButton(
                                        text = translatedResult,
                                        languageCode = targetLang.code,
                                        ttsRepository = ttsRepository,
                                        size = 36
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isTranslating) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Translating speech...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else if (translationError != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = translationError ?: "Translation failed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (transcribedSpeech.isNotBlank()) {
                                            triggerManualTranslation(
                                                text = transcribedSpeech,
                                                source = sourceLang,
                                                target = targetLang,
                                                languageRepository = languageRepository,
                                                translationRepository = translationRepository,
                                                onStart = { isTranslating = true; translationError = null },
                                                onSuccess = { res, detected ->
                                                    translatedResult = res
                                                    isTranslating = false
                                                    identifiedLanguage = detected
                                                },
                                                onError = { err ->
                                                    isTranslating = false
                                                    translationError = err
                                                }
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        } else if (translatedResult.isNotBlank()) {
                            Text(
                                text = translatedResult,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // TTS audio playback controls strip
                            if (ttsRepository != null) {
                                TtsAudioPlayerStrip(
                                    text = translatedResult,
                                    languageCode = targetLang.code,
                                    languageName = targetLang.name,
                                    ttsRepository = ttsRepository,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            // Actions Bar: Copy, Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Translated Speech", translatedResult))
                                        Toast.makeText(context, "Translation copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Translation",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "🗣️ $transcribedSpeech\n🌐 $translatedResult (${targetLang.name})"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Voice Translation"))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share Translation",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error or Silence Banner
            if (recognitionState.error != null && !recognitionState.isListening) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = recognitionState.error ?: "Recognition error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val langCode = if (sourceLang.isAuto) "en-US" else sourceLang.code
                                speechRepository.startListening(langCode)
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry Voice Input",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Quick Starter Phrases when screen is idle
            if (transcribedSpeech.isBlank() && !recognitionState.isListening) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Try saying something like:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = listOf(
                        "Where is the nearest train station?",
                        "How much does this cost?",
                        "Can you please help me with directions?",
                        "Is there a vegetarian option on the menu?"
                    )
                    suggestions.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .smoothButtonPress(scaleDown = 0.94f)
                                .clickable {
                                    transcribedSpeech = suggestion
                                triggerManualTranslation(
                                    text = suggestion,
                                    source = sourceLang,
                                    target = targetLang,
                                    languageRepository = languageRepository,
                                    translationRepository = translationRepository,
                                    onStart = { isTranslating = true; translationError = null },
                                    onSuccess = { res, detected ->
                                        translatedResult = res
                                        isTranslating = false
                                        identifiedLanguage = detected
                                    },
                                    onError = { err ->
                                        isTranslating = false
                                        translationError = err
                                    }
                                )
                            }
                        ) {
                            Text(
                                text = "“$suggestion”",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Live RMS Sound Wave / Decibel visualizer bar
        if (recognitionState.isListening) {
            com.example.ui.animation.OrganicListeningVisualizer(
                soundLevelDb = recognitionState.soundLevelDb,
                isListening = true,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Central Microphone Animated Hearing Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            AnimatedListeningOrb(
                isListening = recognitionState.isListening,
                soundLevelDb = recognitionState.soundLevelDb,
                size = if (recognitionState.isListening) 124.dp else 90.dp,
                coreColorStart = if (recognitionState.isListening) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary,
                coreColorMid = if (recognitionState.isListening) Color(0xFF6366F1) else MaterialTheme.colorScheme.secondary,
                coreColorEnd = if (recognitionState.isListening) Color(0xFFA855F7) else MaterialTheme.colorScheme.tertiary,
                accentGlow = if (recognitionState.isListening) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    if (recognitionState.isListening) {
                        speechRepository.stopListening()
                    } else {
                        transcribedSpeech = ""
                        translatedResult = ""
                        translationError = null
                        val langCode = if (sourceLang.isAuto) "en-US" else sourceLang.code
                        speechRepository.startListening(langCode)
                    }
                },
                modifier = Modifier.testTag("voice_record_button")
            )

            // Center status Icon inside the orb
            Icon(
                imageVector = if (recognitionState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (recognitionState.isListening) "Stop Listening" else "Start Listening",
                tint = Color.White,
                modifier = Modifier
                    .size(if (recognitionState.isListening) 36.dp else 30.dp)
            )
        }

        Text(
            text = if (recognitionState.isListening) "Listening... Tap to stop" else "Tap to speak",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (recognitionState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Animated dynamic waveform visualizer responding directly to live audio decibels.
 */
@Composable
fun LiveAudioVisualizer(
    soundLevelDb: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Row(
        modifier = modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val normalizedDb = (soundLevelDb / 12f).coerceIn(0.1f, 1f)
        for (i in 0 until barCount) {
            val phase = (i.toFloat() / barCount + waveOffset) % 1f
            val dynamicHeight = if (isListening) {
                val factor = (Math.sin(phase * Math.PI * 2).toFloat() * 0.4f + 0.6f) * normalizedDb
                (factor * 28.dp.value).coerceIn(6f, 28f)
            } else {
                4f
            }

            val animatedHeight by animateFloatAsState(
                targetValue = dynamicHeight,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "bar_height"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

private fun triggerManualTranslation(
    text: String,
    source: Language,
    target: Language,
    languageRepository: LanguageRepository,
    translationRepository: TranslationRepository,
    onStart: () -> Unit,
    onSuccess: (String, Language?) -> Unit,
    onError: (String) -> Unit
) {
    onStart()
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        val (effectiveCode, detectedLang) = if (source.isAuto) {
            when (val idRes = languageRepository.detectLanguage(text)) {
                is AppResult.Success -> {
                    val detected = idRes.data
                    detected.code to detected
                }
                else -> "en" to null
            }
        } else {
            source.code to null
        }

        val req = TranslationRequest(
            sourceText = text,
            sourceLanguageCode = effectiveCode,
            targetLanguageCode = target.code
        )

        when (val res = translationRepository.translate(req)) {
            is AppResult.Success -> onSuccess(res.data.translatedText, detectedLang)
            is AppResult.Error -> onError(res.error.message)
            is AppResult.Loading -> { /* Loading state handled */ }
        }
    }
}
