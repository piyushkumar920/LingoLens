package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.di.AppContainer
import com.example.modules.diagnostics.engine.TranslationTestLabEngine
import com.example.modules.diagnostics.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val testEngine = remember { TranslationTestLabEngine(context, container) }

    // Tab Navigation: 0 = Dashboard/Full Test, 1 = Individual Tests, 2 = Performance, 3 = Error Logs
    var selectedTab by remember { mutableIntStateOf(0) }

    // System Capability State
    var capabilities by remember { mutableStateOf<List<SystemCapabilityItem>>(emptyList()) }
    var isCheckingCapabilities by remember { mutableStateOf(false) }

    // Full Test Suite State
    var isRunningFullTest by remember { mutableStateOf(false) }
    var fullTestItems by remember { mutableStateOf<List<TestProgressItem>>(emptyList()) }
    var fullTestSummary by remember { mutableStateOf<TestLabSummary?>(null) }

    // Individual Tests State
    var activeIndividualTest by remember { mutableStateOf<String?>(null) }
    var individualTestLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var individualTestResult by remember { mutableStateOf<Triple<Boolean, String, Long>?>(null) }

    // Error Logs State
    val errorLogs by testEngine.errorLogs.collectAsState()
    var isDeveloperModeEnabled by remember { mutableStateOf(false) }
    var expandedLogId by remember { mutableStateOf<String?>(null) }

    // Performance State
    var performanceMetrics by remember { mutableStateOf<List<PerformanceMetricItem>>(emptyList()) }

    // Initial system load
    LaunchedEffect(Unit) {
        isCheckingCapabilities = true
        capabilities = testEngine.checkSystemCapabilities()
        performanceMetrics = testEngine.getPerformanceMetrics()
        isCheckingCapabilities = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("translation_test_lab_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Translation Test Lab",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verify every translation pipeline and device capability",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isCheckingCapabilities = true
                                capabilities = testEngine.checkSystemCapabilities()
                                performanceMetrics = testEngine.getPerformanceMetrics()
                                isCheckingCapabilities = false
                            }
                        },
                        modifier = Modifier.testTag("refresh_capabilities_button")
                    ) {
                        if (isCheckingCapabilities) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh System Checks", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Category Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview & Full Test", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_overview")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Individual Tests", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_individual")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            performanceMetrics = testEngine.getPerformanceMetrics()
                        },
                        text = { Text("Performance", style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_performance")
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            val count = errorLogs.size
                            Text(if (count > 0) "Logs ($count)" else "Logs", style = MaterialTheme.typography.labelMedium)
                        },
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_logs")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> OverviewAndFullTestTab(
                    capabilities = capabilities,
                    isCheckingCapabilities = isCheckingCapabilities,
                    isRunningFullTest = isRunningFullTest,
                    fullTestItems = fullTestItems,
                    fullTestSummary = fullTestSummary,
                    onRunFullTest = {
                        coroutineScope.launch {
                            isRunningFullTest = true
                            fullTestSummary = null
                            testEngine.runFullTest { items, summary ->
                                fullTestItems = items
                                fullTestSummary = summary
                            }
                            isRunningFullTest = false
                            performanceMetrics = testEngine.getPerformanceMetrics()
                        }
                    }
                )

                1 -> IndividualTestsTab(
                    activeTest = activeIndividualTest,
                    testLogs = individualTestLogs,
                    testResult = individualTestResult,
                    onRunTest = { testName ->
                        activeIndividualTest = testName
                        individualTestLogs = emptyList()
                        individualTestResult = null
                        coroutineScope.launch {
                            when (testName) {
                                "CAMERA" -> {
                                    val res = testEngine.runCameraTest(steps = 10) { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "REPEATED_TRANSLATION" -> {
                                    val res = testEngine.runRepeatedTranslationTest(count = 10) { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "TEXT_TRANSLATION" -> {
                                    val res = testEngine.runTextTranslationTest { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "VOICE" -> {
                                    val res = testEngine.runVoiceTest { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "DIALOGUE" -> {
                                    val res = testEngine.runDialogueTest(turns = 6) { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "TTS" -> {
                                    val res = testEngine.runTtsTest { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "OFFLINE" -> {
                                    val res = testEngine.runOfflineTest { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "LANGUAGE_SWITCH" -> {
                                    val res = testEngine.runLanguageSwitchTest { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "LIFECYCLE" -> {
                                    val res = testEngine.runLifecycleTest { _, _, log ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "STRESS_20" -> {
                                    val res = testEngine.runStressTest(count = 20) { _, _, log, _ ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                                "CAMERA_STRESS_20" -> {
                                    val res = testEngine.runCameraStressTest(count = 20) { _, _, log, _ ->
                                        individualTestLogs = individualTestLogs + log
                                    }
                                    individualTestResult = res
                                }
                            }
                            activeIndividualTest = null
                            performanceMetrics = testEngine.getPerformanceMetrics()
                        }
                    }
                )

                2 -> PerformanceMetricsTab(
                    metrics = performanceMetrics,
                    container = container,
                    onRefresh = {
                        performanceMetrics = testEngine.getPerformanceMetrics()
                    }
                )

                3 -> ErrorDiagnosticsTab(
                    logs = errorLogs,
                    isDeveloperMode = isDeveloperModeEnabled,
                    expandedId = expandedLogId,
                    onToggleDeveloperMode = { isDeveloperModeEnabled = it },
                    onToggleExpand = { id ->
                        expandedLogId = if (expandedLogId == id) null else id
                    },
                    onClearLogs = { testEngine.clearLogs() }
                )
            }
        }
    }
}

// =============================================================================
// TAB 1: OVERVIEW & FULL TEST SUITE
// =============================================================================

@Composable
private fun OverviewAndFullTestTab(
    capabilities: List<SystemCapabilityItem>,
    isCheckingCapabilities: Boolean,
    isRunningFullTest: Boolean,
    fullTestItems: List<TestProgressItem>,
    fullTestSummary: TestLabSummary?,
    onRunFullTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Run Full Test Hero Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Automated QA Test Suite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "Execute all 10 component tests sequentially: Camera, OCR, Translation, 10x Consecutive, Speech, Dialogue (6 turns), TTS, Offline, Language Switching, and Lifecycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )

                Button(
                    onClick = onRunFullTest,
                    enabled = !isRunningFullTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("run_full_test_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isRunningFullTest) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Executing Test Suite...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RUN FULL TEST",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Test Summary Card if Completed
                if (fullTestSummary != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TEST COMPLETE",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (fullTestSummary.failed == 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Passed: ${fullTestSummary.passed}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Failed: ${fullTestSummary.failed}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fullTestSummary.failed > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Warnings: ${fullTestSummary.warnings}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fullTestSummary.warnings > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Total Duration: ${fullTestSummary.totalDurationMs} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Live Test Progress Items List
        if (fullTestItems.isNotEmpty()) {
            Text(
                text = "Live Execution Pipeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fullTestItems.forEach { item ->
                    TestProgressRow(item = item)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // System Capabilities Live Status Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM STATUS (13 Live Checks)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isCheckingCapabilities) {
                Text(
                    text = "Checking...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Live runtime checks across all translation, audio, vision, and hardware subsystems. Every check is derived directly from live device APIs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Capabilities List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            capabilities.forEach { capability ->
                CapabilityCard(capability = capability)
            }
        }
    }
}

@Composable
private fun TestProgressRow(item: TestProgressItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                TestStatus.PASS -> Color(0xFF10B981).copy(alpha = 0.12f)
                TestStatus.FAIL -> Color(0xFFEF4444).copy(alpha = 0.12f)
                TestStatus.WARNING -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                TestStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                TestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Animated Status Icon
            when (item.status) {
                TestStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TestStatus.PASS -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pass",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TestStatus.FAIL -> {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Fail",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TestStatus.WARNING -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TestStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.details.isNotBlank()) {
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = item.status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = item.status.color
            )
        }
    }
}

@Composable
private fun CapabilityCard(capability: SystemCapabilityItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = capability.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = capability.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = capability.status.color.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, capability.status.color.copy(alpha = 0.4f))
            ) {
                Text(
                    text = capability.status.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = capability.status.color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// =============================================================================
// TAB 2: INDIVIDUAL TESTS
// =============================================================================

@Composable
private fun IndividualTestsTab(
    activeTest: String?,
    testLogs: List<String>,
    testResult: Triple<Boolean, String, Long>?,
    onRunTest: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Targeted Feature QA Tests",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Isolate and stress individual pipelines without app refreshes. Each test records latency, throughput, and error recovery actions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Camera Test (10 consecutive cycles)
        IndividualTestCard(
            title = "1. Camera Test (10 Cycles)",
            description = "Camera permission, CameraX lifecycle, ImageAnalysis, 10 consecutive OCR → translation cycles without app refresh.",
            icon = Icons.Default.CameraAlt,
            isRunning = activeTest == "CAMERA",
            buttonTag = "camera_test_button",
            onClick = { onRunTest("CAMERA") }
        )

        // 2. Repeated Translation Test (10 consecutive cycles)
        IndividualTestCard(
            title = "2. Repeated Translation Test (10x)",
            description = "10 consecutive translations across EN→HI, HI→EN, EN→ES, ES→EN. Verifies healthy translator, models loaded, zero connection failures.",
            icon = Icons.Default.Repeat,
            isRunning = activeTest == "REPEATED_TRANSLATION",
            buttonTag = "repeated_translation_test_button",
            onClick = { onRunTest("REPEATED_TRANSLATION") }
        )

        // 3. Text Translation Test (5 sentences)
        IndividualTestCard(
            title = "3. Text Translation Pipeline",
            description = "Tests 5 sentences through Text input → language detection → translation → TTS output.",
            icon = Icons.Default.Translate,
            isRunning = activeTest == "TEXT_TRANSLATION",
            buttonTag = "text_translation_test_button",
            onClick = { onRunTest("TEXT_TRANSLATION") }
        )

        // 4. Voice Test
        IndividualTestCard(
            title = "4. Voice Speech Test",
            description = "Microphone check, SpeechRecognizer init on Main thread, listening, translation, vocalization, and safe teardown.",
            icon = Icons.Default.Mic,
            isRunning = activeTest == "VOICE",
            buttonTag = "voice_test_button",
            onClick = { onRunTest("VOICE") }
        )

        // 5. Dialogue Test (6 alternating turns)
        IndividualTestCard(
            title = "5. Dialogue Mode (6 Alternating Turns)",
            description = "A → B → A → B → A → B turns. Verifies clean microphone handover, isolated audio sessions, and zero connection disconnects.",
            icon = Icons.AutoMirrored.Filled.Chat,
            isRunning = activeTest == "DIALOGUE",
            buttonTag = "dialogue_test_button",
            onClick = { onRunTest("DIALOGUE") }
        )

        // 6. TTS Test
        IndividualTestCard(
            title = "6. Text-to-Speech (TTS) Test",
            description = "Start → Speak → Stop → Change Language → Speak again. Verifies non-blocking speech playback and clean stop.",
            icon = Icons.Default.VolumeUp,
            isRunning = activeTest == "TTS",
            buttonTag = "tts_test_button",
            onClick = { onRunTest("TTS") }
        )

        // 7. Offline Test
        IndividualTestCard(
            title = "7. Pure Offline Translation Test",
            description = "Verifies on-device ML Kit models without remote network connectivity. Zero Gemini or external API dependence.",
            icon = Icons.Default.WifiOff,
            isRunning = activeTest == "OFFLINE",
            buttonTag = "offline_test_button",
            onClick = { onRunTest("OFFLINE") }
        )

        // 8. Language Switch Test
        IndividualTestCard(
            title = "8. Language Switch Test",
            description = "Switches between EN→HI, EN→ES, HI→EN, ES→EN. Verifies no stale translator instances remain.",
            icon = Icons.Default.SwapHoriz,
            isRunning = activeTest == "LANGUAGE_SWITCH",
            buttonTag = "language_switch_test_button",
            onClick = { onRunTest("LANGUAGE_SWITCH") }
        )

        // 9. Lifecycle Test
        IndividualTestCard(
            title = "9. Resource Lifecycle Test",
            description = "Simulates screen exit/return, camera backgrounding/resume, and dialogue handover across lifecycle events.",
            icon = Icons.Default.Sync,
            isRunning = activeTest == "LIFECYCLE",
            buttonTag = "lifecycle_test_button",
            onClick = { onRunTest("LIFECYCLE") }
        )

        // 10. Stress Tests Section
        Text(
            text = "Stress & Bottleneck Benchmarks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onRunTest("STRESS_20") },
                enabled = activeTest == null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("stress_test_20_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (activeTest == "STRESS_20") {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("RUN 20 TRANSLATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { onRunTest("CAMERA_STRESS_20") },
                enabled = activeTest == null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("camera_stress_test_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (activeTest == "CAMERA_STRESS_20") {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onTertiary)
                } else {
                    Text("CAMERA STRESS TEST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Log Output Console
        if (testLogs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Execution Log Console:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    testLogs.forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (log.contains("✓")) Color(0xFF10B981) else if (log.contains("✕")) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (testResult != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = if (testResult.first) "✓ RESULT: PASS (${testResult.third} ms)" else "✕ RESULT: FAIL (${testResult.third} ms)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (testResult.first) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Text(
                            text = testResult.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndividualTestCard(
    title: String,
    description: String,
    icon: ImageVector,
    isRunning: Boolean,
    buttonTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onClick,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(buttonTag),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...", style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Test", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =============================================================================
// TAB 3: PERFORMANCE METRICS & BOTTLENECK ANALYSIS
// =============================================================================

@Composable
private fun PerformanceMetricsTab(
    metrics: List<PerformanceMetricItem>,
    container: AppContainer,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_performance_button")) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Metrics", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Text(
            text = "Real-time latency breakdown across OCR, Language ID, Neural Translation, and Audio playback. Includes Cache Hit rates to measure zero-network acceleration.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Metrics Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            metrics.forEach { metric ->
                PerformanceMetricCard(metric = metric)
            }
        }

        // Cache Statistics Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Translation Engine Telemetry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                MetricDataRow("Current Lifecycle State", container.translationService.translatorLifecycleState)
                MetricDataRow("Active Language Pair", container.translationService.currentPair)
                MetricDataRow("Total Requests Handled", "${container.translationService.totalRequestsCount}")
                MetricDataRow("Successful Requests", "${container.translationService.successfulRequestsCount}")
                MetricDataRow("Failed Requests", "${container.translationService.failedRequestsCount}")
                MetricDataRow("In-Memory Cache Hits", "${container.translationService.cacheHitCount}")
                MetricDataRow("In-Memory Cache Misses", "${container.translationService.cacheMissCount}")
                MetricDataRow("Last Recorded Request ID", "#${container.translationService.lastRequestId}")
                MetricDataRow("Last Execution Latency", "${container.translationService.lastLatencyMs} ms")
            }
        }
    }
}

@Composable
private fun PerformanceMetricCard(metric: PerformanceMetricItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${metric.currentMs} ${metric.unit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Visual Gauge Bar
            val progressFraction = if (metric.unit == "%") {
                (metric.currentMs / 100f).coerceIn(0f, 1f)
            } else {
                (metric.currentMs / 250f).coerceIn(0.05f, 1f)
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    metric.unit == "%" -> Color(0xFF10B981)
                    metric.currentMs < 60 -> Color(0xFF10B981)
                    metric.currentMs < 150 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Avg: ${String.format("%.1f", metric.avgMs)} ${metric.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Min: ${metric.minMs} ${metric.unit} | Max: ${metric.maxMs} ${metric.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Samples: ${metric.sampleCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

// =============================================================================
// TAB 4: ERROR DIAGNOSTICS & TELEMETRY LOGS
// =============================================================================

@Composable
private fun ErrorDiagnosticsTab(
    logs: List<DiagnosticErrorLog>,
    isDeveloperMode: Boolean,
    expandedId: String?,
    onToggleDeveloperMode: (Boolean) -> Unit,
    onToggleExpand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Error Diagnostics Console",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Expandable operational logs with recovery actions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(onClick = onClearLogs, modifier = Modifier.testTag("clear_logs_button")) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Developer Mode Switch (exposes raw stack traces)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Developer Mode (Stack Traces)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isDeveloperMode) "Exposing full exception stack traces for debugging" else "Showing user-friendly readable diagnostics and recovery actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDeveloperMode,
                    onCheckedChange = onToggleDeveloperMode,
                    modifier = Modifier.testTag("developer_mode_switch")
                )
            }
        }

        if (logs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Zero Runtime Errors Recorded",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "All OCR, translation, voice, and audio subsystems are operating smoothly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.forEach { log ->
                    val isExpanded = expandedId == log.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleExpand(log.id) }
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = log.feature,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Operation: ${log.operation} (${log.sourceLanguage.uppercase()} → ${log.targetLanguage.uppercase()})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "Error: ${log.readableErrorMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Recovery Action: ${log.recoveryAction}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (isDeveloperMode && isExpanded && log.fullStackTrace != null) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Stack Trace:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.fullStackTrace,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
