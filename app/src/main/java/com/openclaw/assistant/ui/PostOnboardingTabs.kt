package com.openclaw.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import com.openclaw.assistant.MainActivity
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.PermissionInfo
import com.openclaw.assistant.PermissionStatusInfo
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.diagnostics.VoiceDiagnostic
import com.openclaw.assistant.ui.chat.ChatSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOnboardingTabs(
    settings: SettingsRepository,
    diagnostic: VoiceDiagnostic?,
    missingPermissions: List<PermissionInfo>,
    allPermissionsStatus: List<PermissionStatusInfo>,
    onRefreshDiagnostics: () -> Unit = {},
    onRequestPermissions: (List<String>) -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val runtime = remember(context.applicationContext) {
        (context.applicationContext as OpenClawApplication).nodeRuntime
    }
    val nodeConnected by runtime.isConnected.collectAsState()

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val useNavigationRail = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val navigationItems = listOf(
        Triple(stringResource(R.string.connection), Icons.Default.Link, 0),
        Triple(stringResource(R.string.conversations_title), Icons.AutoMirrored.Filled.Chat, 1),
        Triple(stringResource(R.string.voice), Icons.Default.Mic, 2),
        Triple(stringResource(R.string.capability_screen), Icons.Default.Screenshot, 3),
        Triple(stringResource(R.string.settings_title), Icons.Default.Settings, 4)
    )

    Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                header = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (nodeConnected) Color(0xFF4CAF50) else Color(0xFFF44336))
                            .padding(8.dp)
                    )
                }
            ) {
                navigationItems.forEach { (label, icon, index) ->
                    NavigationRailItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }

    Scaffold(
        modifier = Modifier.weight(1f),
        topBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (nodeConnected) Color(0xFF4CAF50) else Color(0xFFF44336))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    if (selectedTab == 0) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!useNavigationRail) {
                NavigationBar {
                    navigationItems.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> ConnectTabScreen(
                    settings = settings,
                    diagnostic = diagnostic,
                    missingPermissions = missingPermissions,
                    allPermissionsStatus = allPermissionsStatus,
                    onRefreshDiagnostics = onRefreshDiagnostics,
                    onRequestPermissions = onRequestPermissions,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenSettings = { selectedTab = 4 }
                )
                1 -> ChatSheet()
                2 -> VoiceTabScreen(settings = settings)
                3 -> CanvasScreen()
                4 -> SettingsSheet(settings = settings)
            }
        }
    }
    }
}
