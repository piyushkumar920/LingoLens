package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * Selection target mode for the ML Kit translation service.
 */
enum class MlKitLanguageTarget {
    SOURCE,
    TARGET
}

/**
 * High-performance, Material 3 Bottom Sheet allowing users to select Source and Target
 * languages for Google ML Kit on-device and cloud translation services.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MlKitLanguageSelectionBottomSheet(
    sourceLanguage: Language,
    targetLanguage: Language,
    availableLanguages: List<Language>,
    initialTarget: MlKitLanguageTarget = MlKitLanguageTarget.SOURCE,
    languageRepository: LanguageRepository? = null,
    translationRepository: TranslationRepository? = null,
    onSourceLanguageSelected: (Language) -> Unit,
    onTargetLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTarget by remember { mutableStateOf(initialTarget) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var downloadingLanguageCode by remember { mutableStateOf<String?>(null) }

    val downloadedModels by (translationRepository?.getDownloadedModels()?.collectAsState() ?: remember { mutableStateOf(emptySet()) })

    val isSource = selectedTarget == MlKitLanguageTarget.SOURCE
    val currentSelectedLang = if (isSource) sourceLanguage else targetLanguage

    val filteredLanguages = remember(searchQuery, availableLanguages, isSource) {
        val baseList = if (isSource) {
            listOf(Language.AUTO_DETECT) + availableLanguages.filter { !it.isAuto }
        } else {
            availableLanguages.filter { !it.isAuto }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.name.lowercase().contains(q) ||
                it.nativeName.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("mlkit_language_selection_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row: Title, active target indicator, and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ML Kit Translation Languages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select source and target for real-time ML Kit neural models",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.smoothButtonPress()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Language Picker"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dual Tab Switcher: [ Source Language ] vs [ Target Language ]
            PrimaryTabRow(
                selectedTabIndex = if (isSource) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("mlkit_language_tabs")
            ) {
                Tab(
                    selected = isSource,
                    onClick = {
                        selectedTarget = MlKitLanguageTarget.SOURCE
                        searchQuery = ""
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = sourceLanguage.flagEmoji)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Source: ${sourceLanguage.name}",
                                fontWeight = if (isSource) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier
                        .smoothButtonPress(scaleDown = 0.96f)
                        .testTag("mlkit_source_lang_tab")
                )

                Tab(
                    selected = !isSource,
                    onClick = {
                        selectedTarget = MlKitLanguageTarget.TARGET
                        searchQuery = ""
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = targetLanguage.flagEmoji)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Target: ${targetLanguage.name}",
                                fontWeight = if (!isSource) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier
                        .smoothButtonPress(scaleDown = 0.96f)
                        .testTag("mlkit_target_lang_tab")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search 59+ ML Kit languages or codes...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mlkit_language_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Popular / Recent Quick-Picks Row
            if (searchQuery.isEmpty()) {
                val quickPicks = remember(availableLanguages) {
                    val priorityCodes = listOf("en", "es", "hi", "fr", "ja", "de", "zh", "ar")
                    priorityCodes.mapNotNull { code -> availableLanguages.find { it.code == code } }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (isSource) {
                        item {
                            FilterChip(
                                selected = sourceLanguage.isAuto,
                                onClick = {
                                    onSourceLanguageSelected(Language.AUTO_DETECT)
                                    onDismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text("Auto Detect") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.smoothButtonPress()
                            )
                        }
                    }

                    items(quickPicks) { lang ->
                        val isSelected = lang.code == currentSelectedLang.code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSource) {
                                    onSourceLanguageSelected(lang)
                                } else {
                                    onTargetLanguageSelected(lang)
                                }
                                onDismiss()
                            },
                            label = { Text("${lang.flagEmoji} ${lang.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.smoothButtonPress()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Language List with Offline Download Badges
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .testTag("mlkit_language_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredLanguages, key = { it.code }) { language ->
                    val isSelected = language.code == currentSelectedLang.code
                    val isOfflineDownloaded = language.isDownloadedOffline || downloadedModels.contains(language.code)
                    val isDownloading = downloadingLanguageCode == language.code

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .smoothButtonPress(scaleDown = 0.97f)
                            .clickable {
                                languageRepository?.markLanguageUsed(language)
                                if (isSource) {
                                    onSourceLanguageSelected(language)
                                } else {
                                    onTargetLanguageSelected(language)
                                }
                                onDismiss()
                            }
                            .testTag("mlkit_lang_item_${language.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = language.flagEmoji,
                                    fontSize = 22.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = language.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (language.isAuto) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Neural Detector",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!language.isAuto) {
                                        Text(
                                            text = "${language.nativeName} · ${language.code.uppercase()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Offline model badge or download action
                                if (!language.isAuto && translationRepository != null) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isOfflineDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Offline ready",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    downloadingLanguageCode = language.code
                                                    val res = translationRepository.downloadModel(language.code)
                                                    downloadingLanguageCode = null
                                                    if (res is AppResult.Success) {
                                                        Toast.makeText(context, "${language.name} offline model ready", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .smoothButtonPress(scaleDown = 0.88f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = "Download ${language.name} model for offline translation",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (isSelected) {
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
}

/**
 * Elegant Dropdown Menu for selecting ML Kit Source or Target languages.
 */
@Composable
fun MlKitLanguageDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    languages: List<Language>,
    selectedLanguage: Language,
    isSource: Boolean,
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, languages, isSource) {
        val base = if (isSource) listOf(Language.AUTO_DETECT) + languages.filter { !it.isAuto } else languages.filter { !it.isAuto }
        if (searchQuery.isBlank()) {
            base.take(15)
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }.take(15)
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .width(260.dp)
            .heightIn(max = 380.dp)
            .testTag("mlkit_language_dropdown_menu")
    ) {
        // Search field inside dropdown
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        filtered.forEach { lang ->
            val isSelected = lang.code == selectedLanguage.code
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flagEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lang.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onLanguageSelected(lang)
                    onDismissRequest()
                },
                modifier = Modifier.testTag("dropdown_item_${lang.code}")
            )
        }
    }
}

/**
 * Compact, interactive Dual Language Selector Bar featuring both Bottom Sheet
 * and Dropdown Menu capabilities for Google ML Kit translation.
 */
@Composable
fun MlKitLanguageSelectorDualControl(
    sourceLanguage: Language,
    targetLanguage: Language,
    availableLanguages: List<Language>,
    onSourceLanguageChanged: (Language) -> Unit,
    onTargetLanguageChanged: (Language) -> Unit,
    onSwapLanguages: () -> Unit,
    modifier: Modifier = Modifier,
    languageRepository: LanguageRepository? = null,
    translationRepository: TranslationRepository? = null,
    useDropdown: Boolean = false
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var activeSheetTarget by remember { mutableStateOf(MlKitLanguageTarget.SOURCE) }

    var showSourceDropdown by remember { mutableStateOf(false) }
    var showTargetDropdown by remember { mutableStateOf(false) }

    var swapAngle by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapAngle,
        animationSpec = tween(durationMillis = 280),
        label = "swap_rotation"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mlkit_dual_language_selector")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source Language Button / Anchor
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .smoothButtonPress(scaleDown = 0.95f)
                        .clickable {
                            if (useDropdown) {
                                showSourceDropdown = true
                            } else {
                                activeSheetTarget = MlKitLanguageTarget.SOURCE
                                showBottomSheet = true
                            }
                        }
                        .testTag("mlkit_source_language_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                            Text(
                                text = if (sourceLanguage.isAuto) "Auto Detect" else "Source",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                MlKitLanguageDropdownMenu(
                    expanded = showSourceDropdown,
                    onDismissRequest = { showSourceDropdown = false },
                    languages = availableLanguages,
                    selectedLanguage = sourceLanguage,
                    isSource = true,
                    onLanguageSelected = onSourceLanguageChanged
                )
            }

            // Interactive Swap Button with spring bounce
            IconButton(
                onClick = {
                    swapAngle += 180f
                    onSwapLanguages()
                },
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .rotate(animatedRotation)
                    .smoothButtonPress(scaleDown = 0.88f)
                    .testTag("mlkit_swap_languages_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap Languages",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Target Language Button / Anchor
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .smoothButtonPress(scaleDown = 0.95f)
                        .clickable {
                            if (useDropdown) {
                                showTargetDropdown = true
                            } else {
                                activeSheetTarget = MlKitLanguageTarget.TARGET
                                showBottomSheet = true
                            }
                        }
                        .testTag("mlkit_target_language_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                            Text(
                                text = "Target",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                MlKitLanguageDropdownMenu(
                    expanded = showTargetDropdown,
                    onDismissRequest = { showTargetDropdown = false },
                    languages = availableLanguages,
                    selectedLanguage = targetLanguage,
                    isSource = false,
                    onLanguageSelected = onTargetLanguageChanged
                )
            }
        }
    }

    // Modal Bottom Sheet Dialog
    if (showBottomSheet) {
        MlKitLanguageSelectionBottomSheet(
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            availableLanguages = availableLanguages,
            initialTarget = activeSheetTarget,
            languageRepository = languageRepository,
            translationRepository = translationRepository,
            onSourceLanguageSelected = onSourceLanguageChanged,
            onTargetLanguageSelected = onTargetLanguageChanged,
            onDismiss = { showBottomSheet = false }
        )
    }
}
