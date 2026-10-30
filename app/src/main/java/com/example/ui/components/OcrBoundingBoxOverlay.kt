package com.example.ui.components

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.ocr.model.BoundingBox
import com.example.modules.ocr.model.RecognizedBlock
import com.example.modules.ocr.model.TextDetectionResult
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Coordinate transform mapping source image coordinates to target canvas coordinates (Center Crop).
 */
data class TransformMatrix(
    val scale: Float,
    val dx: Float,
    val dy: Float
) {
    fun mapBox(box: BoundingBox): BoundingBox {
        return BoundingBox(
            left = box.left * scale + dx,
            top = box.top * scale + dy,
            right = box.right * scale + dx,
            bottom = box.bottom * scale + dy
        )
    }

    fun mapPoint(x: Float, y: Float): Offset {
        return Offset(x * scale + dx, y * scale + dy)
    }

    fun contains(mappedBox: BoundingBox, px: Float, py: Float): Boolean {
        return px in mappedBox.left..mappedBox.right && py in mappedBox.top..mappedBox.bottom
    }
}

/**
 * Interactive OCR bounding box overlay rendered over the Camera preview or frozen image.
 */
@Composable
fun OcrBoundingBoxOverlay(
    detectionResult: TextDetectionResult,
    onBlockClicked: ((RecognizedBlock) -> Unit)? = null,
    modifier: Modifier = Modifier,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (detectionResult.isEmpty || detectionResult.blocks.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        val sourceWidth = if (detectionResult.sourceWidth > 0) detectionResult.sourceWidth.toFloat() else canvasWidth
        val sourceHeight = if (detectionResult.sourceHeight > 0) detectionResult.sourceHeight.toFloat() else canvasHeight

        val transform = remember(canvasWidth, canvasHeight, sourceWidth, sourceHeight) {
            val scale = max(canvasWidth / sourceWidth, canvasHeight / sourceHeight)
            val scaledWidth = sourceWidth * scale
            val scaledHeight = sourceHeight * scale
            val dx = (canvasWidth - scaledWidth) / 2f
            val dy = (canvasHeight - scaledHeight) / 2f
            TransformMatrix(scale, dx, dy)
        }

        // Tap detector on mapped text blocks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(detectionResult, transform) {
                    detectTapGestures { tapOffset ->
                        val clickedBlock = detectionResult.blocks.firstOrNull { block ->
                            block.boundingBox?.let { box ->
                                val mapped = transform.mapBox(box)
                                transform.contains(mapped, tapOffset.x, tapOffset.y)
                            } ?: false
                        }
                        if (clickedBlock != null) {
                            onBlockClicked?.invoke(clickedBlock)
                        }
                    }
                }
        ) {
            // Draw visual bounding highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                detectionResult.blocks.forEach { block ->
                    block.boundingBox?.let { box ->
                        val mapped = transform.mapBox(box)
                        val boxWidth = mapped.width
                        val boxHeight = mapped.height

                        // Semi-transparent highlight backdrop
                        drawRoundRect(
                            color = highlightColor.copy(alpha = 0.20f),
                            topLeft = Offset(mapped.left, mapped.top),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Distinct bounding border
                        drawRoundRect(
                            color = highlightColor.copy(alpha = 0.85f),
                            topLeft = Offset(mapped.left, mapped.top),
                            size = Size(boxWidth, boxHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Subtle corner accents
                        drawCornerAccents(mapped, accentColor)
                    }
                }
            }

            // Draw floating label chips for prominent blocks
            detectionResult.blocks.take(8).forEach { block ->
                block.boundingBox?.let { box ->
                    val mapped = transform.mapBox(box)
                    if (mapped.left >= 0 && mapped.top >= 0 && mapped.width > 20) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.80f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = mapped.left.coerceAtLeast(8f).roundToInt(),
                                        y = (mapped.top - 24.dp.toPx()).coerceAtLeast(8f).roundToInt()
                                    )
                                }
                                .clickable { onBlockClicked?.invoke(block) }
                        ) {
                            Text(
                                text = block.text.trim().replace("\n", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCornerAccents(box: BoundingBox, color: Color) {
    val cornerLen = 8.dp.toPx()
    val strokeW = 3.dp.toPx()

    // Top-Left
    drawLine(color, Offset(box.left, box.top), Offset(box.left + cornerLen, box.top), strokeWidth = strokeW)
    drawLine(color, Offset(box.left, box.top), Offset(box.left, box.top + cornerLen), strokeWidth = strokeW)

    // Top-Right
    drawLine(color, Offset(box.right, box.top), Offset(box.right - cornerLen, box.top), strokeWidth = strokeW)
    drawLine(color, Offset(box.right, box.top), Offset(box.right, box.top + cornerLen), strokeWidth = strokeW)

    // Bottom-Left
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left + cornerLen, box.bottom), strokeWidth = strokeW)
    drawLine(color, Offset(box.left, box.bottom), Offset(box.left, box.bottom - cornerLen), strokeWidth = strokeW)

    // Bottom-Right
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right - cornerLen, box.bottom), strokeWidth = strokeW)
    drawLine(color, Offset(box.right, box.bottom), Offset(box.right - cornerLen, box.bottom), strokeWidth = strokeW)
}
