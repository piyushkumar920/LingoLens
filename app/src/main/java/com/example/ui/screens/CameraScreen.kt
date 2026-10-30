package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.animation.smoothButtonPress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.permission.PermissionManager
import com.example.core.result.AppResult
import com.example.modules.camera.model.TapFocusState
import com.example.modules.camera.repository.CameraRepository
import com.example.modules.favorites.repository.FavoritesRepository
import com.example.modules.history.repository.HistoryRepository
import com.example.modules.image_translation.repository.ImageTranslationRepository
import com.example.modules.language.model.Language
import com.example.modules.language.repository.LanguageRepository
import com.example.modules.ocr.model.OcrScript
import com.example.modules.ocr.model.TextDetectionResult
import com.example.modules.ocr.repository.OcrRepository
import com.example.modules.translation.engine.VisualTranslationEngine
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.model.TranslatedBlock
import com.example.modules.translation.repository.TranslationRepository
import com.example.modules.tts.repository.TextToSpeechRepository
import com.example.ui.components.BlockInspectionBottomSheet
import com.example.ui.components.LanguageSelectorBar
import com.example.ui.components.PermissionRequiredView
import com.example.ui.components.VisualTranslationOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    languageRepository: LanguageRepository,
    cameraRepository: CameraRepository,
    ocrRepository: OcrRepository,
    modifier: Modifier = Modifier,
    translationRepository: TranslationRepository? = null,
    imageTranslationRepository: ImageTranslationRepository? = null,
    ttsRepository: TextToSpeechRepository? = null,
    favoritesRepository: FavoritesRepository? = null,
    historyRepository: HistoryRepository? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(PermissionManager.hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val languages by languageRepository.getSupportedLanguages().collectAsState(initial = emptyList())
    var sourceLang by remember { mutableStateOf(Language.AUTO_DETECT) }
    var targetLang by remember { mutableStateOf(Language("es", "Spanish", "Español", "🇪🇸")) }

    val cameraState by cameraRepository.cameraUiState.collectAsState()
    val currentScript by ocrRepository.currentScript.collectAsState()
    val liveOcrResult by ocrRepository.latestLiveDetection.collectAsState()

    var capturedOcrResult by remember { mutableStateOf<TextDetectionResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var ocrErrorMessage by remember { mutableStateOf<String?>(null) }

    var overlayMode by remember { mutableStateOf(OverlayMode.TRANSLATED) }
    var selectedBlock by remember { mutableStateOf<TranslatedBlock?>(null) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var tapFocusState by remember { mutableStateOf(TapFocusState()) }

    val hwMaxZoom = cameraState.maxZoomRatio
    val isHardwareZoomCapable = hwMaxZoom > 1.05f && !cameraState.isFrozen
    val hardwareZoomApplied = if (isHardwareZoomCapable) {
        cameraState.zoomRatio.coerceIn(1.0f, hwMaxZoom)
    } else {
        1.0f
    }
    val digitalZoomTarget = (cameraState.zoomRatio / hardwareZoomApplied).coerceIn(1.0f, 5.0f)

    val animatedZoomScale by animateFloatAsState(
        targetValue = digitalZoomTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "camera_zoom_scale"
    )

    // Visual Translation Engine with memory cache & debounce
    val visualTranslationEngine = remember(translationRepository) {
        translationRepository?.let { VisualTranslationEngine(it) }
    }

    val translationState by visualTranslationEngine?.state?.collectAsState()
        ?: remember { mutableStateOf(com.example.modules.translation.model.VisualTranslationState()) }

    // Active detection display: captured result when frozen, otherwise live result
    val displayOcrResult = if (cameraState.isFrozen) {
        capturedOcrResult ?: TextDetectionResult()
    } else {
        liveOcrResult
    }

    // Process OCR into Translation Engine with debouncing
    LaunchedEffect(displayOcrResult, sourceLang.code, targetLang.code, cameraState.autoOcrEnabled, cameraState.isFrozen) {
        if ((cameraState.autoOcrEnabled || cameraState.isFrozen) && visualTranslationEngine != null) {
            visualTranslationEngine.processOcrResult(
                detectionResult = displayOcrResult,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                scope = coroutineScope,
                debounceMs = if (cameraState.isFrozen) 0L else 200L
            )
        }
    }

    // Connect Camera Frame Pipeline with ML Kit Frame Consumer
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val frameConsumer = ocrRepository.createFrameConsumer()
            cameraRepository.setFrameConsumer(frameConsumer)
        }
    }

    // Preload translation model in background when entering camera screen
    LaunchedEffect(sourceLang.code, targetLang.code) {
        translationRepository?.let { repo ->
            coroutineScope.launch(Dispatchers.IO) {
                if (!repo.isModelDownloaded(targetLang.code)) {
                    repo.downloadModel(targetLang.code, requireWifi = false)
                }
            }
        }
    }

    // Sync OCR recognition script when source language changes
    LaunchedEffect(sourceLang.code) {
        val mappedScript = OcrScript.fromLanguageCode(sourceLang.code)
        ocrRepository.setScript(mappedScript)
    }

    // Photo Gallery Picker integration
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                ocrErrorMessage = null
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        cameraRepository.freezeFrame(bitmap)
                        when (val result = ocrRepository.processImageUri(context, uri)) {
                            is AppResult.Success -> {
                                capturedOcrResult = result.data
                                if (result.data.isEmpty) {
                                    ocrErrorMessage = "No recognizable text found in the selected image."
                                }
                            }
                            is AppResult.Error -> {
                                ocrErrorMessage = "Failed to process image text: ${result.error.message}"
                            }
                            is AppResult.Loading -> { /* no-op */ }
                        }
                    }
                } catch (e: Exception) {
                    ocrErrorMessage = "Error opening image: ${e.localizedMessage}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Tap-to-focus animation state
    val focusAnimScale = remember { Animatable(1.5f) }
    val focusAnimAlpha = remember { Animatable(1f) }

    LaunchedEffect(tapFocusState.timestamp) {
        if (tapFocusState.isVisible) {
            focusAnimScale.snapTo(1.5f)
            focusAnimAlpha.snapTo(1f)
            focusAnimScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            delay(1200)
            focusAnimAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
            tapFocusState = tapFocusState.copy(isVisible = false)
        }
    }

    // Scanning line animation in viewfinder
    val infiniteTransition = rememberInfiniteTransition(label = "scan_transition")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_offset"
    )

    var selectedModeTab by remember { mutableIntStateOf(0) }

    // Unbind camera & clean OCR when leaving screen or switching tabs
    DisposableEffect(lifecycleOwner, selectedModeTab) {
        onDispose {
            cameraRepository.unbind()
            ocrRepository.clearLiveDetection()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("camera_screen")
    ) {
        // Mode Switcher: [Live Camera] vs [Photo & Doc Studio]
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedModeTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedModeTab == 0,
                    onClick = { selectedModeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Camera", fontWeight = if (selectedModeTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier
                        .smoothButtonPress(scaleDown = 0.96f)
                        .testTag("tab_live_camera")
                )
                Tab(
                    selected = selectedModeTab == 1,
                    onClick = { selectedModeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photo Studio", fontWeight = if (selectedModeTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier
                        .smoothButtonPress(scaleDown = 0.96f)
                        .testTag("tab_photo_studio")
                )
            }
        }

        if (selectedModeTab == 1 && imageTranslationRepository != null) {
            // Photo & Document Studio Embedded Screen
            ImageTranslationStudioScreen(
                imageTranslationRepository = imageTranslationRepository,
                languageRepository = languageRepository,
                ttsRepository = ttsRepository,
                favoritesRepository = favoritesRepository,
                historyRepository = historyRepository,
                modifier = Modifier.weight(1f)
            )
        } else {
            if (!hasCameraPermission) {
                PermissionRequiredView(
                    permissionName = "Camera",
                    rationale = "Vision Translator requires camera access to recognize signs, menus, and text in real time using on-device optical character recognition.",
                    onRequestPermission = {
                        permissionLauncher.launch(PermissionManager.CAMERA_PERMISSION)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Language Selector Header
                    if (languages.isNotEmpty()) {
                        LanguageSelectorBar(
                            sourceLanguage = sourceLang,
                            targetLanguage = targetLang,
                            availableLanguages = languages,
                            languageRepository = languageRepository,
                            translationRepository = translationRepository,
                            onSourceLanguageChanged = { sourceLang = it },
                            onTargetLanguageChanged = { targetLang = it },
                            onSwapLanguages = {
                                val (newSource, newTarget) = languageRepository.swapLanguages(sourceLang, targetLang)
                                sourceLang = newSource
                                targetLang = newTarget
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Overlay & Script Mode Chips Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Overlay Display Mode Selector (Translated, Original, Split, Off)
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = overlayMode == OverlayMode.TRANSLATED,
                                    onClick = { overlayMode = OverlayMode.TRANSLATED },
                                    leadingIcon = {
                                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    label = { Text("Translate Overlay", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .smoothButtonPress(scaleDown = 0.94f)
                                        .testTag("mode_chip_translated")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = overlayMode == OverlayMode.ORIGINAL,
                                    onClick = { overlayMode = OverlayMode.ORIGINAL },
                                    label = { Text("Original OCR", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier
                                        .smoothButtonPress(scaleDown = 0.94f)
                                        .testTag("mode_chip_original")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = overlayMode == OverlayMode.SIDE_BY_SIDE,
                                    onClick = { overlayMode = OverlayMode.SIDE_BY_SIDE },
                                    label = { Text("Split View", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier
                                        .smoothButtonPress(scaleDown = 0.94f)
                                        .testTag("mode_chip_split")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = overlayMode == OverlayMode.OFF,
                                    onClick = { overlayMode = OverlayMode.OFF },
                                    label = { Text("Overlay Off", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier
                                        .smoothButtonPress(scaleDown = 0.94f)
                                        .testTag("mode_chip_off")
                                )
                            }
                        }
                    }

        Spacer(modifier = Modifier.height(6.dp))

        // Camera Viewfinder Box & Visual Translation Overlay Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0B0F19))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .pointerInput(previewViewRef) {
                    detectTapGestures { offset ->
                        previewViewRef?.let { pView ->
                            cameraRepository.focusOnPoint(offset.x, offset.y, pView)
                            tapFocusState = TapFocusState(
                                x = offset.x,
                                y = offset.y,
                                isVisible = true,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (zoomChange != 1.0f) {
                            val currentZoom = cameraState.zoomRatio
                            val newZoom = (currentZoom * zoomChange).coerceIn(1.0f, 5.0f)
                            cameraRepository.setZoomRatio(newZoom)
                        }
                    }
                }
                .testTag("camera_viewfinder_container"),
            contentAlignment = Alignment.Center
        ) {
            // 1. Live PreviewView or Frozen Image with visual zoom transformation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = animatedZoomScale
                        scaleY = animatedZoomScale
                    }
            ) {
                if (cameraState.isFrozen && cameraState.frozenBitmap != null) {
                    Image(
                        bitmap = cameraState.frozenBitmap!!.asImageBitmap(),
                        contentDescription = "Frozen Camera Frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camera_frozen_image")
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                previewViewRef = this
                                cameraRepository.bindCamera(lifecycleOwner, this)
                            }
                        },
                        update = { view ->
                            previewViewRef = view
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camera_preview_view")
                    )
                }

                // 2. High-Performance Visual Translation Overlay aligned with viewport zoom
                if ((cameraState.autoOcrEnabled || cameraState.isFrozen) && overlayMode != OverlayMode.OFF) {
                    VisualTranslationOverlay(
                        blocks = translationState.blocks,
                        sourceWidth = if (translationState.sourceWidth > 0) translationState.sourceWidth else displayOcrResult.sourceWidth,
                        sourceHeight = if (translationState.sourceHeight > 0) translationState.sourceHeight else displayOcrResult.sourceHeight,
                        overlayMode = overlayMode,
                        onBlockClicked = { block ->
                            selectedBlock = block
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Flashlight / Night Torch illumination effect overlay
            if (cameraState.isTorchOn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.20f))
                )
            }

            // 3. Viewfinder Reticle (when live with no detected text)
            if (!cameraState.isFrozen && displayOcrResult.isEmpty) {
                Box(
                    modifier = Modifier
                        .size(280.dp, 190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (scanLineOffset * 190.dp.toPx()).roundToInt()
                                )
                            }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.secondary,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // 4. Tap-to-Focus Indicator Ring
            if (tapFocusState.isVisible) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (tapFocusState.x - 30.dp.toPx()).roundToInt(),
                                y = (tapFocusState.y - 30.dp.toPx()).roundToInt()
                            )
                        }
                        .size((60 * focusAnimScale.value).dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = focusAnimAlpha.value),
                            CircleShape
                        )
                )
            }

            // 5. Top Viewfinder Quick Controls (Flash, Switch, Zoom & OCR Metrics)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Translation & OCR Stats Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format("%.1fx", cameraState.zoomRatio),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (translationState.blocks.isNotEmpty()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${translationState.blocks.size} translated",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (translationState.isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flash / Torch Button
                    IconButton(
                        onClick = { cameraRepository.toggleTorch() },
                        enabled = !cameraState.isFrozen,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .smoothButtonPress(scaleDown = 0.88f)
                            .background(
                                if (cameraState.isTorchOn) MaterialTheme.colorScheme.primary
                                else Color.Black.copy(alpha = 0.65f)
                            )
                            .testTag("camera_flash_button")
                    ) {
                        Icon(
                            imageVector = if (cameraState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = if (cameraState.isTorchOn) Color.Yellow else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Camera Switch Button (Rear / Front)
                    IconButton(
                        onClick = { cameraRepository.switchCamera() },
                        enabled = !cameraState.isFrozen,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .smoothButtonPress(scaleDown = 0.88f)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .testTag("camera_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera Lens",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 6. Frozen / Processing Banner
            if (cameraState.isFrozen || isProcessing) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 58.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isProcessing) "Translating frame..." else "Frame Frozen • Tap box to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 7. Quick Zoom Presets
            if (!cameraState.isFrozen) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1.0f, 2.0f, 4.0f).forEach { zoomTarget ->
                        val isSelected = (cameraState.zoomRatio - zoomTarget).let { it >= -0.2f && it <= 0.2f }
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .smoothButtonPress(scaleDown = 0.88f)
                                .testTag("zoom_btn_${zoomTarget.toInt()}x")
                        ) {
                            IconButton(
                                onClick = { cameraRepository.setZoomRatio(zoomTarget) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Text(
                                    text = "${zoomTarget.toInt()}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 8. Warning / Error Overlay
            if (cameraState.errorMessage != null || ocrErrorMessage != null) {
                val errorText = cameraState.errorMessage ?: ocrErrorMessage
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorText ?: "Operation encountered an issue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    ocrErrorMessage = null
                                    if (cameraState.isFrozen) cameraRepository.resumeLiveCamera()
                                    previewViewRef?.let { cameraRepository.bindCamera(lifecycleOwner, it) }
                                },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.94f)
                            ) {
                                Text("Dismiss / Retry")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Action Controls (Freeze/Resume, Capture-and-Translate, Gallery Import, Live OCR Toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Import Action
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .smoothButtonPress(scaleDown = 0.90f)
                    .testTag("camera_gallery_button")
            ) {
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Import from Gallery for OCR",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Freeze / Resume Live Stream
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (cameraState.isFrozen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .smoothButtonPress(scaleDown = 0.90f)
                    .testTag("camera_freeze_toggle")
            ) {
                IconButton(
                    onClick = {
                        if (cameraState.isFrozen) {
                            capturedOcrResult = null
                            ocrErrorMessage = null
                            cameraRepository.resumeLiveCamera()
                        } else {
                            coroutineScope.launch {
                                isProcessing = true
                                when (val result = cameraRepository.captureStillImage()) {
                                    is AppResult.Success -> {
                                        cameraRepository.freezeFrame(result.data)
                                        when (val ocrResult = ocrRepository.processBitmap(result.data)) {
                                            is AppResult.Success -> capturedOcrResult = ocrResult.data
                                            is AppResult.Error -> ocrErrorMessage = "OCR processing failed"
                                            is AppResult.Loading -> {}
                                        }
                                    }
                                    is AppResult.Error -> cameraRepository.freezeFrame(null)
                                    is AppResult.Loading -> {}
                                }
                                isProcessing = false
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (cameraState.isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (cameraState.isFrozen) "Resume Live Camera" else "Freeze Frame",
                        tint = if (cameraState.isFrozen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Capture & High-Res Translation Shutter Button
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        isProcessing = true
                        ocrErrorMessage = null
                        when (val captureResult = cameraRepository.captureStillImage()) {
                            is AppResult.Success -> {
                                cameraRepository.freezeFrame(captureResult.data)
                                when (val ocrResult = ocrRepository.processBitmap(captureResult.data)) {
                                    is AppResult.Success -> {
                                        capturedOcrResult = ocrResult.data
                                        if (ocrResult.data.isEmpty) {
                                            ocrErrorMessage = "No text detected in captured image"
                                        }
                                    }
                                    is AppResult.Error -> {
                                        ocrErrorMessage = "Text recognition error: ${ocrResult.error.message}"
                                    }
                                    is AppResult.Loading -> {}
                                }
                            }
                            is AppResult.Error -> {
                                ocrErrorMessage = "Capture failed: ${captureResult.error.message}"
                            }
                            is AppResult.Loading -> {}
                        }
                        isProcessing = false
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .smoothButtonPress(scaleDown = 0.90f)
                    .testTag("camera_capture_button")
            ) {
                if (isProcessing || cameraState.isCapturing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Capture and Translate",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Auto-OCR Live Recognition Toggle
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (cameraState.autoOcrEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .smoothButtonPress(scaleDown = 0.90f)
                    .testTag("camera_auto_ocr_toggle")
            ) {
                IconButton(
                    onClick = {
                        val nextState = !cameraState.autoOcrEnabled
                        cameraRepository.setAutoOcr(nextState)
                        if (!nextState) {
                            ocrRepository.clearLiveDetection()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (cameraState.autoOcrEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Live Recognition",
                        tint = if (cameraState.autoOcrEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
                }
            }
        }
    }

    // Modal Bottom Sheet for Inspecting Clicked Translated Text Block
    if (selectedBlock != null) {
        BlockInspectionBottomSheet(
            block = selectedBlock!!,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang,
            onDismiss = { selectedBlock = null },
            ttsRepository = ttsRepository,
            favoritesRepository = favoritesRepository,
            historyRepository = historyRepository
        )
    }
}
