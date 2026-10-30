package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LovableLightColorScheme = lightColorScheme(
    primary = LovableCharcoal,
    onPrimary = LovableOffWhite,
    primaryContainer = LovableLightCream,
    onPrimaryContainer = LovableCharcoal,
    secondary = LovableCharcoal,
    onSecondary = LovableOffWhite,
    secondaryContainer = LovableLightCream,
    onSecondaryContainer = LovableCharcoal,
    tertiary = LovableMutedGray,
    onTertiary = LovableOffWhite,
    background = LovableCream,
    onBackground = LovableCharcoal,
    surface = LovableCream,
    onSurface = LovableCharcoal,
    surfaceVariant = LovableLightCream,
    onSurfaceVariant = LovableMutedGray,
    outline = LovableLightCream,
    outlineVariant = LovableInteractiveBorder,
    error = ErrorRed
)

private val LovableDarkColorScheme = darkColorScheme(
    primary = LovableOffWhite,
    onPrimary = LovableCharcoal,
    primaryContainer = LovableDarkSurface,
    onPrimaryContainer = LovableOffWhite,
    secondary = LovableOffWhite,
    onSecondary = LovableCharcoal,
    secondaryContainer = LovableDarkSurface,
    onSecondaryContainer = LovableOffWhite,
    tertiary = LovableMutedGray,
    onTertiary = LovableOffWhite,
    background = LovableDarkBackground,
    onBackground = LovableOffWhite,
    surface = LovableDarkSurface,
    onSurface = LovableOffWhite,
    surfaceVariant = LovableDarkOutline,
    onSurfaceVariant = LovableMutedGray,
    outline = LovableDarkOutline,
    outlineVariant = LovableInteractiveBorder,
    error = ErrorRed
)

@Composable
fun VisionTranslatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LovableDarkColorScheme
        else -> LovableLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    VisionTranslatorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
