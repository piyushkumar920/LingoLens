package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.result.AppResult
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.translation.repository.TranslationRepository
import com.example.ui.animation.smoothButtonPress
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectorBar(
    sourceLanguage: Language,
    targetLanguage: Language,
    availableLanguages: List<Language>,
    onSourceLanguageChanged: (Language) -> Unit,
    onTargetLanguageChanged: (Language) -> Unit,
    onSwapLanguages: () -> Unit,
    modifier: Modifier = Modifier,
    languageRepository: LanguageRepository? = null,
    translationRepository: TranslationRepository? = null
) {
    var selectingSource by remember { mutableStateOf(false) }
    var selectingTarget by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var swapRotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = tween(durationMillis = 300),
        label = "swap_rotation"
    )

    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("language_selector_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source Language Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .smoothButtonPress(scaleDown = 0.95f)
                    .clickable { selectingSource = true }
                    .testTag("source_language_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = sourceLanguage.flagEmoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = sourceLanguage.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sourceLanguage.isAuto) {
                            Text(
                                text = "Auto Detect",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        } else if (sourceLanguage.isDownloadedOffline) {
                            Text(
                                text = "Offline ready",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Interactive Swap Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    swapRotation += 180f
                    onSwapLanguages()
                },
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .smoothButtonPress(scaleDown = 0.88f)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .rotate(animatedRotation)
                    .testTag("swap_languages_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Source and Target Languages",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Target Language Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .smoothButtonPress(scaleDown = 0.95f)
                    .clickable { selectingTarget = true }
                    .testTag("target_language_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = targetLanguage.flagEmoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = targetLanguage.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (targetLanguage.isDownloadedOffline) {
                            Text(
                                text = "Offline ready",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectingSource) {
        ModalBottomSheet(
            onDismissRequest = { selectingSource = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LanguagePickerSheet(
                title = "Select Source Language",
                includeAuto = true,
                languages = availableLanguages,
                selected = sourceLanguage,
                languageRepository = languageRepository,
                translationRepository = translationRepository,
                onSelected = {
                    onSourceLanguageChanged(it)
                    selectingSource = false
                },
                onDismiss = { selectingSource = false }
            )
        }
    }

    if (selectingTarget) {
        ModalBottomSheet(
            onDismissRequest = { selectingTarget = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            LanguagePickerSheet(
                title = "Select Target Language",
                includeAuto = false,
                languages = availableLanguages,
                selected = targetLanguage,
                languageRepository = languageRepository,
                translationRepository = translationRepository,
                onSelected = {
                    onTargetLanguageChanged(it)
                    selectingTarget = false
                },
                onDismiss = { selectingTarget = false }
            )
        }
    }
}

@Composable
private fun LanguagePickerSheet(
    title: String,
    includeAuto: Boolean,
    languages: List<Language>,
    selected: Language,
    languageRepository: LanguageRepository?,
    translationRepository: TranslationRepository?,
    onSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var downloadingLanguageCode by remember { mutableStateOf<String?>(null) }

    val filteredLanguages = remember(searchQuery, languages, includeAuto) {
        val base = if (includeAuto) listOf(Language.AUTO_DETECT) + languages.filter { !it.isAuto } else languages.filter { !it.isAuto }
        if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.name.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("language_picker_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.smoothButtonPress(scaleDown = 0.90f)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Language Picker")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.smoothButtonPress(scaleDown = 0.90f)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            placeholder = { Text("Search language or country...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Fast Selection Chips (Favorites / Recents)
        if (searchQuery.isEmpty()) {
            val quickPicks = remember(languages) {
                languages.filter { it.isFavorite || listOf("en", "hi", "ja", "zh", "es").contains(it.code) }.take(6)
            }
            if (quickPicks.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(quickPicks) { lang ->
                        FilterChip(
                            selected = lang.code == selected.code,
                            onClick = { onSelected(lang) },
                            label = { Text("${lang.flagEmoji} ${lang.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.smoothButtonPress(scaleDown = 0.92f)
                        )
                    }
                }
            }
        }

        // Full Languages List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            items(filteredLanguages, key = { it.code }) { lang ->
                val isCurrent = lang.code == selected.code
                val isDownloading = downloadingLanguageCode == lang.code

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .smoothButtonPress(scaleDown = 0.96f)
                        .clickable { onSelected(lang) }
                        .testTag("lang_item_${lang.code}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = lang.flagEmoji, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lang.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (lang.isDownloadedOffline) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Offline Model Downloaded",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Right actions (Favorite toggle & Offline Model Download action)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!lang.isAuto && translationRepository != null) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(4.dp)
                                    )
                                } else if (lang.isDownloadedOffline) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                translationRepository.deleteModel(lang.code)
                                                Toast.makeText(context, "${lang.name} offline model deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .smoothButtonPress(scaleDown = 0.88f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Offline Model",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else if (lang.supportsOffline) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                downloadingLanguageCode = lang.code
                                                when (val result = translationRepository.downloadModel(lang.code)) {
                                                    is AppResult.Success -> {
                                                        Toast.makeText(context, "${lang.name} downloaded for offline translation", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is AppResult.Error -> {
                                                        Toast.makeText(context, "Download failed: ${result.error}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is AppResult.Loading -> {}
                                                }
                                                downloadingLanguageCode = null
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .smoothButtonPress(scaleDown = 0.88f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = "Download for Offline Translation",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (!lang.isAuto && languageRepository != null) {
                                IconButton(
                                    onClick = { languageRepository.toggleFavorite(lang) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .smoothButtonPress(scaleDown = 0.88f)
                                ) {
                                    Icon(
                                        imageVector = if (lang.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Toggle Favorite",
                                        tint = if (lang.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
