package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.result.AppResult
import com.example.modules.favorites.model.FavoritePhrase
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationType
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.offline.repository.OfflinePackageRepository
import com.example.modules.travel.model.DestinationCountry
import com.example.modules.travel.model.TravelCategory
import com.example.modules.travel.model.TravelPhrase
import com.example.modules.travel.repository.TravelRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.components.DestinationSelectorSheet
import com.example.ui.components.OfflinePackManagerSheet
import com.example.ui.components.TravelPhraseCard
import com.example.ui.navigation.Screen
import com.example.ui.animation.smoothButtonPress
import kotlinx.coroutines.launch

@Composable
fun TravelScreen(
    travelRepository: TravelRepository,
    ttsRepository: TextToSpeechRepository? = null,
    historyRepository: HistoryRepository? = null,
    favoritesRepository: FavoritesRepository? = null,
    offlinePackageRepository: OfflinePackageRepository? = null,
    onNavigateToScreen: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Destination Country / Language selection
    val availableDestinations = remember { travelRepository.getSupportedDestinations() }
    var selectedDestination by remember {
        mutableStateOf(availableDestinations.firstOrNull() ?: DestinationCountry("Spain", "🇪🇸", "es", "Spanish", "EUR", "€"))
    }
    var showDestinationSheet by remember { mutableStateOf(false) }
    var showOfflinePackSheet by remember { mutableStateOf(false) }
    var showCurrencyPlaceholderDialog by remember { mutableStateOf(false) }

    // Tab / Category selection
    var selectedCategoryTab by remember { mutableStateOf<TravelCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Collect saved/favorite phrases from favorites repository
    val savedFavorites by favoritesRepository?.getAllFavorites()?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<FavoritePhrase>()) }

    // Phrases flow for currently selected destination & tab
    val currentPhrasesFlow = remember(selectedDestination.primaryLanguageCode, selectedCategoryTab) {
        when (selectedCategoryTab) {
            null -> travelRepository.getAllPhrases(selectedDestination.primaryLanguageCode)
            TravelCategory.COMMON -> travelRepository.getCommonPhrases(selectedDestination.primaryLanguageCode)
            TravelCategory.EMERGENCY -> travelRepository.getEmergencyPhrases(selectedDestination.primaryLanguageCode)
            TravelCategory.SAVED -> travelRepository.getAllPhrases(selectedDestination.primaryLanguageCode)
            else -> travelRepository.getPhrasesForCategory(selectedCategoryTab!!, selectedDestination.primaryLanguageCode)
        }
    }
    val allCategoryPhrases by currentPhrasesFlow.collectAsState(initial = emptyList())

    // Convert Saved Favorites to TravelPhrase representation if SAVED tab is active
    val displayedPhrases = remember(allCategoryPhrases, savedFavorites, selectedCategoryTab, searchQuery, selectedDestination) {
        val baseList = if (selectedCategoryTab == TravelCategory.SAVED) {
            savedFavorites.map { fav ->
                TravelPhrase(
                    id = "fav_${fav.id}",
                    originalEnglish = fav.sourceText,
                    translatedText = fav.translatedText,
                    category = TravelCategory.SAVED,
                    targetLanguageCode = fav.targetLanguage
                )
            }
        } else {
            allCategoryPhrases
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.originalEnglish.contains(searchQuery, ignoreCase = true) ||
                it.translatedText.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("travel_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Travel Destination Selector Card
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Travel Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Destination tools, common & emergency phrases",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Destination Country & Language Selector Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .smoothButtonPress(scaleDown = 0.95f)
                        .clickable { showDestinationSheet = true }
                        .testTag("destination_selector_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDestination.flagEmoji,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Destination: ${selectedDestination.countryName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Language: ${selectedDestination.primaryLanguageName} (${selectedDestination.primaryLanguageCode.uppercase()})" +
                                            if (selectedDestination.currencyCode != null) " • Currency: ${selectedDestination.currencyCode}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Change destination",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Quick Travel Tools: Camera, Voice, Conversation, Offline Pack, Currency
            item {
                Text(
                    text = "Travel Tools & Shortcuts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Camera & Voice Translators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TravelToolCard(
                            title = "Camera Translator",
                            subtitle = "Menus, signs, OCR",
                            icon = Icons.Default.CameraAlt,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("travel_camera_shortcut"),
                            onClick = { onNavigateToScreen?.invoke(Screen.Camera.route) }
                        )
                        TravelToolCard(
                            title = "Voice Translator",
                            subtitle = "Speak & translate",
                            icon = Icons.Default.Mic,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("travel_voice_shortcut"),
                            onClick = { onNavigateToScreen?.invoke(Screen.Voice.route) }
                        )
                    }

                    // Row 2: Conversation Mode & Offline Language Pack
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TravelToolCard(
                            title = "Conversation Mode",
                            subtitle = "Two-way talk with locals",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            accentColor = Color(0xFF0284C7),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("travel_conversation_shortcut"),
                            onClick = { onNavigateToScreen?.invoke(Screen.Conversation.route) }
                        )
                        TravelToolCard(
                            title = "Offline Pack",
                            subtitle = "No roaming or data",
                            icon = Icons.Default.OfflinePin,
                            accentColor = Color(0xFF059669),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("travel_offline_pack_shortcut"),
                            onClick = { showOfflinePackSheet = true }
                        )
                    }

                    // Row 3: Currency shortcut placeholder (as requested: placeholder only if a real currency service is implemented later)
                    TravelToolCard(
                        title = "Currency Calculator",
                        subtitle = if (selectedDestination.currencyCode != null) {
                            "${selectedDestination.currencyCode} (${selectedDestination.currencySymbol}) Exchange Rates • Offline Placeholder"
                        } else {
                            "Foreign exchange rates • Offline Placeholder"
                        },
                        icon = Icons.Default.CurrencyExchange,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("travel_currency_shortcut"),
                        onClick = { showCurrencyPlaceholderDialog = true }
                    )
                }
            }

            // Phrasebook Navigation & Filter Chips
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Travel Phrasebook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Search field for instant filtering
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search phrases in ${selectedDestination.countryName}...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search phrases",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("travel_search_phrases_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Row: Common, Emergency, Saved, All
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryTab == null,
                            onClick = { selectedCategoryTab = null },
                            label = { Text("All") },
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategoryTab == TravelCategory.COMMON,
                            onClick = { selectedCategoryTab = TravelCategory.COMMON },
                            label = { Text("💬 Common Phrases") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_common")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategoryTab == TravelCategory.EMERGENCY,
                            onClick = { selectedCategoryTab = TravelCategory.EMERGENCY },
                            label = { Text("🚨 Emergency") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.testTag("filter_chip_emergency")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategoryTab == TravelCategory.SAVED,
                            onClick = { selectedCategoryTab = TravelCategory.SAVED },
                            label = { Text("⭐ Saved Phrases (${savedFavorites.size})") },
                            modifier = Modifier.testTag("filter_chip_saved")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategoryTab == TravelCategory.DINING,
                            onClick = { selectedCategoryTab = TravelCategory.DINING },
                            label = { Text("🍽️ Dining") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategoryTab == TravelCategory.TRANSIT,
                            onClick = { selectedCategoryTab = TravelCategory.TRANSIT },
                            label = { Text("🚆 Transit") }
                        )
                    }
                }
            }

            // Emergency Notice Banner (Safe & transparent)
            if (selectedCategoryTab == TravelCategory.EMERGENCY) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Emergency Phrase Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Show these translated phrases directly to doctors, hospital staff, or local emergency services. In a life safety situation, always contact local emergency dispatch.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Phrases List
            if (displayedPhrases.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedCategoryTab == TravelCategory.SAVED) "No Saved Phrases" else "No matching phrases found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedCategoryTab == TravelCategory.SAVED) {
                                    "Tap the star icon on any phrase in Common, Emergency, or Translator to pin it here."
                                } else {
                                    "Try clearing your search query or selecting another category."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedPhrases, key = { "${it.id}_${it.originalEnglish}" }) { phrase ->
                    val isSaved = savedFavorites.any { it.sourceText == phrase.originalEnglish }

                    TravelPhraseCard(
                        phrase = phrase,
                        ttsRepository = ttsRepository,
                        isSaved = isSaved,
                        onToggleSave = {
                            scope.launch {
                                if (isSaved) {
                                    favoritesRepository?.removeFavoriteByText(phrase.originalEnglish, phrase.targetLanguageCode)
                                    Toast.makeText(context, "Removed from saved", Toast.LENGTH_SHORT).show()
                                } else {
                                    favoritesRepository?.saveFavorite(
                                        FavoritePhrase(
                                            sourceText = phrase.originalEnglish,
                                            translatedText = phrase.translatedText,
                                            sourceLanguage = "en",
                                            targetLanguage = phrase.targetLanguageCode,
                                            category = phrase.category.displayName,
                                            notes = "From Travel Mode for ${selectedDestination.countryName}"
                                        )
                                    )
                                    Toast.makeText(context, "Saved to your phrasebook", Toast.LENGTH_SHORT).show()
                                }

                                // Also log to local translation history
                                historyRepository?.insertHistoryItem(
                                    TranslationHistoryItem(
                                        sourceText = phrase.originalEnglish,
                                        translatedText = phrase.translatedText,
                                        sourceLanguage = "en",
                                        targetLanguage = phrase.targetLanguageCode,
                                        translationType = TranslationType.TRAVEL,
                                        isFavorite = !isSaved
                                    )
                                )
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Sheet 1: Destination Selector
    if (showDestinationSheet) {
        DestinationSelectorSheet(
            destinations = availableDestinations,
            selectedDestination = selectedDestination,
            onDestinationSelected = { destination ->
                selectedDestination = destination
                Toast.makeText(context, "Destination set to ${destination.countryName}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDestinationSheet = false }
        )
    }

    // Modal Sheet 2: Offline Pack Manager
    if (showOfflinePackSheet && offlinePackageRepository != null) {
        OfflinePackManagerSheet(
            destination = selectedDestination,
            offlinePackageRepository = offlinePackageRepository,
            onDismiss = { showOfflinePackSheet = false }
        )
    }

    // Modal Dialog: Currency shortcut placeholder (strictly per requirement: placeholder only if a real currency service is implemented later)
    if (showCurrencyPlaceholderDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyPlaceholderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Currency Service")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Destination Currency: ${selectedDestination.currencyCode ?: "Standard"} (${selectedDestination.currencySymbol ?: "$"})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live exchange rate integration is configured as a future modular service placeholder. Real-time rates will be displayed here once connected to an active foreign exchange API.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyPlaceholderDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun TravelToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
            .smoothButtonPress(scaleDown = 0.94f)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
