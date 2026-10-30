package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animation.smoothButtonPress
import com.example.core.result.AppResult
import com.example.modules.favorites.model.FavoritePhrase
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationMode
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.model.TranslationResult
import com.example.modules.translation.repository.TranslationRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.components.LanguageSelectorBar
import com.example.ui.components.TtsAudioPlayerStrip
import com.example.ui.components.TtsSpeechButton
import kotlinx.coroutines.launch

/**
 * Full-featured, responsive Text Translation screen.
 *
 * Capabilities:
 * - Multi-line auto-scrolling text input supporting long documents and paragraphs
 * - Quick Clipboard Paste and Instant Clear
 * - Automatic source language detection with badge feedback
 * - Source and target language selector with rapid swap
 * - One-touch translation with ML Kit on-device neural models
 * - Prominent translation result card with formatted display
 * - Real Android Text-to-Speech audio controls (Listen, Pause, Replay, Speed, Pitch)
 * - Copy, Share, and Save-to-Favorites/History local persistence
 * - Character count & word count indicators
 * - Polished Empty, Loading, and Error states with retry
 */
@Composable
fun TextScreen(
    languageRepository: LanguageRepository,
    translationRepository: TranslationRepository,
    ttsRepository: TextToSpeechRepository? = null,
    favoritesRepository: FavoritesRepository? = null,
    historyRepository: HistoryRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val languages by languageRepository.getSupportedLanguages().collectAsState(initial = emptyList())
    val downloadedModels by translationRepository.getDownloadedModels().collectAsState()

    var sourceLang by remember { mutableStateOf(Language.AUTO_DETECT) }
    var targetLang by remember { mutableStateOf(Language("es", "Spanish", "Español", "🇪🇸")) }
    var inputText by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    var translationResult by remember { mutableStateOf<TranslationResult?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    var detectedLanguageInfo by remember { mutableStateOf<Language?>(null) }

    // Check if current text is already favorited
    LaunchedEffect(inputText, targetLang.code) {
        if (favoritesRepository != null && inputText.isNotBlank()) {
            isFavorite = favoritesRepository.isFavorite(inputText.trim(), targetLang.code)
        } else {
            isFavorite = false
        }
    }

    fun executeTranslation(textToTranslate: String = inputText) {
        val trimmed = textToTranslate.trim()
        if (trimmed.isBlank()) return

        coroutineScope.launch {
            isTranslating = true
            translationError = null
            detectedLanguageInfo = null

            val request = TranslationRequest(
                sourceText = trimmed,
                sourceLanguageCode = sourceLang.code,
                targetLanguageCode = targetLang.code
            )

            when (val result = translationRepository.translate(request)) {
                is AppResult.Success -> {
                    translationResult = result.data
                    if (sourceLang.isAuto) {
                        detectedLanguageInfo = languageRepository.findLanguageByCode(result.data.sourceLanguageCode)
                    }

                    // Save into translation history
                    historyRepository?.insertHistoryItem(
                        TranslationHistoryItem(
                            sourceText = trimmed,
                            translatedText = result.data.translatedText,
                            sourceLanguage = result.data.sourceLanguageCode,
                            targetLanguage = result.data.targetLanguageCode,
                            mode = TranslationMode.TEXT,
                            isFavorite = isFavorite
                        )
                    )
                }
                is AppResult.Error -> {
                    translationError = result.error.message
                }
                is AppResult.Loading -> { /* no-op */ }
            }
            isTranslating = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("text_screen")
    ) {
        // 1. Language Selector Bar with Search, Swap & Auto-Detect
        if (languages.isNotEmpty()) {
            LanguageSelectorBar(
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                availableLanguages = languages,
                languageRepository = languageRepository,
                translationRepository = translationRepository,
                onSourceLanguageChanged = {
                    sourceLang = it
                    translationResult = null
                    translationError = null
                    if (inputText.isNotBlank()) {
                        executeTranslation()
                    }
                },
                onTargetLanguageChanged = {
                    targetLang = it
                    translationResult = null
                    translationError = null
                    if (inputText.isNotBlank()) {
                        executeTranslation()
                    }
                },
                onSwapLanguages = {
                    val (newSource, newTarget) = languageRepository.swapLanguages(sourceLang, targetLang)
                    sourceLang = newSource
                    targetLang = newTarget
                    val prevResult = translationResult
                    if (prevResult != null) {
                        inputText = prevResult.translatedText
                        executeTranslation(prevResult.translatedText)
                    } else if (inputText.isNotBlank()) {
                        executeTranslation()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Text Input Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().testTag("text_input_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Input header: Label, Detected Badge, Paste & Clear actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = if (sourceLang.isAuto) "Source Text (Auto Detect)" else "Source Text (${sourceLang.name})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (detectedLanguageInfo != null && sourceLang.isAuto) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Detected: ${detectedLanguageInfo?.flagEmoji} ${detectedLanguageInfo?.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Paste Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        inputText = text
                                        executeTranslation(text)
                                        Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .smoothButtonPress(scaleDown = 0.88f)
                                .testTag("paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear Button
                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    inputText = ""
                                    translationResult = null
                                    translationError = null
                                    detectedLanguageInfo = null
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .smoothButtonPress(scaleDown = 0.88f)
                                    .testTag("clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Multi-line Text Field Supporting Long Text
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        if (it.isBlank()) {
                            translationResult = null
                            translationError = null
                        }
                    },
                    placeholder = {
                        Text(
                            "Type or paste text in any language to translate...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp, max = 280.dp)
                        .testTag("text_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom bar of input card: Word/Character count + TTS speak + Translate button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wordCount = if (inputText.isBlank()) 0 else inputText.trim().split("\\s+".toRegex()).size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${inputText.length} chars · $wordCount words",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.testTag("character_count_label")
                        )
                        if (inputText.isNotBlank() && ttsRepository != null) {
                            TtsSpeechButton(
                                text = inputText,
                                languageCode = if (sourceLang.isAuto) (detectedLanguageInfo?.code ?: "en") else sourceLang.code,
                                ttsRepository = ttsRepository,
                                size = 28,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = { executeTranslation() },
                        enabled = inputText.isNotBlank() && !isTranslating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .smoothButtonPress(scaleDown = 0.94f)
                            .testTag("translate_button")
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translating...")
                        } else {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translate")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Loading, Error, Result, or Empty State Section
        when {
            // Loading State
            isTranslating -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("loading_state_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Translating text...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Processing via on-device ML Kit neural model",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error State
            translationError != null -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("translation_error_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Translation Failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = translationError ?: "Failed to complete translation. Please check model download or connection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { executeTranslation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("retry_translation_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Translation")
                            }
                        }
                    }
                }
            }

            // Translation Result Available
            translationResult != null -> {
                val result = translationResult!!
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("translation_result_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Result Header: Language Tag, On-device Badge & Action Icons (Speak, Star, Copy, Share, Save)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Translation (${targetLang.name})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (result.isOffline) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Offline ML Kit",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Speak Button
                                if (ttsRepository != null) {
                                    TtsSpeechButton(
                                        text = result.translatedText,
                                        languageCode = targetLang.code,
                                        ttsRepository = ttsRepository,
                                        size = 32,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Favorite / Star Button
                                IconButton(
                                    onClick = {
                                        if (favoritesRepository != null) {
                                            coroutineScope.launch {
                                                if (isFavorite) {
                                                    favoritesRepository.removeFavoriteByText(inputText.trim(), targetLang.code)
                                                    isFavorite = false
                                                    Toast.makeText(context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val fav = FavoritePhrase(
                                                        sourceText = inputText.trim(),
                                                        translatedText = result.translatedText,
                                                        sourceLanguage = result.sourceLanguageCode,
                                                        targetLanguage = result.targetLanguageCode,
                                                        category = "Text Translation"
                                                    )
                                                    favoritesRepository.saveFavorite(fav)
                                                    isFavorite = true
                                                    Toast.makeText(context, "Saved to Favorites & History", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .smoothButtonPress(scaleDown = 0.88f)
                                        .testTag("favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite Translation",
                                        tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Copy Button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Translation", result.translatedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Translation copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .smoothButtonPress(scaleDown = 0.88f)
                                        .testTag("copy_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Translation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Share Button
                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "${inputText.trim()}\n\n↳ ${result.translatedText}")
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Translation")
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .smoothButtonPress(scaleDown = 0.88f)
                                        .testTag("share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Translation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Translated Text
                        Text(
                            text = result.translatedText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("translation_output_text")
                        )

                    }
                }

                // Interactive TTS Player Strip
                if (ttsRepository != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TtsAudioPlayerStrip(
                        text = result.translatedText,
                        languageCode = targetLang.code,
                        languageName = targetLang.name,
                        ttsRepository = ttsRepository
                    )
                }
            }

            // Empty State with Quick Conversation Starter Phrases
            else -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("empty_state_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Quick Starter Phrases",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap any common phrase to test instant translation:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val starterPhrases = listOf(
                            "Hello! Where is the nearest train station?",
                            "Could you please help me with directions?",
                            "How much does this ticket cost?",
                            "Thank you very much for your kind help!",
                            "Do you speak English?"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            starterPhrases.forEach { phrase ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputText = phrase
                                            executeTranslation(phrase)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = phrase,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Feature Highlights Footer Pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Instant Offline Neural Engine",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Translates securely on-device with ML Kit. Download language packs in Settings for zero-latency offline translations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

