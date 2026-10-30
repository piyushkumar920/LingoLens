package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.NetworkConnectionState
import com.example.core.network.NetworkConnectivityMonitor
import com.example.modules.offline.model.DownloadState
import com.example.modules.offline.model.OfflinePackage
import com.example.modules.offline.repository.OfflinePackageRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.animation.smoothButtonPress
import kotlinx.coroutines.launch

@Composable
fun OfflineLanguagesScreen(
    offlinePackageRepository: OfflinePackageRepository,
    connectivityMonitor: NetworkConnectivityMonitor? = null,
    ttsRepository: TextToSpeechRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val packages by offlinePackageRepository.packages.collectAsState()
    val connectionState by connectivityMonitor?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(NetworkConnectionState.ONLINE) }

    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("All") } // All, Installed, Available
    var packageToDelete by remember { mutableStateOf<OfflinePackage?>(null) }

    val installedPackages = remember(packages) {
        packages.filter { it.state == DownloadState.DOWNLOADED }
    }

    val filteredPackages = remember(packages, searchQuery, filterTab) {
        packages.filter { pkg ->
            val matchesQuery = searchQuery.isBlank() ||
                    pkg.languageName.contains(searchQuery, ignoreCase = true) ||
                    pkg.languageCode.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (filterTab) {
                "Installed" -> pkg.state == DownloadState.DOWNLOADED
                "Not Installed" -> pkg.state != DownloadState.DOWNLOADED
                else -> true
            }

            matchesQuery && matchesTab
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("offline_languages_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title and Network status badge
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Offline Languages",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "On-device ML Kit neural translation models",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Connection State Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (connectionState) {
                            NetworkConnectionState.ONLINE -> Color(0xFF10B981).copy(alpha = 0.15f)
                            NetworkConnectionState.LIMITED_CONNECTION -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            NetworkConnectionState.OFFLINE -> Color(0xFF6B7280).copy(alpha = 0.15f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when (connectionState) {
                                            NetworkConnectionState.ONLINE -> Color(0xFF10B981)
                                            NetworkConnectionState.LIMITED_CONNECTION -> Color(0xFFF59E0B)
                                            NetworkConnectionState.OFFLINE -> Color(0xFF6B7280)
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = connectionState.name.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (connectionState) {
                                    NetworkConnectionState.ONLINE -> Color(0xFF065F46)
                                    NetworkConnectionState.LIMITED_CONNECTION -> Color(0xFF92400E)
                                    NetworkConnectionState.OFFLINE -> Color(0xFF374151)
                                }
                            )
                        }
                    }
                }
            }

            // Storage Information Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_info_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "On-Device Storage Info",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${installedPackages.size} installed • ${offlinePackageRepository.totalStorageUsedFormatted} used",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Internal App Storage",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Search and Filtering Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search language or code (e.g., Spanish, es)...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_language_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filterTab == "All",
                        onClick = { filterTab = "All" },
                        label = { Text("All (${packages.size})") },
                        modifier = Modifier
                            .smoothButtonPress(scaleDown = 0.94f)
                            .testTag("filter_all_languages")
                    )
                    FilterChip(
                        selected = filterTab == "Installed",
                        onClick = { filterTab = "Installed" },
                        label = { Text("Installed (${installedPackages.size})") },
                        modifier = Modifier
                            .smoothButtonPress(scaleDown = 0.94f)
                            .testTag("filter_installed_languages")
                    )
                    FilterChip(
                        selected = filterTab == "Not Installed",
                        onClick = { filterTab = "Not Installed" },
                        label = { Text("Not Installed (${packages.size - installedPackages.size})") },
                        modifier = Modifier
                            .smoothButtonPress(scaleDown = 0.94f)
                            .testTag("filter_not_installed_languages")
                    )
                }
            }

            // Offline Guidelines / Instructions Box
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Installed models allow 100% offline text translation, Camera OCR, and Travel phrase playback without internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Language Items List
            items(filteredPackages, key = { it.languageCode }) { pkg ->
                val isInstalled = pkg.state == DownloadState.DOWNLOADED
                val isDownloading = pkg.state == DownloadState.DOWNLOADING
                val hasTtsVoice = ttsRepository?.isLanguageAvailable(pkg.languageCode) ?: true

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInstalled) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_package_card_${pkg.languageCode}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = pkg.flagEmoji,
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pkg.languageName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${pkg.languageCode.uppercase()})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Storage information
                                    Text(
                                        text = "Storage: ${pkg.sizeFormatted} • ${if (isInstalled) "Allocated in Internal Storage" else "Required"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Installation Status Badge
                            if (isInstalled) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Installed",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Installed",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "Not Installed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Offline availability description
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline Availability: ${pkg.offlineAvailability}" +
                                        if (hasTtsVoice) " • Device TTS Ready" else " • Voice TTS Data Not On Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Download Progress Indicator (if active)
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading neural weights... ${pkg.progressPercent}%",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pkg.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: Download or Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isInstalled) {
                                if (!pkg.isCoreModel) {
                                    OutlinedButton(
                                        onClick = { packageToDelete = pkg },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .smoothButtonPress(scaleDown = 0.94f)
                                            .testTag("delete_language_${pkg.languageCode}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete (${pkg.sizeFormatted})")
                                    }
                                } else {
                                    Text(
                                        text = "Protected Core Language",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        // Do not download without user action unless explicitly requested
                                        scope.launch {
                                            val res = offlinePackageRepository.downloadPackage(pkg.languageCode)
                                            if (res is com.example.core.result.AppResult.Success) {
                                                Toast.makeText(context, "Downloaded ${pkg.languageName} for offline use", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isDownloading,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .smoothButtonPress(scaleDown = 0.94f)
                                        .testTag("download_language_${pkg.languageCode}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download (${pkg.sizeFormatted})")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation dialog before deleting language model
    if (packageToDelete != null) {
        val pkg = packageToDelete!!
        AlertDialog(
            onDismissRequest = { packageToDelete = null },
            title = { Text("Delete ${pkg.languageName}?") },
            text = {
                Text(
                    "This will remove the on-device ML Kit translation model (${pkg.sizeFormatted}) from internal storage. Offline translation for ${pkg.languageName} will be disabled until downloaded again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            offlinePackageRepository.deletePackage(pkg.languageCode)
                            Toast.makeText(context, "Deleted ${pkg.languageName} model", Toast.LENGTH_SHORT).show()
                            packageToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { packageToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
