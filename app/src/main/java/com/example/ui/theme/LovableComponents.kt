package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.animation.smoothButtonPress

/**
 * Modifier for the signature Lovable dark button multi-layer inset shadow:
 * rgba(255,255,255,0.2) 0px 0.5px 0px 0px inset, rgba(0,0,0,0.2) 0px 0px 0px 0.5px inset
 */
fun Modifier.lovableButtonShadow(): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = Color.Black.copy(alpha = 0.2f)
        }
        canvas.drawRect(
            left = 0f,
            top = size.height - 1f,
            right = size.width,
            bottom = size.height,
            paint = paint
        )
    }
}

/**
 * Lovable Card primitive: Cream background (#f7f4ed), 1px solid #eceae4 border, 12px radius.
 */
@Composable
fun LovableCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    borderColor: Color = LovableLightCream,
    containerColor: Color = LovableCream,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}

/**
 * Lovable Primary Dark Button (#1c1c1c bg, #fcfbf8 text, 6px radius, inset shadow).
 */
@Composable
fun LovablePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .smoothButtonPress()
            .lovableButtonShadow(),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LovableCharcoal,
            contentColor = LovableOffWhite,
            disabledContainerColor = LovableCharcoal.copy(alpha = 0.5f),
            disabledContentColor = LovableOffWhite.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        content = content
    )
}

/**
 * Lovable Outline / Ghost Button (transparent bg, 1px solid interactive border, 6px radius).
 */
@Composable
fun LovableOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.smoothButtonPress(),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, LovableInteractiveBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = LovableCharcoal,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = LovableCharcoal.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        content = content
    )
}

/**
 * Lovable Pill Action Button (9999px radius).
 */
@Composable
fun LovablePillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LovableLightCream,
    contentColor: Color = LovableCharcoal,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(containerColor)
            .border(BorderStroke(1.dp, LovableLightCream), RoundedCornerShape(9999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}
