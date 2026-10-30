package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.modules.favorites.model.FavoritePhrase
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.animation.smoothButtonPress
import com.example.ui.components.EmptyStateView
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    ttsRepository: TextToSpeechRepository? = null,
    modifier: Modifier = Modifier
) {
    val favorites by favoritesRepository.getAllFavorites().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredFavorites = remember(favorites, selectedCategory) {
        when (selectedCategory) {
            "All" -> favorites
            "Vocabulary" -> favorites.filter { it.category.contains("Vocabulary", ignoreCase = true) }
            "Learning Phrases" -> favorites.filter { it.category.contains("Learning", ignoreCase = true) || it.category.contains("Phrase", ignoreCase = true) }
            else -> favorites.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("favorites_screen")
    ) {
        Text(
            text = "Saved Phrases & Vocabulary",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Saved expressions, word notes, and learning vocabulary",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val categories = listOf("All", "Learning Phrases", "Vocabulary")
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.smoothButtonPress(scaleDown = 0.94f),
                    leadingIcon = {
                        val icon = when (category) {
                            "Vocabulary" -> Icons.Default.School
                            "Learning Phrases" -> Icons.Default.Bookmark
                            else -> Icons.Default.Star
                        }
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredFavorites.isEmpty()) {
            EmptyStateView(
                title = if (selectedCategory == "All") "No Saved Items" else "No $selectedCategory Items",
                subtitle = "Save phrases and vocabulary words from the Learning Card or Translation to build your study collection.",
                icon = if (selectedCategory == "Vocabulary") Icons.Default.School else Icons.Default.Bookmark
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFavorites, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.category.contains("Vocabulary", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        text = "${item.sourceLanguage.uppercase()} → ${item.targetLanguage.uppercase()} • ${item.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.category.contains("Vocabulary", ignoreCase = true)) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Speak Button
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                ttsRepository?.speak(item.sourceText, item.sourceLanguage)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .smoothButtonPress(scaleDown = 0.88f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak original",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Repeat Button
                                    IconButton(
                                        onClick = {
                                            ttsRepository?.stop()
                                            scope.launch {
                                                ttsRepository?.speak(item.sourceText, item.sourceLanguage)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .smoothButtonPress(scaleDown = 0.88f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay,
                                            contentDescription = "Repeat",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { scope.launch { favoritesRepository.removeFavorite(item.id) } },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .smoothButtonPress(scaleDown = 0.88f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.sourceText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.translatedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Show notes/pronunciation if present
                            if (!item.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.notes!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
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
