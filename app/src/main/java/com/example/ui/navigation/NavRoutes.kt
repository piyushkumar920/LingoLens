package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isBottomBarItem: Boolean = false
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        isBottomBarItem = true
    )

    data object Camera : Screen(
        route = "camera",
        title = "Camera",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        isBottomBarItem = true
    )

    data object Voice : Screen(
        route = "voice",
        title = "Voice",
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.Mic,
        isBottomBarItem = true
    )

    data object Text : Screen(
        route = "text",
        title = "Text",
        selectedIcon = Icons.Filled.Translate,
        unselectedIcon = Icons.Outlined.Translate,
        isBottomBarItem = true
    )

    data object Conversation : Screen(
        route = "conversation",
        title = "Dialogue",
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        isBottomBarItem = true
    )

    data object Travel : Screen(
        route = "travel",
        title = "Travel Guide",
        selectedIcon = Icons.Filled.Flight,
        unselectedIcon = Icons.Outlined.Flight,
        isBottomBarItem = false
    )

    data object History : Screen(
        route = "history",
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        isBottomBarItem = false
    )

    data object Favorites : Screen(
        route = "favorites",
        title = "Saved",
        selectedIcon = Icons.Filled.Star,
        unselectedIcon = Icons.Outlined.Star,
        isBottomBarItem = false
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        isBottomBarItem = false
    )

    data object OfflineLanguages : Screen(
        route = "offline_languages",
        title = "Offline Languages",
        selectedIcon = Icons.Filled.OfflinePin,
        unselectedIcon = Icons.Outlined.OfflinePin,
        isBottomBarItem = false
    )

    data object ImageStudio : Screen(
        route = "image_studio",
        title = "Photo Translate",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        isBottomBarItem = false
    )

    data object Diagnostics : Screen(
        route = "diagnostics",
        title = "Translation Test Lab",
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        isBottomBarItem = false
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Camera, Voice, Text, Conversation)

        val allScreens: List<Screen>
            get() = listOf(Home, Camera, Voice, Text, Conversation, Travel, History, Favorites, Settings, OfflineLanguages, ImageStudio, Diagnostics)
    }
}
