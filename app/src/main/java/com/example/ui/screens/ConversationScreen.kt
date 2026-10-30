package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.permission.PermissionManager
import com.example.core.result.AppResult
import com.example.modules.conversation.model.ConversationMessage
import com.example.modules.conversation.model.ConversationSpeaker
import com.example.modules.conversation.repository.ConversationRepository
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationMode
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.speech.repository.SpeechRecognitionRepository
import com.example.modules.translation.model.TranslationRequest
import com.example.modules.translation.repository.TranslationRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.animation.AnimatedListeningOrb
import com.example.ui.animation.smoothButtonPress
import com.example.ui.components.PermissionRequiredView
import com.example.ui.components.TtsSpeechButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    languageRepository: LanguageRepository,
    conversationRepository: ConversationRepository,
    speechRepository: SpeechRecognitionRepository,
    translationRepository: TranslationRepository,
    ttsRepository: TextToSpeechRepository? = null,
    historyRepository: HistoryRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var hasAudioPermission by remember {
        mutableStateOf(PermissionManager.hasAudioPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    val session by conversationRepository.currentSession.collectAsState()
    val languages by languageRepository.getSupportedLanguages().collectAsState(initial = emptyList())
    val recognitionState by speechRepository.recognitionState.collectAsState()

    var personALang by remember { mutableStateOf(Language("hi", "Hindi", "हिन्दी", "🇮🇳")) }
    var personBLang by remember { mutableStateOf(Language("en", "English", "English", "🇺🇸")) }

    var activeSpeaker by remember { mutableStateOf<ConversationSpeaker?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var isSpeakingTranslation by remember { mutableStateOf(false) }
    var isContinuousMode by remember { mutableStateOf(false) }
    var lastSpokenTurn by remember { mutableStateOf<ConversationSpeaker?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageSheetFor by remember { mutableStateOf<ConversationSpeaker?>(null) }
    var conversationError by remember { mutableStateOf<String?>(null) }

    // Initialize languages if repository has them
    LaunchedEffect(languages) {
        if (languages.isNotEmpty()) {
            val defaultA = languages.find { it.code == session.personALanguage } ?: languages.find { it.code == "hi" } ?: languages.first()
            val defaultB = languages.find { it.code == session.personBLanguage } ?: languages.find { it.code == "en" } ?: languages.getOrNull(1) ?: defaultA
            personALang = defaultA
            personBLang = defaultB
        }
    }

    // Auto-scroll when new message is added
    LaunchedEffect(session.messages.size) {
        if (session.messages.isNotEmpty()) {
            listState.animateScrollToItem(session.messages.size - 1)
        }
    }

    // Clean up speech and audio on exit
    DisposableEffect(Unit) {
        onDispose {
            speechRepository.stopListening()
            ttsRepository?.stop()
        }
    }

    // React to final speech recognition result
    LaunchedEffect(recognitionState.finalResult) {
        val finalTranscript = recognitionState.finalResult.trim()
        val currentSpeaker = activeSpeaker

        if (finalTranscript.isNotBlank() && currentSpeaker != null) {
            isTranslating = true
            conversationError = null
            activeSpeaker = null // Reset active speaker now that speech finished

            scope.launch {
                val sourceCode = if (currentSpeaker == ConversationSpeaker.PERSON_A) personALang.code else personBLang.code
                val targetCode = if (currentSpeaker == ConversationSpeaker.PERSON_A) personBLang.code else personALang.code
                val sourceName = if (currentSpeaker == ConversationSpeaker.PERSON_A) personALang.name else personBLang.name
                val targetName = if (currentSpeaker == ConversationSpeaker.PERSON_A) personBLang.name else personALang.name
                val sourceFlag = if (currentSpeaker == ConversationSpeaker.PERSON_A) personALang.flagEmoji else personBLang.flagEmoji
                val targetFlag = if (currentSpeaker == ConversationSpeaker.PERSON_A) personBLang.flagEmoji else personALang.flagEmoji

                val request = TranslationRequest(
                    sourceText = finalTranscript,
                    sourceLanguageCode = sourceCode,
                    targetLanguageCode = targetCode
                )

                when (val result = translationRepository.translate(request)) {
                    is AppResult.Success -> {
                        val translatedText = result.data.translatedText
                        isTranslating = false
                        lastSpokenTurn = currentSpeaker

                        // Add to session
                        conversationRepository.addMessage(
                            speaker = currentSpeaker,
                            originalText = finalTranscript,
                            translatedText = translatedText,
                            originalLanguage = sourceCode,
                            targetLanguage = targetCode,
                            originalLanguageName = sourceName,
                            targetLanguageName = targetName,
                            originalFlagEmoji = sourceFlag,
                            targetFlagEmoji = targetFlag
                        )

                        // Save to history
                        historyRepository?.insertHistoryItem(
                            TranslationHistoryItem(
                                sourceText = finalTranscript,
                                translatedText = translatedText,
                                sourceLanguage = sourceCode,
                                targetLanguage = targetCode,
                                mode = TranslationMode.CONVERSATION
                            )
                        )

                        // Auto-speak translated output if enabled
                        if (session.isAutoPlayAudio && ttsRepository != null) {
                            isSpeakingTranslation = true
                            ttsRepository.speak(
                                text = translatedText,
                                languageCode = targetCode
                            )
                            val estimatedDurationMs = (translatedText.length * 80L).coerceIn(1200L, 5000L)
                            delay(estimatedDurationMs)
                            ttsRepository.stop()
                            isSpeakingTranslation = false
                        }

                        // Explicitly release speech audio resources
                        speechRepository.stopListening()
                        delay(250L) // Crucial pause for Android audio track to fully close

                        // Auto-continue to other speaker if Continuous Conversation mode is active
                        if (isContinuousMode) {
                            val nextSpeaker = if (currentSpeaker == ConversationSpeaker.PERSON_A) {
                                ConversationSpeaker.PERSON_B
                            } else {
                                ConversationSpeaker.PERSON_A
                            }
                            val nextLangCode = if (nextSpeaker == ConversationSpeaker.PERSON_A) personALang.code else personBLang.code
                            activeSpeaker = nextSpeaker
                            speechRepository.startListening(nextLangCode)
                        }
                    }
                    is AppResult.Error -> {
                        isTranslating = false
                        conversationError = result.error.message
                        ttsRepository?.stop()
                        speechRepository.stopListening()
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
            rationale = "Vision Translator needs microphone access to enable real-time two-way conversation translation between Person A and Person B.",
            onRequestPermission = {
                permissionLauncher.launch(PermissionManager.RECORD_AUDIO_PERMISSION)
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("conversation_screen")
    ) {
        // Top Header Controls: Two Language Selectors & Action Buttons
        ConversationHeader(
            personALanguage = personALang,
            personBLanguage = personBLang,
            isAutoSpeak = session.isAutoPlayAudio,
            onSelectLanguageA = { showLanguageSheetFor = ConversationSpeaker.PERSON_A },
            onSelectLanguageB = { showLanguageSheetFor = ConversationSpeaker.PERSON_B },
            onSwapLanguages = {
                val temp = personALang
                personALang = personBLang
                personBLang = temp
                conversationRepository.swapLanguages()
            },
            onToggleAutoSpeak = { conversationRepository.setAutoPlayAudio(it) },
            onCopyTranscript = {
                if (session.messages.isEmpty()) {
                    Toast.makeText(context, "No messages to copy", Toast.LENGTH_SHORT).show()
                } else {
                    val fullTranscript = session.messages.joinToString("\n\n") { msg ->
                        val speakerName = if (msg.speaker == ConversationSpeaker.PERSON_A) "Person A (${msg.originalLanguageName})" else "Person B (${msg.originalLanguageName})"
                        "[$speakerName]\nOriginal: ${msg.originalText}\nTranslation: ${msg.translatedText}"
                    }
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Conversation Transcript", fullTranscript))
                    Toast.makeText(context, "Full conversation transcript copied!", Toast.LENGTH_SHORT).show()
                }
            },
            onClearConversation = {
                if (session.messages.isNotEmpty()) {
                    showClearDialog = true
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main Conversation Message Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (session.messages.isEmpty() && activeSpeaker == null && !isTranslating) {
                // Empty state greeting card
                ConversationEmptyState(
                    personALang = personALang,
                    personBLang = personBLang,
                    onStartPersonA = {
                        activeSpeaker = ConversationSpeaker.PERSON_A
                        speechRepository.startListening(personALang.code)
                    },
                    onStartPersonB = {
                        activeSpeaker = ConversationSpeaker.PERSON_B
                        speechRepository.startListening(personBLang.code)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(session.messages, key = { it.id }) { message ->
                        ConversationMessageBubble(
                            message = message,
                            ttsRepository = ttsRepository,
                            onCopy = {
                                val textToCopy = "🗣️ ${message.originalText}\n🌐 ${message.translatedText}"
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Conversation Message", textToCopy))
                                Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                conversationRepository.removeMessage(message.id)
                            }
                        )
                    }

                    // Live In-Progress Speech Card (Person A or Person B)
                    if (activeSpeaker != null || isTranslating) {
                        item(key = "live_speech_in_progress") {
                            val currentActive = activeSpeaker ?: lastSpokenTurn ?: ConversationSpeaker.PERSON_A
                            LiveTurnPreviewCard(
                                speaker = currentActive,
                                speakerLanguage = if (currentActive == ConversationSpeaker.PERSON_A) personALang else personBLang,
                                partialText = recognitionState.partialText,
                                isListening = recognitionState.isListening,
                                isTranslating = isTranslating,
                                soundLevelDb = recognitionState.soundLevelDb,
                                onStopListening = {
                                    speechRepository.stopListening()
                                }
                            )
                        }
                    }

                    // Bottom padding for clear floating mic bar visibility
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Error Banner
        if (conversationError != null || (recognitionState.error != null && !recognitionState.isListening)) {
            val err = conversationError ?: recognitionState.error ?: "Operation failed"
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            conversationError = null
                            speechRepository.clearResults()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Turn Status / Next Turn Indicator & Mode Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurnStatusIndicator(
                activeSpeaker = activeSpeaker,
                lastSpeaker = lastSpokenTurn,
                personALang = personALang,
                personBLang = personBLang,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Continuous Mode Toggle Chip
            FilterChip(
                selected = isContinuousMode,
                onClick = {
                    isContinuousMode = !isContinuousMode
                    if (isContinuousMode && activeSpeaker == null) {
                        // Start conversation with Person A
                        ttsRepository?.stop()
                        speechRepository.cancel()
                        activeSpeaker = ConversationSpeaker.PERSON_A
                        conversationError = null
                        scope.launch {
                            delay(120)
                            speechRepository.startListening(personALang.code)
                        }
                    }
                },
                label = {
                    Text(
                        if (isContinuousMode) "Auto Alternate: ON" else "Continuous: OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isContinuousMode) Icons.Default.Sync else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.testTag("continuous_mode_chip")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dual Person Microphone Control Bar
        DualMicrophoneBar(
            activeSpeaker = activeSpeaker,
            personALang = personALang,
            personBLang = personBLang,
            soundLevelDb = recognitionState.soundLevelDb,
            onPersonAMicClick = {
                ttsRepository?.stop()
                if (activeSpeaker == ConversationSpeaker.PERSON_A) {
                    speechRepository.stopListening()
                    activeSpeaker = null
                } else {
                    speechRepository.cancel()
                    activeSpeaker = ConversationSpeaker.PERSON_A
                    conversationError = null
                    scope.launch {
                        delay(120) // Release audio resources cleanly
                        speechRepository.startListening(personALang.code)
                    }
                }
            },
            onPersonBMicClick = {
                ttsRepository?.stop()
                if (activeSpeaker == ConversationSpeaker.PERSON_B) {
                    speechRepository.stopListening()
                    activeSpeaker = null
                } else {
                    speechRepository.cancel()
                    activeSpeaker = ConversationSpeaker.PERSON_B
                    conversationError = null
                    scope.launch {
                        delay(120) // Release audio resources cleanly
                        speechRepository.startListening(personBLang.code)
                    }
                }
            }
        )
    }

    // Clear Conversation Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation?") },
            text = { Text("This will erase all messages in the current two-way conversation session.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationRepository.clearSession()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Language Picker Bottom Sheet
    showLanguageSheetFor?.let { targetSpeaker ->
        LanguageSelectionBottomSheet(
            speaker = targetSpeaker,
            currentLanguage = if (targetSpeaker == ConversationSpeaker.PERSON_A) personALang else personBLang,
            availableLanguages = languages,
            onLanguageSelected = { selected ->
                if (targetSpeaker == ConversationSpeaker.PERSON_A) {
                    personALang = selected
                    conversationRepository.setLanguages(selected.code, personBLang.code)
                } else {
                    personBLang = selected
                    conversationRepository.setLanguages(personALang.code, selected.code)
                }
                showLanguageSheetFor = null
            },
            onDismiss = { showLanguageSheetFor = null }
        )
    }
}

/**
 * Top conversation header bar displaying two language selectors, swap button, and utility tools.
 */
@Composable
fun ConversationHeader(
    personALanguage: Language,
    personBLanguage: Language,
    isAutoSpeak: Boolean,
    onSelectLanguageA: () -> Unit,
    onSelectLanguageB: () -> Unit,
    onSwapLanguages: () -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onCopyTranscript: () -> Unit,
    onClearConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Dual Language Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Person A Language Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .smoothButtonPress(scaleDown = 0.95f)
                        .clickable { onSelectLanguageA() }
                        .testTag("conversation_lang_a_selector")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Person A",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${personALanguage.flagEmoji} ${personALanguage.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1
                        )
                    }
                }

                // Swap Languages Button
                IconButton(
                    onClick = onSwapLanguages,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(36.dp)
                        .smoothButtonPress(scaleDown = 0.88f)
                        .testTag("conversation_swap_languages_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap conversation languages",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Person B Language Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .smoothButtonPress(scaleDown = 0.95f)
                        .clickable { onSelectLanguageB() }
                        .testTag("conversation_lang_b_selector")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Person B",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${personBLanguage.flagEmoji} ${personBLanguage.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action toolbar: Auto-Speak Switch, Copy All, Clear All
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Auto-speak toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleAutoSpeak(!isAutoSpeak) }
                ) {
                    Icon(
                        imageVector = if (isAutoSpeak) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null,
                        tint = if (isAutoSpeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-Speak",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isAutoSpeak,
                        onCheckedChange = onToggleAutoSpeak,
                        modifier = Modifier.scale(0.75f).testTag("conversation_auto_speak_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopyTranscript,
                        modifier = Modifier
                            .size(32.dp)
                            .smoothButtonPress(scaleDown = 0.88f)
                            .testTag("conversation_copy_all_button")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy conversation transcript",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearConversation,
                        modifier = Modifier
                            .size(32.dp)
                            .smoothButtonPress(scaleDown = 0.88f)
                            .testTag("conversation_clear_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear conversation history",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual chat bubble displaying Person A or Person B utterance, translation, and audio actions.
 */
@Composable
fun ConversationMessageBubble(
    message: ConversationMessage,
    ttsRepository: TextToSpeechRepository?,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPersonA = message.speaker == ConversationSpeaker.PERSON_A
    val alignment = if (isPersonA) Alignment.Start else Alignment.End
    val containerColor = if (isPersonA)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val contentColor = if (isPersonA)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    val bubbleShape = if (isPersonA) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomEnd = 20.dp, bottomStart = 20.dp)
    }

    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversation_message_${message.id}"),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Bubble Header: Speaker badge, language, timestamp, and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (isPersonA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isPersonA) "Person A" else "Person B",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPersonA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${message.originalFlagEmoji} → ${message.targetFlagEmoji}",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )

                        // Speak Original Spoken Text
                        if (ttsRepository != null) {
                            TtsSpeechButton(
                                text = message.originalText,
                                languageCode = message.originalLanguageCode,
                                ttsRepository = ttsRepository,
                                size = 28,
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                        }

                        // Copy Message Button
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Original Spoken Text
                Text(
                    text = message.originalText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = contentColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Translated Card Section
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.translatedText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (ttsRepository != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            // Speak Translated Output
                            TtsSpeechButton(
                                text = message.translatedText,
                                languageCode = message.targetLanguageCode,
                                ttsRepository = ttsRepository,
                                size = 32,
                                tint = if (isPersonA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live speech transcript preview displayed above the conversation stream while someone is speaking.
 */
@Composable
fun LiveTurnPreviewCard(
    speaker: ConversationSpeaker,
    speakerLanguage: Language,
    partialText: String,
    isListening: Boolean,
    isTranslating: Boolean,
    soundLevelDb: Float,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPersonA = speaker == ConversationSpeaker.PERSON_A
    val cardColor = if (isPersonA)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    else
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversation_live_turn_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isPersonA) "Person A" else "Person B"} is speaking (${speakerLanguage.name})...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPersonA) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (isListening) {
                    AnimatedListeningOrb(
                        isListening = true,
                        soundLevelDb = soundLevelDb,
                        size = 36.dp,
                        coreColorStart = if (isPersonA) Color(0xFF38BDF8) else Color(0xFFC084FC),
                        coreColorMid = if (isPersonA) Color(0xFF6366F1) else Color(0xFFEC4899),
                        accentGlow = if (isPersonA) Color(0xFF00F0FF) else Color(0xFFF43F5E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isTranslating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Translating speech...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (partialText.isNotBlank()) {
                Text(
                    text = partialText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
                    color = if (isPersonA) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                Text(
                    text = "Listening... Speak in ${speakerLanguage.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (isPersonA) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.7f)
                )
            }

            if (isListening) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onStopListening,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Empty conversation placeholder with quick starters.
 */
@Composable
fun ConversationEmptyState(
    personALang: Language,
    personBLang: Language,
    onStartPersonA: () -> Unit,
    onStartPersonB: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Two-Way Conversation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Person A speaks in ${personALang.name} (${personALang.flagEmoji})\nPerson B speaks in ${personBLang.name} (${personBLang.flagEmoji})\n\nTranslations will be spoken and displayed in real time.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onStartPersonA,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start ${personALang.name}")
            }

            Button(
                onClick = onStartPersonB,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start ${personBLang.name}")
            }
        }
    }
}

/**
 * Subtle turn indicator displaying whose turn it is or who spoke last.
 */
@Composable
fun TurnStatusIndicator(
    activeSpeaker: ConversationSpeaker?,
    lastSpeaker: ConversationSpeaker?,
    personALang: Language,
    personBLang: Language,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        activeSpeaker == ConversationSpeaker.PERSON_A -> "Listening to Person A (${personALang.name})..."
        activeSpeaker == ConversationSpeaker.PERSON_B -> "Listening to Person B (${personBLang.name})..."
        lastSpeaker == ConversationSpeaker.PERSON_A -> "Person A finished. Ready for Person B (${personBLang.name})"
        lastSpeaker == ConversationSpeaker.PERSON_B -> "Person B finished. Ready for Person A (${personALang.name})"
        else -> "Tap either microphone to speak"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Dual floating microphone control bar for Person A and Person B.
 */
@Composable
fun DualMicrophoneBar(
    activeSpeaker: ConversationSpeaker?,
    personALang: Language,
    personBLang: Language,
    soundLevelDb: Float,
    onPersonAMicClick: () -> Unit,
    onPersonBMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_conversation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Person A Microphone Button
        val isPersonAActive = activeSpeaker == ConversationSpeaker.PERSON_A
        Surface(
            onClick = onPersonAMicClick,
            shape = RoundedCornerShape(20.dp),
            color = if (isPersonAActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .smoothButtonPress(scaleDown = 0.94f)
                .testTag("speak_person_a_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isPersonAActive) {
                    AnimatedListeningOrb(
                        isListening = true,
                        soundLevelDb = soundLevelDb,
                        size = 64.dp,
                        coreColorStart = Color(0xFF00F0FF),
                        coreColorMid = Color(0xFFEF4444),
                        accentGlow = Color(0xFFF43F5E)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isPersonAActive) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isPersonAActive) "Stop listening Person A" else "Listen Person A",
                        tint = if (isPersonAActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Person A",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = (if (isPersonAActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary).copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${personALang.flagEmoji} ${personALang.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPersonAActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Person B Microphone Button
        val isPersonBActive = activeSpeaker == ConversationSpeaker.PERSON_B
        Surface(
            onClick = onPersonBMicClick,
            shape = RoundedCornerShape(20.dp),
            color = if (isPersonBActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .smoothButtonPress(scaleDown = 0.94f)
                .testTag("speak_person_b_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isPersonBActive) {
                    AnimatedListeningOrb(
                        isListening = true,
                        soundLevelDb = soundLevelDb,
                        size = 64.dp,
                        coreColorStart = Color(0xFFC084FC),
                        coreColorMid = Color(0xFFEF4444),
                        accentGlow = Color(0xFFEC4899)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isPersonBActive) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isPersonBActive) "Stop listening Person B" else "Listen Person B",
                        tint = if (isPersonBActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Person B",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = (if (isPersonBActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary).copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${personBLang.flagEmoji} ${personBLang.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPersonBActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Sheet for selecting language for Person A or Person B.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionBottomSheet(
    speaker: ConversationSpeaker,
    currentLanguage: Language,
    availableLanguages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery, availableLanguages) {
        if (searchQuery.isBlank()) availableLanguages
        else availableLanguages.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.nativeName.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Select Language for ${if (speaker == ConversationSpeaker.PERSON_A) "Person A" else "Person B"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search languages...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                items(filteredLanguages, key = { it.code }) { lang ->
                    val isSelected = lang.code == currentLanguage.code
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onLanguageSelected(lang) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = lang.flagEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = lang.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Text(
                                    text = "Selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
