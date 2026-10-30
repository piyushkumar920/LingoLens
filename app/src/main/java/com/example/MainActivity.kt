package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.modules.settings.model.AppThemeMode
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.VisionTranslatorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as VisionTranslatorApp).container

        setContent {
            val settings by appContainer.settingsRepository.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            VisionTranslatorTheme(darkTheme = isDarkTheme) {
                AppNavigation(container = appContainer)
            }
        }
    }
}
