package com.example.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.animation.MotionTransitions
import com.example.ui.animation.isReducedMotionEnabled
import com.example.ui.animation.smoothButtonPress
import com.example.core.di.AppContainer
import com.example.core.network.NetworkConnectionState
import com.example.ui.components.NetworkStatusBar
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageTranslationStudioScreen
import com.example.ui.screens.OfflineLanguagesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TextScreen
import com.example.ui.screens.TravelScreen
import com.example.ui.screens.VoiceScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val currentScreen = Screen.allScreens.find { it.route == currentRoute } ?: Screen.Home
    val isRootScreen = Screen.bottomNavItems.any { it.route == currentRoute }
    val connectionState by container.connectivityMonitor.connectionState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (!isRootScreen) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        if (isRootScreen) {
                            IconButton(
                                onClick = { navController.navigate(Screen.OfflineLanguages.route) },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflinePin,
                                    contentDescription = "Offline Languages",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Screen.Travel.route) },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flight,
                                    contentDescription = "Travel Phrases",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Screen.Favorites.route) },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Saved Favorites",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Screen.History.route) },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Screen.Settings.route) },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Screen.Diagnostics.route) },
                                modifier = Modifier.smoothButtonPress(scaleDown = 0.88f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Translation Test Lab",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Network Status Indicator for Offline / Limited connectivity
                NetworkStatusBar(
                    connectionState = connectionState,
                    onManageOfflineLanguagesClick = {
                        if (currentRoute != Screen.OfflineLanguages.route) {
                            navController.navigate(Screen.OfflineLanguages.route)
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier
                            .smoothButtonPress(scaleDown = 0.92f)
                            .testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = MotionTransitions.enterTransition,
            exitTransition = MotionTransitions.exitTransition,
            popEnterTransition = MotionTransitions.popEnterTransition,
            popExitTransition = MotionTransitions.popExitTransition,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateTo = { route -> navController.navigate(route) },
                    languageRepository = container.languageRepository,
                    translationRepository = container.translationRepository,
                    speechRepository = container.speechRepository
                )
            }
            composable(Screen.Camera.route) {
                CameraScreen(
                    cameraRepository = container.cameraRepository,
                    ocrRepository = container.ocrRepository,
                    translationRepository = container.translationRepository,
                    languageRepository = container.languageRepository,
                    historyRepository = container.historyRepository,
                    favoritesRepository = container.favoritesRepository,
                    ttsRepository = container.ttsRepository,
                    imageTranslationRepository = container.imageTranslationRepository
                )
            }
            composable(Screen.ImageStudio.route) {
                ImageTranslationStudioScreen(
                    imageTranslationRepository = container.imageTranslationRepository,
                    languageRepository = container.languageRepository,
                    ttsRepository = container.ttsRepository,
                    favoritesRepository = container.favoritesRepository
                )
            }
            composable(Screen.Voice.route) {
                VoiceScreen(
                    languageRepository = container.languageRepository,
                    speechRepository = container.speechRepository,
                    translationRepository = container.translationRepository,
                    ttsRepository = container.ttsRepository,
                    favoritesRepository = container.favoritesRepository,
                    historyRepository = container.historyRepository,
                    settingsRepository = container.settingsRepository
                )
            }
            composable(Screen.Text.route) {
                TextScreen(
                    languageRepository = container.languageRepository,
                    translationRepository = container.translationRepository,
                    ttsRepository = container.ttsRepository,
                    favoritesRepository = container.favoritesRepository,
                    historyRepository = container.historyRepository
                )
            }
            composable(Screen.Conversation.route) {
                ConversationScreen(
                    languageRepository = container.languageRepository,
                    conversationRepository = container.conversationRepository,
                    speechRepository = container.speechRepository,
                    translationRepository = container.translationRepository,
                    ttsRepository = container.ttsRepository,
                    historyRepository = container.historyRepository
                )
            }
            composable(Screen.Travel.route) {
                TravelScreen(
                    travelRepository = container.travelRepository,
                    ttsRepository = container.ttsRepository,
                    historyRepository = container.historyRepository,
                    favoritesRepository = container.favoritesRepository,
                    offlinePackageRepository = container.offlinePackageRepository,
                    onNavigateToScreen = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    historyRepository = container.historyRepository,
                    ttsRepository = container.ttsRepository,
                    favoritesRepository = container.favoritesRepository,
                    onNavigateToScreen = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    favoritesRepository = container.favoritesRepository,
                    ttsRepository = container.ttsRepository
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsRepository = container.settingsRepository,
                    offlinePackageRepository = container.offlinePackageRepository,
                    secretManager = container.secureSecretManager,
                    languageRepository = container.languageRepository,
                    historyRepository = container.historyRepository,
                    ttsRepository = container.ttsRepository,
                    onNavigateToOfflineLanguages = { navController.navigate(Screen.OfflineLanguages.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
                )
            }
            composable(Screen.OfflineLanguages.route) {
                OfflineLanguagesScreen(
                    offlinePackageRepository = container.offlinePackageRepository,
                    connectivityMonitor = container.connectivityMonitor,
                    ttsRepository = container.ttsRepository
                )
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(
                    container = container
                )
            }
        }
    }
}
