package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Clean, high-performance visual translation overlay.
 * Completely independent of CameraX, operates strictly on spatial coordinates and layout constraints.
 */
@Composable
fun VisualTranslationOverlay(
    blocks: List<TranslatedBlock>,
    sourceWidth: Int,
    sourceHeight: Int,
    overlayMode: OverlayMode,
    onBlockClicked: (TranslatedBlock) -> Unit,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    translatedBadgeBg: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    translatedTextColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (overlayMode == OverlayMode.OFF || blocks.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val srcW = if (sourceWidth > 0) sourceWidth.toFloat() else canvasWidth
        val srcH = if (sourceHeight > 0) sourceHeight.toFloat() else canvasHeight

        val transform = remember(canvasWidth, canvasHeight, srcW, srcH) {
            val scale = max(canvasWidth / srcW, canvasHeight / srcH)
            val scaledW = srcW * scale
            val scaledH = srcH * scale
            val dx = (canvasWidth - scaledW) / 2f
            val dy = (canvasHeight - scaledH) / 2f
            TransformMatrix(scale, dx, dy)
        }

        // Tap gestures detector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(blocks, transform) {
                    detectTapGestures { tapOffset ->
                        val clicked = blocks.firstOrNull { block ->
                            val mapped = transform.mapBox(block.boundingBox)
                            // Expand hit box slightly for easier tapping on mobile
                            val hitLeft = mapped.left - 12f
                            val hitTop = mapped.top - 12f
                            val hitRight = mapped.right + 12f
                            val hitBottom = mapped.bottom + 12f
                            tapOffset.x in hitLeft..hitRight && tapOffset.y in hitTop..hitBottom
                        }
                        if (clicked != null) {
                            onBlockClicked(clicked)
                        }
                    }
                }
        ) {
            when (overlayMode) {
                OverlayMode.ORIGINAL -> {
                    // Render raw OCR bounding boxes and labels
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        blocks.forEach { block ->
                            val mapped = transform.mapBox(block.boundingBox)
                            drawRoundRect(
                                color = highlightColor.copy(alpha = 0.22f),
                                topLeft = Offset(mapped.left, mapped.top),
                                size = Size(mapped.width.coerceAtLeast(16f), mapped.height.coerceAtLeast(16f)),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                color = highlightColor.copy(alpha = 0.85f),
                                topLeft = Offset(mapped.left, mapped.top),
                                size = Size(mapped.width.coerceAtLeast(16f), mapped.height.coerceAtLeast(16f)),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Original text floating pills
                    blocks.forEach { block ->
                        val mapped = transform.mapBox(block.boundingBox)
                        val clampedX = mapped.left.coerceIn(8f, (canvasWidth - 120f).coerceAtLeast(8f))
                        val clampedY = (mapped.top - 24.dp.value * density.density).coerceIn(8f, (canvasHeight - 40f).coerceAtLeast(8f))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.80f),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                                .clickable { onBlockClicked(block) }
                                .testTag("original_ocr_chip_${block.id}")
                        ) {
                            Text(
                                text = block.originalText.trim().replace("\n", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                OverlayMode.TRANSLATED -> {
                    // Render Translated Text directly over the detected location
                    blocks.forEach { block ->
                        val mapped = transform.mapBox(block.boundingBox)
                        val boxWidthDp = with(density) { mapped.width.toDp() }.coerceAtLeast(56.dp)
                        val boxHeightDp = with(density) { mapped.height.toDp() }.coerceAtLeast(26.dp)

                        // Clamp offset so badge is always fully visible inside viewport
                        val clampedX = mapped.left.coerceIn(8f, (canvasWidth - mapped.width).coerceAtLeast(8f))
                        val clampedY = mapped.top.coerceIn(8f, (canvasHeight - mapped.height).coerceAtLeast(8f))

                        val dynamicFontSize = computeAdaptiveFontSize(
                            textLength = block.displayText.length,
                            boxWidthPx = mapped.width,
                            boxHeightPx = mapped.height
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = if (block.isTranslating) highlightColor.copy(alpha = 0.5f) else highlightColor
                            ),
                            modifier = Modifier
                                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                                .widthIn(min = boxWidthDp, max = (canvasWidth * 0.9f).dp)
                                .heightIn(min = boxHeightDp)
                                .rotate(block.angle)
                                .clickable { onBlockClicked(block) }
                                .testTag("translated_block_chip_${block.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (block.isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = highlightColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                Text(
                                    text = block.displayText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = dynamicFontSize.sp,
                                        lineHeight = (dynamicFontSize * 1.25f).sp
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OverlayMode.SIDE_BY_SIDE -> {
                    // Render dual chips (Original + Translation)
                    blocks.forEach { block ->
                        val mapped = transform.mapBox(block.boundingBox)
                        val clampedX = mapped.left.coerceIn(8f, (canvasWidth - 220f).coerceAtLeast(8f))
                        val clampedY = mapped.top.coerceIn(8f, (canvasHeight - 60f).coerceAtLeast(8f))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            tonalElevation = 6.dp,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, highlightColor),
                            modifier = Modifier
                                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                                .widthIn(min = 100.dp, max = (canvasWidth * 0.92f).dp)
                                .clickable { onBlockClicked(block) }
                                .testTag("side_by_side_chip_${block.id}")
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = block.originalText.trim(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = block.displayText.trim(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OverlayMode.OFF -> {}
            }
        }
    }
}

/**
 * Calculates readable font size based on bounding box dimensions and character count.
 */
private fun computeAdaptiveFontSize(textLength: Int, boxWidthPx: Float, boxHeightPx: Float): Float {
    return when {
        boxHeightPx < 40f || textLength > 80 -> 10f
        boxHeightPx < 70f || textLength > 40 -> 12f
        boxHeightPx < 120f || textLength > 20 -> 14f
        else -> 16f
    }
}

// Helper extension for surface elevation color
@Composable
private fun androidx.compose.material3.ColorScheme.surfaceColorAtElevation(elevation: androidx.compose.ui.unit.Dp): Color {
    return this.surface
}
