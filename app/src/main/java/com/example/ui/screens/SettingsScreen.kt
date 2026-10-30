package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.config.SecureSecretManager
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.offline.model.DownloadState
import com.example.modules.offline.repository.OfflinePackageRepository
import com.example.modules.settings.model.AppTextSize
import com.example.modules.settings.model.AppThemeMode
import com.example.modules.settings.model.FlashPreference
import com.example.modules.settings.model.FreezeFrameBehavior
import com.example.modules.settings.model.TranslationStyle
import com.example.modules.settings.repository.SettingsRepository
import com.example.modules.tts.model.TtsPlaybackState
import com.example.modules.tts.model.TtsVoiceInfo
import com.example.modules.tts.repository.TextToSpeechRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    offlinePackageRepository: OfflinePackageRepository,
    secretManager: SecureSecretManager,
    languageRepository: LanguageRepository? = null,
    historyRepository: HistoryRepository? = null,
    ttsRepository: TextToSpeechRepository? = null,
    onNavigateToOfflineLanguages: (() -> Unit)? = null,
    onNavigateToDiagnostics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settings.collectAsState()
    val ttsState by ttsRepository?.playbackState?.collectAsState() ?: remember {
        mutableStateOf(TtsPlaybackState())
    }
    val packages by offlinePackageRepository.packages.collectAsState()

    // Available languages for source & target selectors
    val supportedLanguages by languageRepository?.getSupportedLanguages()?.collectAsState(initial = emptyList())
        ?: remember {
            mutableStateOf(
                listOf(
                    Language("en", "English", "English", "🇺🇸"),
                    Language("es", "Spanish", "Español", "🇪🇸"),
                    Language("fr", "French", "Français", "🇫🇷"),
                    Language("de", "German", "Deutsch", "🇩🇪"),
                    Language("ja", "Japanese", "日本語", "🇯🇵"),
                    Language("zh", "Chinese (Simplified)", "简体中文", "🇨🇳")
                )
            )
        }

    // Dialog state controllers
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAiExplanationDialog by remember { mutableStateOf(false) }
    var showAiDisclaimerDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showOssLicensesDialog by remember { mutableStateOf(false) }

    // Query available voices
    val availableVoices = remember(ttsState.languageCode, ttsState.status) {
        ttsRepository?.getAvailableVoices() ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Page Header
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Preferences, on-device intelligence, privacy & controls",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // 1. TRANSLATION SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "TRANSLATION",
            icon = Icons.Default.Translate,
            tint = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Default Source & Target Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LanguageSelectorDropdown(
                        label = "Default Source",
                        selectedCode = settings.defaultSourceLanguage,
                        languages = supportedLanguages,
                        onLanguageSelected = { code ->
                            scope.launch { settingsRepository.updateLanguages(code, settings.defaultTargetLanguage) }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    LanguageSelectorDropdown(
                        label = "Default Target",
                        selectedCode = settings.defaultTargetLanguage,
                        languages = supportedLanguages,
                        onLanguageSelected = { code ->
                            scope.launch { settingsRepository.updateLanguages(settings.defaultSourceLanguage, code) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Auto-detect Language Switch
                SettingsSwitchRow(
                    title = "Auto-Detect Language",
                    description = "Identify spoken or visual input language automatically",
                    checked = settings.autoDetectLanguage,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setAutoDetect(checked) }
                    }
                )

                // Auto-speak Translation Switch
                SettingsSwitchRow(
                    title = "Auto-Speak Translation",
                    description = "Read target translations aloud as soon as results arrive",
                    checked = settings.autoSpeakTranslation,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setAutoSpeakTranslation(checked) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Translation Style
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Translation Style",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Customize phrasing demeanor for daily conversations, business, or studies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TranslationStyle.values().forEach { style ->
                            val isSelected = settings.translationStyle == style
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                onClick = {
                                    scope.launch { settingsRepository.setTranslationStyle(style) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = style.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. CAMERA SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "CAMERA",
            icon = Icons.Default.CameraAlt,
            tint = Color(0xFF3B82F6)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Translation Toggle
                SettingsSwitchRow(
                    title = "Live AR Translation",
                    description = "Render translated text overlays in real-time over camera frames",
                    checked = settings.liveCameraTranslation,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setLiveCameraTranslation(checked) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Overlay Opacity
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overlay Background Opacity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(settings.cameraOverlayOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.cameraOverlayOpacity,
                        onValueChange = { scope.launch { settingsRepository.setCameraOverlayOpacity(it) } },
                        valueRange = 0.3f..1.0f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Camera Text Size
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overlay Text Size",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${settings.cameraTextSize.toInt()} sp",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = settings.cameraTextSize,
                        onValueChange = { scope.launch { settingsRepository.setCameraTextSize(it) } },
                        valueRange = 10f..24f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Freeze Frame Behavior
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Freeze-Frame Behavior",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    FreezeFrameBehavior.values().forEach { behavior ->
                        val isSelected = settings.freezeFrameBehavior == behavior
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            onClick = {
                                scope.launch { settingsRepository.setFreezeFrameBehavior(behavior) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = behavior.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = behavior.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Flash Preference
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Flash Preference",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlashPreference.values().forEach { pref ->
                            val isSelected = settings.flashPreference == pref
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                onClick = { scope.launch { settingsRepository.setFlashPreference(pref) } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = pref.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. VOICE SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "VOICE",
            icon = Icons.Default.Mic,
            tint = Color(0xFF10B981)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().testTag("voice_settings_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speech Speed
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speech Speed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.2f", settings.speechSpeed)}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.speechSpeed,
                        onValueChange = {
                            ttsRepository?.setSpeechRate(it)
                            scope.launch { settingsRepository.updateSpeechRate(it) }
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Pitch
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pitch",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.2f", settings.speechPitch)}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = settings.speechPitch,
                        onValueChange = {
                            ttsRepository?.setPitch(it)
                            scope.launch { settingsRepository.updateSpeechPitch(it) }
                        },
                        valueRange = 0.5f..1.5f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Auto-speak in Voice Conversations
                SettingsSwitchRow(
                    title = "Auto-Speak Voice Results",
                    description = "Automatically vocalize incoming replies during two-way voice conversations",
                    checked = settings.autoSpeakVoice,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setAutoSpeakVoice(checked) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Voice Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Voice Engine Selection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = if (availableVoices.isNotEmpty()) {
                            "${availableVoices.size} local voice profiles detected for installed language engines."
                        } else {
                            "Default system voice engine active."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (availableVoices.isNotEmpty()) {
                        VoiceDropdownSelector(
                            voices = availableVoices,
                            selectedVoiceName = settings.selectedVoiceName,
                            onVoiceSelected = { voice ->
                                ttsRepository?.setVoice(voice)
                                scope.launch { settingsRepository.setSelectedVoiceName(voice?.name) }
                            }
                        )
                    }

                    // Test Speech & System TTS Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (ttsRepository != null) {
                                    scope.launch {
                                        ttsRepository.speak(
                                            text = "Hello! Android speech synthesis is fully active.",
                                            languageCode = settings.defaultTargetLanguage,
                                            speechRate = settings.speechSpeed,
                                            pitch = settings.speechPitch
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("test_voice_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Voice")
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = ttsRepository?.manager?.createTtsSettingsIntent()
                                        ?: Intent("com.android.settings.TTS_SETTINGS")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "System TTS Settings unavailable on this device", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("system_tts_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("System Voices")
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. OFFLINE SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "OFFLINE",
            icon = Icons.Default.CloudDownload,
            tint = Color(0xFFF59E0B)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Downloaded Models Status Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Downloaded Models",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${packages.count { it.state == DownloadState.DOWNLOADED }} language models ready • Total Storage: ${offlinePackageRepository.totalStorageUsedFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onNavigateToOfflineLanguages?.invoke() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Manage Packs")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Auto-update Preference
                SettingsSwitchRow(
                    title = "Auto-Update Neural Models",
                    description = "Keep on-device translation weights and neural vocabulary updated over Wi-Fi",
                    checked = settings.autoUpdateModels,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setAutoUpdateModels(checked) }
                    }
                )

                // Offline Preferred
                SettingsSwitchRow(
                    title = "Prefer Offline Processing",
                    description = "Always route translations through on-device models even when network is active",
                    checked = settings.offlineModePreferred,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setOfflinePreferred(checked) }
                    }
                )
            }
        }

        // ==========================================
        // 5. APPEARANCE SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "APPEARANCE",
            icon = Icons.Default.Palette,
            tint = Color(0xFF8B5CF6)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionPill(
                        label = "Light",
                        icon = Icons.Default.LightMode,
                        selected = settings.themeMode == AppThemeMode.LIGHT,
                        onClick = { scope.launch { settingsRepository.updateTheme(AppThemeMode.LIGHT) } },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionPill(
                        label = "Dark",
                        icon = Icons.Default.DarkMode,
                        selected = settings.themeMode == AppThemeMode.DARK,
                        onClick = { scope.launch { settingsRepository.updateTheme(AppThemeMode.DARK) } },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionPill(
                        label = "System",
                        icon = Icons.Default.Palette,
                        selected = settings.themeMode == AppThemeMode.SYSTEM,
                        onClick = { scope.launch { settingsRepository.updateTheme(AppThemeMode.SYSTEM) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Text Size / Accessibility
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Text Size & Readability",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTextSize.values().forEach { size ->
                            val isSelected = settings.textSize == size
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                onClick = { scope.launch { settingsRepository.setTextSize(size) } },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = size.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                // High-Contrast Mode
                SettingsSwitchRow(
                    title = "High Contrast Mode",
                    description = "Elevate contrast boundaries for visual clarity in bright sunlight",
                    checked = settings.highContrastMode,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setHighContrastMode(checked) }
                    }
                )
            }
        }

        // ==========================================
        // 6. PRIVACY SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "PRIVACY",
            icon = Icons.Default.Lock,
            tint = Color(0xFFEF4444)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // History logging switch
                SettingsSwitchRow(
                    title = "Translation History Logging",
                    description = "Save search, camera, voice, and conversation records in local Room database",
                    checked = settings.historyLoggingEnabled,
                    onCheckedChange = { checked ->
                        scope.launch { settingsRepository.setHistoryLoggingEnabled(checked) }
                    }
                )

                // Clear History Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear History",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Delete all stored local translation logs and cache records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // AI Data Explanation Info Row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    onClick = { showAiExplanationDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Data Explanation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Learn how your inputs are handled with zero retention & on-device isolation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Permissions Overview
                PermissionsOverviewRow(context = context)
            }
        }

        // ==========================================
        // 7. AI SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "ON-DEVICE NEURAL ENGINE",
            icon = Icons.Default.Translate,
            tint = Color(0xFF10B981)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Offline Translation Mode
                SettingsSwitchRow(
                    title = "Offline-First Translation",
                    description = "Prioritize local ML Kit neural models for zero-latency, private on-device translations",
                    checked = true,
                    onCheckedChange = { /* Always enabled for local reliability */ }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Privacy Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "100% Private & On-Device",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Camera OCR, translations, and voice recognition are processed directly on your device with no external AI transmission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. DEVELOPER TEST LAB & QA
        // ==========================================
        SettingsSectionHeader(
            title = "DEVELOPER TEST LAB",
            icon = Icons.Default.Science,
            tint = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("developer_test_lab_card")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Translation Test Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verify every translation pipeline and device capability with automated tests and diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { onNavigateToDiagnostics?.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_translation_test_lab_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OPEN TRANSLATION TEST LAB", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // 9. ABOUT SECTION
        // ==========================================
        SettingsSectionHeader(
            title = "ABOUT",
            icon = Icons.Default.Info,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // App Version
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LingoLens",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 2.4.0 (Build 2026.09.24)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "STABLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Developer Information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "LingoLens Engineering",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Open-source acknowledgements
                AboutLinkRow(
                    title = "Open-Source Acknowledgements",
                    subtitle = "ML Kit, CameraX, Jetpack Compose, KotlinX Coroutines & Room",
                    icon = Icons.Default.Description,
                    onClick = { showOssLicensesDialog = true }
                )

                // Privacy Policy
                AboutLinkRow(
                    title = "Privacy Policy",
                    subtitle = "Zero-telemetry policy, local database & model terms",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { showPrivacyPolicyDialog = true }
                )

                // Terms of Service
                AboutLinkRow(
                    title = "Terms of Service",
                    subtitle = "Usage rights, software license & machine learning disclosures",
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    onClick = { showTermsDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ==========================================
    // DIALOGS & MODALS
    // ==========================================

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Translation History?") },
            text = {
                Text("This will permanently remove all visual OCR scans, voice conversations, and text translation logs stored in your local database. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            historyRepository?.clearAllHistory()
                            showClearHistoryDialog = false
                            Toast.makeText(context, "Translation history cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Data Explanation Dialog
    if (showAiExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showAiExplanationDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("AI Data Privacy Architecture") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "• On-Device ML First: Translations, OCR text detection, and Text-to-Speech occur strictly on your physical device using downloaded ML Kit neural weights whenever possible.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Zero Cloud Retention: All translations, camera recognition, and speech processing run strictly on-device without telemetry or cloud logging.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Local Storage Only: Your history records, favorites, and saved offline language weights remain stored strictly in sandboxed application SQLite storage.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAiExplanationDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    // AI Disclaimer Dialog
    if (showAiDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showAiDisclaimerDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B)) },
            title = { Text("AI Translation Disclaimer") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Machine translation and artificial intelligence models may produce unexpected, incomplete, or nuanced misinterpretations depending on dialects, idioms, and lighting conditions.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Always double-check critical safety instructions, emergency warnings, legal documents, and vital medical communications with official or native sources.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAiDisclaimerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = { Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Privacy Policy") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("LingoLens Privacy Policy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Effective Date: September 24, 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "1. Data Collection: LingoLens operates on a zero-tracking framework. We do not collect names, emails, advertising IDs, or telemetry beacons.\n\n" +
                                "2. Camera & Audio: Camera frames and microphone input are processed locally in RAM for real-time OCR and speech recognition and discarded immediately after inference.\n\n" +
                                "3. Offline Packages: On-device translation packs are downloaded directly via Google ML Kit CDN without user-identifiable metadata.\n\n" +
                                "4. User Rights: You have full control to inspect, export, or permanently wipe your translation database at any time from this Settings screen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Accept & Close")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            icon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Terms of Service") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("End User License Agreement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Effective Date: September 24, 2026", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "By using LingoLens, you agree to utilize the tool for lawful communication, educational translation, and travel comprehension.\n\n" +
                                "The translation services are provided 'as is' without warranties of absolute grammatical perfection. The developers shall not be liable for losses arising from reliance on machine-translated outputs in high-risk contexts.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("Agree")
                }
            }
        )
    }

    // Open Source Acknowledgements Dialog
    if (showOssLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showOssLicensesDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Open-Source Acknowledgements") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OssItem(
                        name = "Google ML Kit (Vision & Natural Language)",
                        license = "Apache License 2.0",
                        description = "On-device Text Recognition, Language Identification, and Neural Machine Translation."
                    )
                    OssItem(
                        name = "Android Jetpack Compose & Material 3",
                        license = "Apache License 2.0",
                        description = "Modern declarative UI toolkit and dynamic M3 design system."
                    )
                    OssItem(
                        name = "AndroidX CameraX",
                        license = "Apache License 2.0",
                        description = "Camera viewfinder streaming, live ImageAnalysis, and hardware lifecycle binding."
                    )
                    OssItem(
                        name = "Room Persistence Library",
                        license = "Apache License 2.0",
                        description = "Robust SQLite object mapping for translation history and favorites."
                    )
                    OssItem(
                        name = "Kotlin Coroutines & Flow",
                        license = "Apache License 2.0",
                        description = "Asynchronous reactive stream primitives."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showOssLicensesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ==========================================
// REUSABLE HELPER COMPONENTS
// ==========================================

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectorDropdown(
    label: String,
    selectedCode: String,
    languages: List<Language>,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLang = languages.find { it.code == selectedCode }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLang?.let { "${it.flag} ${it.displayName}" } ?: selectedCode,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.take(25).forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = lang.flag, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = lang.displayName, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    onClick = {
                        onLanguageSelected(lang.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceDropdownSelector(
    voices: List<TtsVoiceInfo>,
    selectedVoiceName: String?,
    onVoiceSelected: (TtsVoiceInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVoice = voices.find { it.name == selectedVoiceName }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedVoice?.let { "${it.languageDisplayName} (${it.quality})" } ?: "Default System Voice",
            onValueChange = {},
            readOnly = true,
            label = { Text("Selected Voice", style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Default System Voice", style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    onVoiceSelected(null)
                    expanded = false
                }
            )
            voices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${voice.languageDisplayName} (${voice.quality})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = voice.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onVoiceSelected(voice)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PermissionsOverviewRow(context: Context) {
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open application settings", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Application Permissions",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Camera: ${if (cameraGranted) "Granted" else "Not Granted"} • Mic: ${if (micGranted) "Granted" else "Not Granted"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (cameraGranted && micGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun OssItem(
    name: String,
    license: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = license,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
