package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.result.AppResult
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.speech.repository.SpeechRecognitionRepository
import com.example.modules.speech.repository.SpeechRecognitionState
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.repository.TranslationRepository
import com.example.ui.animation.AnimatedListeningOrb
import com.example.ui.animation.smoothButtonPress
import com.example.ui.components.MlKitLanguageSelectorDualControl
import com.example.ui.navigation.Screen
import com.example.ui.theme.LovableCard
import com.example.ui.theme.LovableCharcoal
import com.example.ui.theme.LovableCream
import com.example.ui.theme.LovableLightCream
import com.example.ui.theme.LovableOffWhite
import com.example.ui.theme.LovablePrimaryButton
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier,
    languageRepository: LanguageRepository? = null,
    translationRepository: TranslationRepository? = null,
    speechRepository: SpeechRecognitionRepository? = null
) {
    val scope = rememberCoroutineScope()

    val availableLanguages by (languageRepository?.getSupportedLanguages()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) })

    val defaultLanguages = remember {
        listOf(
            Language("en", "English", "English", "🇺🇸"),
            Language("es", "Spanish", "Español", "🇪🇸"),
            Language("fr", "French", "Français", "🇫🇷"),
            Language("de", "German", "Deutsch", "🇩🇪"),
            Language("ja", "Japanese", "日本語", "🇯🇵"),
            Language("hi", "Hindi", "हिन्दी", "🇮🇳"),
            Language("zh", "Chinese", "中文", "🇨🇳"),
            Language("ar", "Arabic", "العربية", "🇸🇦"),
            Language("it", "Italian", "Italiano", "🇮🇹")
        )
    }

    val effectiveLanguages = if (availableLanguages.isNotEmpty()) availableLanguages else defaultLanguages

    var sourceLang by remember { mutableStateOf(Language.AUTO_DETECT) }
    var targetLang by remember { mutableStateOf(effectiveLanguages.find { it.code == "es" } ?: effectiveLanguages.getOrElse(1) { Language("es", "Spanish", "Español", "🇪🇸") }) }

    // Live Voice Hearing Preview State
    val recognitionState by (speechRepository?.recognitionState?.collectAsState() ?: remember { mutableStateOf(SpeechRecognitionState()) })
    var isSimulatedHearing by remember { mutableStateOf(false) }
    val isActuallyListening = recognitionState.isListening || isSimulatedHearing
    val activeSoundLevel = if (recognitionState.isListening) recognitionState.soundLevelDb else (if (isSimulatedHearing) 7.5f else 0f)

    // Language Selector Mode: Bottom Sheet vs Dropdown Menu
    var useDropdownMode by remember { mutableStateOf(false) }

    // Instant Translation Preview State
    var previewInputText by remember { mutableStateOf("Welcome to LingoLens!") }
    var previewTranslatedText by remember { mutableStateOf("") }
    var isPreviewTranslating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LovableCream)
            .padding(horizontal = 20.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Lovable Hero Card with signature charcoal / warm gradient
            LovableCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                containerColor = LovableCharcoal
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LovableOffWhite.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = LovableOffWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LovableOffWhite.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = LovableOffWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Neural Engines Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LovableOffWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "LingoLens",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LovableOffWhite,
                        letterSpacing = (-0.9).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Smart camera lens & AI translation for signs, speech, and text.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LovableOffWhite.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Live Voice Hearing & ML Kit Language Selector Interactive Preview Section
        item {
            LovableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_section_card"),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = LovableCharcoal.copy(alpha = 0.1f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = LovableCharcoal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Live AI Voice & ML Kit Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LovableCharcoal
                                )
                                Text(
                                    text = "Real-time animated orb and language selection",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = LovableCharcoal.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Toggle Mode: Bottom Sheet vs Dropdown Menu
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = !useDropdownMode,
                                onClick = { useDropdownMode = false },
                                label = { Text("Sheet", fontSize = 11.sp) },
                                modifier = Modifier
                                    .smoothButtonPress()
                                    .testTag("toggle_sheet_mode_chip")
                            )
                            FilterChip(
                                selected = useDropdownMode,
                                onClick = { useDropdownMode = true },
                                label = { Text("Dropdown", fontSize = 11.sp) },
                                modifier = Modifier
                                    .smoothButtonPress()
                                    .testTag("toggle_dropdown_mode_chip")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Animated Hearing Orb in Center with Live Aura
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        AnimatedListeningOrb(
                            isListening = isActuallyListening,
                            soundLevelDb = activeSoundLevel,
                            size = if (isActuallyListening) 124.dp else 94.dp,
                            coreColorStart = if (isActuallyListening) Color(0xFF00F0FF) else LovableCharcoal,
                            coreColorMid = if (isActuallyListening) Color(0xFF6366F1) else LovableCharcoal,
                            coreColorEnd = if (isActuallyListening) Color(0xFFA855F7) else LovableCharcoal,
                            accentGlow = if (isActuallyListening) Color(0xFFF43F5E) else LovableLightCream,
                            onClick = {
                                if (speechRepository != null && !isSimulatedHearing) {
                                    if (recognitionState.isListening) {
                                        speechRepository.stopListening()
                                    } else {
                                        val code = if (sourceLang.isAuto) "en-US" else sourceLang.code
                                        speechRepository.startListening(code)
                                    }
                                } else {
                                    isSimulatedHearing = !isSimulatedHearing
                                }
                            },
                            modifier = Modifier.testTag("home_preview_orb")
                        )

                        // Center Microphone Icon
                        Icon(
                            imageVector = if (isActuallyListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isActuallyListening) "Stop Hearing" else "Start Hearing Preview",
                            tint = LovableOffWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Live Status Text
                    Text(
                        text = if (isActuallyListening) "Hearing Audio Input... Tap orb to stop" else "Tap Orb to Test Voice Hearing Animation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LovableCharcoal
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. ML Kit Language Selector Dual Control
                    MlKitLanguageSelectorDualControl(
                        sourceLanguage = sourceLang,
                        targetLanguage = targetLang,
                        availableLanguages = effectiveLanguages,
                        languageRepository = languageRepository,
                        translationRepository = translationRepository,
                        useDropdown = useDropdownMode,
                        onSourceLanguageChanged = { sourceLang = it },
                        onTargetLanguageChanged = { targetLang = it },
                        onSwapLanguages = {
                            if (languageRepository != null) {
                                val (s, t) = languageRepository.swapLanguages(sourceLang, targetLang)
                                sourceLang = s
                                targetLang = t
                            } else {
                                val temp = sourceLang
                                sourceLang = targetLang
                                targetLang = temp
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Fast Interactive ML Kit Translation Trigger in Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = previewInputText,
                            onValueChange = { previewInputText = it },
                            singleLine = true,
                            shape = RoundedCornerShape(6.dp),
                            placeholder = { Text("Enter text to test ML Kit...", fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LovableCharcoal,
                                unfocusedBorderColor = LovableLightCream,
                                focusedContainerColor = LovableCream,
                                unfocusedContainerColor = LovableCream
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_preview_input_field")
                        )

                        LovablePrimaryButton(
                            onClick = {
                                if (previewInputText.isNotBlank()) {
                                    isPreviewTranslating = true
                                    scope.launch {
                                        if (translationRepository != null) {
                                            val req = TranslationRequest(
                                                sourceText = previewInputText,
                                                sourceLanguageCode = if (sourceLang.isAuto) "en" else sourceLang.code,
                                                targetLanguageCode = targetLang.code
                                            )
                                            when (val res = translationRepository.translate(req)) {
                                                is AppResult.Success -> {
                                                    previewTranslatedText = res.data.translatedText
                                                }
                                                else -> {
                                                    previewTranslatedText = "Instant Translation (${targetLang.name}): ${previewInputText}"
                                                }
                                            }
                                        } else {
                                            previewTranslatedText = "Translated (${targetLang.name}): ${previewInputText}"
                                        }
                                        isPreviewTranslating = false
                                    }
                                }
                            },
                            modifier = Modifier.testTag("home_preview_translate_button")
                        ) {
                            if (isPreviewTranslating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = LovableOffWhite
                                )
                            } else {
                                Text("Translate", fontSize = 12.sp)
                            }
                        }
                    }

                    // Translated Preview Result
                    if (previewTranslatedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LovableLightCream,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = targetLang.flagEmoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = previewTranslatedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LovableCharcoal
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Translation Modes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LovableCharcoal
            )
        }

        // Primary Grid of 4 modes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickModeCard(
                    title = "Camera Vision",
                    subtitle = "Live OCR & Sign Reader",
                    icon = Icons.Default.CameraAlt,
                    accentColor = LovableCharcoal,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Camera.route) }
                )
                QuickModeCard(
                    title = "Voice Speech",
                    subtitle = "Dual-mic Speech to Text",
                    icon = Icons.Default.Mic,
                    accentColor = LovableCharcoal,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Voice.route) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickModeCard(
                    title = "Text Engine",
                    subtitle = "Type, Paste & Instant Translate",
                    icon = Icons.Default.Translate,
                    accentColor = LovableCharcoal,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Text.route) }
                )
                QuickModeCard(
                    title = "Dialogue",
                    subtitle = "Two-way Conversational Chat",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    accentColor = LovableCharcoal,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo(Screen.Conversation.route) }
                )
            }
        }

        item {
            Text(
                text = "Traveler & Assistant Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LovableCharcoal
            )
        }

        item {
            AssistantToolRow(
                title = "Photo & Document Translate",
                description = "Translate gallery photos, screenshots, and documents with zoom & OCR",
                icon = Icons.Default.Image,
                accentColor = LovableCharcoal,
                onClick = { onNavigateTo(Screen.ImageStudio.route) }
            )
        }

        item {
            AssistantToolRow(
                title = "Offline Languages",
                description = "Download neural translation packs, manage storage & offline availability",
                icon = Icons.Default.OfflinePin,
                accentColor = Color(0xFF10B981),
                onClick = { onNavigateTo(Screen.OfflineLanguages.route) }
            )
        }

        item {
            AssistantToolRow(
                title = "Travel Phrasebook",
                description = "Essential phrases for transit, dining & emergencies",
                icon = Icons.Default.Flight,
                accentColor = LovableCharcoal,
                onClick = { onNavigateTo(Screen.Travel.route) }
            )
        }

        item {
            AssistantToolRow(
                title = "Saved Phrases",
                description = "Quick access to starred terms and bookmark cards",
                icon = Icons.Default.Star,
                accentColor = Color(0xFFF59E0B),
                onClick = { onNavigateTo(Screen.Favorites.route) }
            )
        }

        item {
            AssistantToolRow(
                title = "Translation History",
                description = "Review past logs and voice transcripts",
                icon = Icons.Default.History,
                accentColor = LovableCharcoal,
                onClick = { onNavigateTo(Screen.History.route) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LovableCard(
        modifier = modifier
            .smoothButtonPress(scaleDown = 0.94f)
            .clickable(onClick = onClick)
            .testTag("mode_card_${title.lowercase().replace(" ", "_")}"),
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LovableCharcoal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LovableCharcoal.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun AssistantToolRow(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    LovableCard(
        modifier = Modifier
            .fillMaxWidth()
            .smoothButtonPress(scaleDown = 0.96f)
            .clickable(onClick = onClick)
            .testTag("tool_row_${title.lowercase().replace(" ", "_")}"),
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LovableCharcoal
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = LovableCharcoal.copy(alpha = 0.65f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = LovableCharcoal.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
