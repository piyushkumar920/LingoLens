package com.example.ui.animation

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Checks whether system reduced motion is enabled for accessibility.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Screen transition animations adhering to calm, modern, and responsive guidelines.
 */
object MotionTransitions {
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
    }

    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
    }

    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
    }

    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
    }
}

/**
 * Universal smooth tactile spring-press animation for buttons and clickable components.
 * Scale subtly compresses when pressed down and springs back smoothly on release,
 * without interrupting the underlying click or touch events.
 */
fun Modifier.smoothButtonPress(
    scaleDown: Float = 0.92f,
    hapticType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "smooth_button_press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isPressed = true
                    try {
                        hapticType?.let { haptic.performHapticFeedback(it) }
                    } catch (_: Exception) {}

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })

                    isPressed = false
                }
            }
        }
}

/**
 * Reusable Tactile Micro-Interaction modifier providing subtle scale feedback
 * and gentle haptic confirmation on touch.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    hapticType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .scale(scale.value)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    hapticType?.let { haptic.performHapticFeedback(it) }
                    scope.launch {
                        scale.animateTo(
                            targetValue = scaleDown,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    val up = waitForUpOrCancellation()
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    if (up != null) {
                        onClick()
                    }
                }
            }
        }
}

/**
 * Lightweight shimmer placeholder modifier for modern skeleton loading states.
 */
fun Modifier.shimmerPlaceholder(
    visible: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    highlightColor: Color? = null,
    baseColor: Color? = null
): Modifier = composed {
    if (!visible) return@composed this

    val base = baseColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val highlight = highlightColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    this
        .clip(shape)
        .background(brush)
}

/**
 * Reusable high-fidelity Skeleton Loading card for translation and AI explain states.
 */
@Composable
fun TranslationSkeletonCard(
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 20.dp)
                        .shimmerPlaceholder()
                )
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 20.dp)
                        .shimmerPlaceholder(shape = RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulated text lines with staggered widths
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(18.dp)
                    .shimmerPlaceholder()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(18.dp)
                    .shimmerPlaceholder()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(18.dp)
                    .shimmerPlaceholder()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .shimmerPlaceholder(shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .shimmerPlaceholder(shape = CircleShape)
                )
            }
        }
    }
}

/**
 * Organic, calm audio waveform and ripple visualizer for speech and conversation listening states.
 * Replaces jarring raw audio bars with fluid, floating acoustic waves.
 */
@Composable
fun OrganicListeningVisualizer(
    isListening: Boolean,
    soundLevelDb: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "organic_wave_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val normalizedLevel = (soundLevelDb / 10f).coerceIn(0.15f, 1.2f)
    val animatedLevel by animateFloatAsState(
        targetValue = if (isListening) normalizedLevel else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animated_audio_level"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            if (!isListening) {
                // Calm resting baseline dots
                val dotCount = 5
                val spacing = 16.dp.toPx()
                val startX = (width - (dotCount - 1) * spacing) / 2f
                for (i in 0 until dotCount) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(startX + i * spacing, centerY)
                    )
                }
                return@Canvas
            }

            // Draw multi-layered organic fluid soundwaves
            val barCount = 19
            val barWidth = 4.dp.toPx()
            val totalBarsWidth = barCount * 8.dp.toPx()
            val startX = (width - totalBarsWidth) / 2f

            for (i in 0 until barCount) {
                val progress = i.toFloat() / (barCount - 1)
                val angle = wavePhase + (progress * PI * 2f).toFloat()

                // Harmonious envelope curve (higher in middle, lower at flanks)
                val envelope = sin(progress * PI).toFloat()
                val waveAmplitude = (sin(angle) * 0.45f + 0.55f) * envelope * animatedLevel
                val barH = (waveAmplitude * height * 0.85f).coerceAtLeast(4.dp.toPx())

                val x = startX + i * 8.dp.toPx()
                val yTop = centerY - barH / 2f

                // Color gradient across the soundwave from primary to tertiary accent
                val barColor = if (i % 2 == 0) primaryColor else accentColor

                drawRoundRect(
                    color = barColor.copy(alpha = 0.85f),
                    topLeft = Offset(x, yTop),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

/**
 * Luminous, multi-layered Animated Orb for voice hearing / speech recognition state.
 * Reacts dynamically to live sound levels, pulses rhythmically, emanates energy aura rings,
 * rotates chromatic plasma gradient, and renders floating energy particles.
 */
@Composable
fun AnimatedListeningOrb(
    isListening: Boolean,
    soundLevelDb: Float,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    coreColorStart: Color = Color(0xFF38BDF8), // Cyan / Sky
    coreColorMid: Color = Color(0xFF6366F1),   // Indigo
    coreColorEnd: Color = Color(0xFFA855F7),   // Purple
    accentGlow: Color = Color(0xFFEC4899),     // Pink / Magenta
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "listening_orb_infinite")

    // Slow ambient rotation of the plasma chromatic spectrum
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    // Breathing pulse cycle
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 800 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breathing"
    )

    // Fluid wave undulation phase for the orb rim
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1200 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_wave_phase"
    )

    // Expanding acoustic ripple rings
    val ringPhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_ring_1"
    )

    val ringPhase2 = (ringPhase1 + 0.33f) % 1f
    val ringPhase3 = (ringPhase1 + 0.66f) % 1f

    // Dynamic audio level with fast spring reaction
    val normalizedSoundLevel = (soundLevelDb / 10f).coerceIn(0f, 1.5f)
    val animatedSoundLevel by animateFloatAsState(
        targetValue = if (isListening) normalizedSoundLevel else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "orb_sound_reactivity"
    )

    val clickableModifier = if (onClick != null) {
        Modifier
            .smoothButtonPress(scaleDown = 0.92f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.44f

            // 1. Expanding Aura Ripple Rings (Active when listening)
            if (isListening) {
                val ringPhases = listOf(ringPhase1, ringPhase2, ringPhase3)
                ringPhases.forEachIndexed { index, progress ->
                    val ringRadius = baseRadius * (1f + progress * 1.15f * (1f + animatedSoundLevel * 0.25f))
                    val ringAlpha = ((1f - progress).pow(1.5f) * 0.55f).coerceIn(0f, 1f)
                    val ringColor = when (index % 3) {
                        0 -> coreColorStart
                        1 -> coreColorMid
                        else -> accentGlow
                    }

                    drawCircle(
                        color = ringColor.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (2.5f - progress * 1.5f).dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 2. Luminous Ambient Glow / Halos behind the Orb
            val glowMultiplier = if (isListening) 1.5f + animatedSoundLevel * 0.35f else 1.25f
            val glowRadius = baseRadius * glowMultiplier
            val glowAlpha = if (isListening) 0.45f + breathingPulse * 0.2f else 0.2f + breathingPulse * 0.1f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColorStart.copy(alpha = glowAlpha),
                        coreColorMid.copy(alpha = glowAlpha * 0.6f),
                        accentGlow.copy(alpha = glowAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 3. Fluid Organic Pulsing Plasma Core
            val dynamicCoreRadius = baseRadius * (1f + if (isListening) {
                animatedSoundLevel * 0.25f + breathingPulse * 0.08f
            } else {
                breathingPulse * 0.04f
            })

            rotate(degrees = rotationAngle, pivot = center) {
                // Harmonic undulating perimeter when listening
                if (isListening) {
                    val path = Path()
                    val segments = 48
                    val undulationAmp = (3.dp.toPx() + animatedSoundLevel * 4.dp.toPx())

                    for (i in 0 until segments) {
                        val theta = (i.toFloat() / segments) * 2f * PI.toFloat()
                        val r = dynamicCoreRadius + sin(theta * 4f + wavePhase).toFloat() * undulationAmp
                        val px = center.x + r * cos(theta)
                        val py = center.y + r * sin(theta)

                        if (i == 0) {
                            path.moveTo(px, py)
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                    path.close()

                    drawPath(
                        path = path,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                coreColorStart,
                                coreColorMid,
                                accentGlow,
                                coreColorEnd,
                                coreColorStart
                            ),
                            center = center
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                coreColorStart,
                                coreColorMid,
                                accentGlow,
                                coreColorEnd,
                                coreColorStart
                            ),
                            center = center
                        ),
                        radius = dynamicCoreRadius,
                        center = center
                    )
                }

                // Inner radial illumination gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isListening) 0.85f else 0.5f),
                            coreColorStart.copy(alpha = 0.7f),
                            coreColorMid.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        center = Offset(center.x - dynamicCoreRadius * 0.25f, center.y - dynamicCoreRadius * 0.25f),
                        radius = dynamicCoreRadius * 0.9f
                    ),
                    radius = dynamicCoreRadius,
                    center = center
                )
            }

            // 4. Optical Lens Reflection Arc / Core Glint
            val glintRadius = dynamicCoreRadius * 0.28f
            val glintCenter = Offset(center.x - dynamicCoreRadius * 0.32f, center.y - dynamicCoreRadius * 0.32f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    center = glintCenter,
                    radius = glintRadius
                ),
                radius = glintRadius,
                center = glintCenter
            )

            // 5. Celestial Spark Particles (Orbiting Sparks)
            val sparkCount = if (isListening) 6 else 3
            for (s in 0 until sparkCount) {
                val sparkAngle = rotationAngle * (if (s % 2 == 0) 1.2f else -0.8f) * (PI.toFloat() / 180f) + (s * (2 * PI.toFloat() / sparkCount))
                val sparkDist = dynamicCoreRadius * (1.18f + (s % 3) * 0.15f + animatedSoundLevel * 0.15f)
                val sparkX = center.x + sparkDist * cos(sparkAngle)
                val sparkY = center.y + sparkDist * sin(sparkAngle)
                val sparkAlpha = (0.4f + 0.5f * sin(wavePhase + s)).coerceIn(0.2f, 0.95f)

                drawCircle(
                    color = if (s % 2 == 0) coreColorStart.copy(alpha = sparkAlpha) else accentGlow.copy(alpha = sparkAlpha),
                    radius = (2f + (s % 2) * 1.5f).dp.toPx(),
                    center = Offset(sparkX, sparkY)
                )
            }
        }
    }
}
