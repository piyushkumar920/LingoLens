package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animation.smoothButtonPress
import com.example.modules.tts.model.TtsLanguageStatus
import com.example.modules.tts.model.TtsPlaybackState
import com.example.modules.tts.model.TtsPlaybackStatus
import com.example.modules.tts.model.TtsSpeedOption
import com.example.modules.tts.model.TtsVoiceInfo
import com.example.modules.tts.repository.TextToSpeechRepository
import kotlinx.coroutines.launch

/**
 * Compact, self-contained speech button for embedding into list items, cards, and toolbars.
 */
@Composable
fun TtsSpeechButton(
    text: String,
    languageCode: String,
    ttsRepository: TextToSpeechRepository,
    modifier: Modifier = Modifier,
    speechRate: Float? = null,
    pitch: Float? = null,
    size: Int = 36,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by ttsRepository.playbackState.collectAsState()

    val isThisSpeaking = state.isSpeaking && state.currentlySpokenText == text.trim()
    val isThisPaused = state.isPaused && state.currentlySpokenText == text.trim()

    IconButton(
        onClick = {
            if (isThisSpeaking) {
                ttsRepository.stop()
            } else if (isThisPaused) {
                ttsRepository.resume()
            } else {
                if (text.isBlank()) {
                    Toast.makeText(context, "No text to speak", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                coroutineScope.launch {
                    ttsRepository.speak(
                        text = text,
                        languageCode = languageCode,
                        speechRate = speechRate,
                        pitch = pitch
                    )
                }
            }
        },
        modifier = modifier
            .size(size.dp)
            .smoothButtonPress(scaleDown = 0.88f)
            .testTag("tts_speech_button_${languageCode}")
    ) {
        Icon(
            imageVector = when {
                isThisSpeaking -> Icons.Default.Stop
                isThisPaused -> Icons.Default.PlayArrow
                else -> Icons.AutoMirrored.Filled.VolumeUp
            },
            contentDescription = if (isThisSpeaking) "Stop speech" else "Speak text",
            tint = if (isThisSpeaking) MaterialTheme.colorScheme.error else tint,
            modifier = Modifier.size((size * 0.65f).dp)
        )
    }
}

/**
 * Full-featured interactive audio player strip with playback controls, progress indicator,
 * speech rate pill, warning notices, and a quick dialog for speed/pitch/voice customization.
 */
@Composable
fun TtsAudioPlayerStrip(
    text: String,
    languageCode: String,
    languageName: String,
    ttsRepository: TextToSpeechRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by ttsRepository.playbackState.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }

    val isThisItemActive = state.currentlySpokenText == text.trim()
    val isSpeaking = isThisItemActive && state.isSpeaking
    val isPaused = isThisItemActive && state.isPaused

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tts_audio_player_strip")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with language tag and settings gear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Audio Pronunciation ($languageName)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speed indicator chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { showSettingsSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.speechRate}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.size(28.dp).testTag("tts_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Voice settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Progress bar if active
            if (isSpeaking || isPaused) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (state.progress > 0f) state.progress else 0.1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback controls row: Play/Pause, Stop, Replay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button
                Button(
                    onClick = {
                        if (isSpeaking) {
                            ttsRepository.pause()
                        } else if (isPaused) {
                            ttsRepository.resume()
                        } else {
                            if (text.isBlank()) {
                                Toast.makeText(context, "No text to speak", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                ttsRepository.speak(text, languageCode)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).testTag("tts_play_pause_button")
                ) {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Default.Pause
                            isPaused -> Icons.Default.PlayArrow
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isSpeaking -> "Pause"
                            isPaused -> "Resume"
                            else -> "Listen"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Stop Button
                FilledTonalButton(
                    onClick = { ttsRepository.stop() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("tts_stop_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop audio", modifier = Modifier.size(18.dp))
                }

                // Replay Button
                FilledTonalButton(
                    onClick = {
                        coroutineScope.launch {
                            ttsRepository.speak(text, languageCode)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("tts_replay_button")
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Replay audio", modifier = Modifier.size(18.dp))
                }
            }

            // Warning Banner (e.g. language fallback or missing voice data)
            if (!state.warningMessage.isNullOrBlank() && isThisItemActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.warningMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        TtsSpeedPitchBottomSheet(
            languageCode = languageCode,
            sampleText = text,
            ttsRepository = ttsRepository,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

/**
 * Modal bottom sheet for configuring speech speed, pitch, and voice selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSpeedPitchBottomSheet(
    languageCode: String,
    sampleText: String,
    ttsRepository: TextToSpeechRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by ttsRepository.playbackState.collectAsState()
    val availableVoices = remember(languageCode) { ttsRepository.getAvailableVoices(languageCode) }

    var currentSpeed by remember { mutableStateOf(state.speechRate) }
    var currentPitch by remember { mutableStateOf(state.pitch) }
    var selectedVoice by remember { mutableStateOf(state.currentVoice) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Speech & Voice Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fine-tune pronunciation speed, pitch, and voice characteristics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Speech Speed Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speech Speed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${"%.2f".format(currentSpeed)}x",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Speed Preset Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                items(TtsSpeedOption.ALL) { opt ->
                    FilterChip(
                        selected = kotlin.math.abs(currentSpeed - opt.rate) < 0.05f,
                        onClick = {
                            currentSpeed = opt.rate
                            ttsRepository.setSpeechRate(opt.rate)
                        },
                        label = { Text(opt.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voice Pitch Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voice Pitch",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${"%.2f".format(currentPitch)}x",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = currentPitch,
                onValueChange = {
                    currentPitch = it
                    ttsRepository.setPitch(it)
                },
                valueRange = 0.5f..1.5f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )

            // Voice selection if available
            if (availableVoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Installed Voices (${availableVoices.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableVoices) { voice ->
                        val isSelected = selectedVoice?.name == voice.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedVoice = voice
                                ttsRepository.setVoice(voice)
                            },
                            label = {
                                Text(
                                    text = "${voice.quality} (${voice.latency})",
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Play Sample Button & Android TTS Settings Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val testTxt = sampleText.ifBlank { "Hello, this is a voice test for Vision Translator." }
                        coroutineScope.launch {
                            ttsRepository.speak(
                                text = testTxt,
                                languageCode = languageCode,
                                speechRate = currentSpeed,
                                pitch = currentPitch,
                                voice = selectedVoice
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Voice")
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = ttsRepository.manager.createTtsSettingsIntent()
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val installIntent = ttsRepository.manager.createInstallVoiceDataIntent()
                                context.startActivity(installIntent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Cannot open TTS settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("System TTS")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
