package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.image.ImageEnhanceMode
import com.example.core.result.AppResult
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.image_translation.model.ImageOcrStatus
import com.example.modules.image_translation.repository.ImageTranslationRepository
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.ocr.model.OcrScript
import com.example.modules.translation.model.OverlayMode
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.components.BlockInspectionBottomSheet
import com.example.ui.components.ImageTranscriptSheet
import com.example.ui.components.LanguageSelectorBar
import com.example.ui.components.ZoomableImageTranslationCanvas
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTranslationStudioScreen(
    imageTranslationRepository: ImageTranslationRepository,
    languageRepository: LanguageRepository,
    ttsRepository: TextToSpeechRepository? = null,
    favoritesRepository: FavoritesRepository? = null,
    historyRepository: HistoryRepository? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by imageTranslationRepository.uiState.collectAsState()
    val languages by languageRepository.getSupportedLanguages().collectAsState(initial = emptyList())

    var showTranscriptSheet by remember { mutableStateOf(false) }
    var showReprocessMenu by remember { mutableStateOf(false) }
    var showEnhanceMenu by remember { mutableStateOf(false) }
    var showScriptMenu by remember { mutableStateOf(false) }

    // 1. Photo Picker Launcher (Gallery)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                imageTranslationRepository.loadImage(context, uri)
            }
        }
    }

    // 2. Generic File Chooser Launcher (Screenshots / Documents / Downloads)
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                imageTranslationRepository.loadImage(context, uri)
            }
        }
    }

    // 3. Camera Capture Launcher (Take Photo)
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                imageTranslationRepository.setBitmap(bitmap, autoProcess = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("image_translation_studio_screen")
    ) {
        // Top Bar & Language Selector
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Language Selection Bar
                LanguageSelectorBar(
                    sourceLanguage = uiState.sourceLanguage,
                    targetLanguage = uiState.targetLanguage,
                    availableLanguages = languages,
                    onSourceLanguageChanged = { newSource ->
                        coroutineScope.launch {
                            imageTranslationRepository.changeLanguages(newSource, uiState.targetLanguage)
                        }
                    },
                    onTargetLanguageChanged = { newTarget ->
                        coroutineScope.launch {
                            imageTranslationRepository.changeLanguages(uiState.sourceLanguage, newTarget)
                        }
                    },
                    onSwapLanguages = {
                        if (!uiState.sourceLanguage.isAuto) {
                            val temp = uiState.sourceLanguage
                            coroutineScope.launch {
                                imageTranslationRepository.changeLanguages(uiState.targetLanguage, temp)
                            }
                        } else {
                            Toast.makeText(context, "Cannot swap with Auto-Detect", Toast.LENGTH_SHORT).show()
                        }
                    },
                    languageRepository = languageRepository,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Control Strip (Overlay modes & Reprocess tools)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Overlay Mode Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(OverlayMode.entries) { mode ->
                            val isSelected = uiState.overlayMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { imageTranslationRepository.setOverlayMode(mode) },
                                label = {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("filter_chip_${mode.name.lowercase()}")
                            )
                        }
                    }

                    // Reprocess / Rotate Action Icons
                    if (uiState.hasImage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rotate 90° CW
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        imageTranslationRepository.rotateImage(90f)
                                    }
                                },
                                modifier = Modifier.size(36.dp).testTag("rotate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rotate90DegreesCw,
                                    contentDescription = "Rotate 90°",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Enhancement filter dropdown
                            Box {
                                IconButton(
                                    onClick = { showEnhanceMenu = true },
                                    modifier = Modifier.size(36.dp).testTag("enhance_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = "Enhance Filter",
                                        tint = if (uiState.enhanceMode != ImageEnhanceMode.ORIGINAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showEnhanceMenu,
                                    onDismissRequest = { showEnhanceMenu = false }
                                ) {
                                    Text(
                                        text = "Image Enhancement",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    ImageEnhanceMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = mode.displayName,
                                                    fontWeight = if (uiState.enhanceMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (uiState.enhanceMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                showEnhanceMenu = false
                                                coroutineScope.launch {
                                                    imageTranslationRepository.setEnhancementMode(mode)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Script / OCR Reprocess menu
                            Box {
                                IconButton(
                                    onClick = { showScriptMenu = true },
                                    modifier = Modifier.size(36.dp).testTag("script_reprocess_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "OCR Script",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showScriptMenu,
                                    onDismissRequest = { showScriptMenu = false }
                                ) {
                                    Text(
                                        text = "OCR Script Recognizer",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    OcrScript.entries.forEach { script ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = script.displayName,
                                                    fontWeight = if (uiState.script == script) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (uiState.script == script) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                showScriptMenu = false
                                                coroutineScope.launch {
                                                    imageTranslationRepository.reprocess(context, script = script)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Reset Zoom
                            if (uiState.zoomScale > 1.05f || uiState.panOffsetX != 0f || uiState.panOffsetY != 0f) {
                                IconButton(
                                    onClick = { imageTranslationRepository.resetTransform() },
                                    modifier = Modifier.size(36.dp).testTag("reset_zoom_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOutMap,
                                        contentDescription = "Reset Zoom",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Viewer & Interactive Image Translation Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            if (uiState.displayedBitmap != null) {
                // Interactive Zoomable Canvas
                ZoomableImageTranslationCanvas(
                    bitmap = uiState.displayedBitmap!!,
                    translatedBlocks = uiState.translatedBlocks,
                    overlayMode = uiState.overlayMode,
                    selectedBlock = uiState.selectedBlock,
                    onBlockClick = { block ->
                        imageTranslationRepository.selectBlock(block)
                    },
                    zoomScale = uiState.zoomScale,
                    panOffsetX = uiState.panOffsetX,
                    panOffsetY = uiState.panOffsetY,
                    onTransformChange = { scale, panX, panY ->
                        imageTranslationRepository.updateTransform(scale, panX, panY)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Processing Indicator Overlay
                if (uiState.isBusy) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("processing_overlay")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = when (uiState.status) {
                                        ImageOcrStatus.LOADING_IMAGE -> "Optimizing and loading image..."
                                        ImageOcrStatus.RUNNING_OCR -> "Scanning text and detecting scripts..."
                                        ImageOcrStatus.TRANSLATING -> "Translating detected regions..."
                                        else -> "Processing..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Auto-orienting & bounds-safe processing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Info Pill at bottom left (Block count & detected language)
                if (uiState.status == ImageOcrStatus.SUCCESS && uiState.translatedBlocks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.translatedBlocks.size} blocks translated • ${uiState.processingTimeMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            if (uiState.detectedMainLanguage != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${uiState.detectedMainLanguage!!.uppercase()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Transcript Button at bottom right
                if (uiState.translatedBlocks.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clickable { showTranscriptSheet = true }
                            .testTag("open_transcript_sheet_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transcript",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else {
                // Empty State / Image Selection Hub
                ImageImportPickerHub(
                    onSelectGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTakePhoto = {
                        cameraLauncher.launch(null)
                    },
                    onSelectScreenshot = {
                        fileChooserLauncher.launch("image/*")
                    }
                )
            }

            // No Text Found Banner
            if (uiState.status == ImageOcrStatus.NO_TEXT_FOUND) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .testTag("no_text_found_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Text Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try rotating the image 90°, applying the 'High Contrast' or 'B&W Document' filter, or switching the OCR Script in the top toolbar.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        imageTranslationRepository.rotateImage(90f)
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotate 90°")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        imageTranslationRepository.setEnhancementMode(ImageEnhanceMode.HIGH_CONTRAST)
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enhance")
                            }
                        }
                    }
                }
            }

            // OCR Error Banner
            if (uiState.status == ImageOcrStatus.ERROR && uiState.errorMessage != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .testTag("ocr_error_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Recognition Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.errorMessage ?: "Unknown error occurred.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    imageTranslationRepository.reprocess(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Recognition")
                        }
                    }
                }
            }
        }

        // Bottom Action Bar (Pick Gallery, Take Photo, Import Screenshot, Reprocess)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                FilledTonalButton(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("bottom_gallery_button")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", style = MaterialTheme.typography.labelMedium)
                }

                // Camera button
                FilledTonalButton(
                    onClick = {
                        cameraLauncher.launch(null)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("bottom_camera_button")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", style = MaterialTheme.typography.labelMedium)
                }

                // Screenshot / File button
                FilledTonalButton(
                    onClick = {
                        fileChooserLauncher.launch("image/*")
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("bottom_screenshot_button")
                ) {
                    Icon(imageVector = Icons.Default.ScreenSearchDesktop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Screenshot", style = MaterialTheme.typography.labelMedium)
                }

                // Reprocess button if image is loaded
                if (uiState.hasImage) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                imageTranslationRepository.reprocess(context)
                            }
                        },
                        modifier = Modifier.testTag("reprocess_action_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reprocess Image")
                    }
                }
            }
        }
    }

    // Block Inspection Bottom Sheet when a translated block is tapped
    uiState.selectedBlock?.let { block ->
        BlockInspectionBottomSheet(
            block = block,
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onDismiss = { imageTranslationRepository.selectBlock(null) },
            ttsRepository = ttsRepository,
            favoritesRepository = favoritesRepository,
            historyRepository = historyRepository
        )
    }

    // Full Document Transcript Bottom Sheet
    if (showTranscriptSheet && uiState.translatedBlocks.isNotEmpty()) {
        ImageTranscriptSheet(
            blocks = uiState.translatedBlocks,
            sourceLanguage = uiState.sourceLanguage,
            targetLanguage = uiState.targetLanguage,
            onDismiss = { showTranscriptSheet = false },
            onSelectBlock = { block ->
                showTranscriptSheet = false
                imageTranslationRepository.selectBlock(block)
            },
            ttsRepository = ttsRepository
        )
    }
}

@Composable
private fun ImageImportPickerHub(
    onSelectGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectScreenshot: () -> Unit,
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
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Photo & Document Translation",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select any photo, screenshot, menu, receipt, or document to scan, translate, and inspect interactive regions with zoom & speech.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 3 Big Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSelectGallery,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("hub_select_gallery_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Select from Gallery", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            FilledTonalButton(
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("hub_take_photo_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Take New Photo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onSelectScreenshot,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("hub_import_screenshot_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.ScreenSearchDesktop, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Import Screenshot / File", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
