package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.ocr.model.BoundingBox
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.model.TranslatedBlock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ZoomableImageTranslationCanvas(
    bitmap: Bitmap,
    translatedBlocks: List<TranslatedBlock>,
    overlayMode: OverlayMode,
    selectedBlock: TranslatedBlock?,
    onBlockClick: (TranslatedBlock) -> Unit,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onTransformChange: (scale: Float, panX: Float, panY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .testTag("zoomable_image_canvas")
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        if (imageWidth <= 0 || imageHeight <= 0 || containerWidthPx <= 0 || containerHeightPx <= 0) {
            return@BoxWithConstraints
        }

        // Compute FitCenter scale & offsets
        val scaleFit = min(containerWidthPx / imageWidth, containerHeightPx / imageHeight)
        val displayedImageWidth = imageWidth * scaleFit
        val displayedImageHeight = imageHeight * scaleFit

        val baseOffsetX = (containerWidthPx - displayedImageWidth) / 2f
        val baseOffsetY = (containerHeightPx - displayedImageHeight) / 2f

        // Gesture handling: Zoom (Pinch), Pan (Drag), Double tap to zoom 2x / reset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (zoomScale * zoom).coerceIn(1.0f, 6.0f)
                        val maxPanX = if (newScale > 1f) (containerWidthPx * (newScale - 1f)) / 2f else 0f
                        val maxPanY = if (newScale > 1f) (containerHeightPx * (newScale - 1f)) / 2f else 0f

                        val newPanX = (panOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                        val newPanY = (panOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)

                        onTransformChange(newScale, newPanX, newPanY)
                    }
                }
                .pointerInput(translatedBlocks, zoomScale, panOffsetX, panOffsetY) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (zoomScale > 1.2f) {
                                onTransformChange(1.0f, 0f, 0f)
                            } else {
                                onTransformChange(2.5f, 0f, 0f)
                            }
                        },
                        onTap = { tapOffset ->
                            // Transform tap coordinate to image-space coordinates
                            val centeredX = tapOffset.x - containerWidthPx / 2f
                            val centeredY = tapOffset.y - containerHeightPx / 2f

                            val unscaledX = (centeredX - panOffsetX) / zoomScale + containerWidthPx / 2f
                            val unscaledY = (centeredY - panOffsetY) / zoomScale + containerHeightPx / 2f

                            val imgX = (unscaledX - baseOffsetX) / scaleFit
                            val imgY = (unscaledY - baseOffsetY) / scaleFit

                            // Check if tapped inside any translated block bounding box
                            val tapped = translatedBlocks.findLast { block ->
                                val box = block.boundingBox
                                imgX >= box.left && imgX <= box.right &&
                                        imgY >= box.top && imgY <= box.bottom
                            }

                            if (tapped != null) {
                                onBlockClick(tapped)
                            }
                        }
                    )
                }
        ) {
            // Main Zoomed & Panned Content Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffsetX
                        translationY = panOffsetY
                    }
            ) {
                // Render the Bitmap
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Translated Image",
                    modifier = Modifier
                        .offset {
                            IntOffset(baseOffsetX.roundToInt(), baseOffsetY.roundToInt())
                        }
                        .width(with(density) { displayedImageWidth.toDp() })
                        .height(with(density) { displayedImageHeight.toDp() })
                )

                // Render Overlays according to OverlayMode
                if (overlayMode != OverlayMode.OFF) {
                    translatedBlocks.forEach { block ->
                        val box = block.boundingBox ?: return@forEach

                        val leftPx = baseOffsetX + (box.left * scaleFit)
                        val topPx = baseOffsetY + (box.top * scaleFit)
                        val widthPx = max(24f, box.width * scaleFit)
                        val heightPx = max(18f, box.height * scaleFit)

                        val isSelected = selectedBlock?.id == block.id

                        val leftDp = with(density) { leftPx.toDp() }
                        val topDp = with(density) { topPx.toDp() }
                        val widthDp = with(density) { widthPx.toDp() }
                        val heightDp = with(density) { heightPx.toDp() }

                        when (overlayMode) {
                            OverlayMode.TRANSLATED -> {
                                TranslatedPill(
                                    block = block,
                                    isSelected = isSelected,
                                    onClick = { onBlockClick(block) },
                                    modifier = Modifier
                                        .offset(x = leftDp, y = topDp)
                                        .width(widthDp)
                                        .height(heightDp)
                                )
                            }
                            OverlayMode.ORIGINAL -> {
                                OriginalOcrPill(
                                    block = block,
                                    isSelected = isSelected,
                                    onClick = { onBlockClick(block) },
                                    modifier = Modifier
                                        .offset(x = leftDp, y = topDp)
                                        .width(widthDp)
                                        .height(heightDp)
                                )
                            }
                            OverlayMode.SIDE_BY_SIDE -> {
                                SplitViewPill(
                                    block = block,
                                    isSelected = isSelected,
                                    onClick = { onBlockClick(block) },
                                    modifier = Modifier
                                        .offset(x = leftDp, y = topDp)
                                        .width(widthDp)
                                        .height(heightDp)
                                )
                            }
                            OverlayMode.OFF -> { /* no-op */ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslatedPill(
    block: TranslatedBlock,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayText = block.translatedText.ifBlank { block.originalText }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        Color.Black.copy(alpha = 0.82f)
    }
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("translated_block_${block.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                color = textColor,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun OriginalOcrPill(
    block: TranslatedBlock,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF00E676) else Color(0xFF00E676).copy(alpha = 0.6f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF00E676).copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("ocr_block_${block.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = block.originalText,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun SplitViewPill(
    block: TranslatedBlock,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.3f)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E1E24).copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("split_block_${block.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = block.originalText,
                color = Color.LightGray,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = block.translatedText.ifBlank { "..." },
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
