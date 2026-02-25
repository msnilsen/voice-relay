package com.openclaw.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.PermissionInfo
import com.openclaw.assistant.PermissionStatusInfo
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.diagnostics.DiagnosticStatus
import com.openclaw.assistant.speech.diagnostics.VoiceDiagnostic
import com.openclaw.assistant.ui.components.PairingRequiredCard
import com.openclaw.assistant.ui.components.CollapsibleSection
import com.openclaw.assistant.ui.GatewayTrustDialog
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast

@Composable
fun ConnectTabScreen(
    settings: SettingsRepository,
    diagnostic: VoiceDiagnostic?,
    missingPermissions: List<PermissionInfo>,
    allPermissionsStatus: List<PermissionStatusInfo>,
    onRefreshDiagnostics: () -> Unit = {},
    onRequestPermissions: (List<String>) -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val runtime = remember(context.applicationContext) {
        (context.applicationContext as OpenClawApplication).nodeRuntime
    }

    val nodeConnected by runtime.isConnected.collectAsState()
    val nodeStatusText by runtime.statusText.collectAsState()
    val isPairingRequired by runtime.isPairingRequired.collectAsState()
    val isOperatorOffline by runtime.isOperatorOffline.collectAsState()
    val deviceId = runtime.deviceId
    val displayName by runtime.displayName.collectAsState()
    val pendingGatewayTrust by runtime.pendingGatewayTrust.collectAsState()

    val manualHostState by runtime.manualHost.collectAsState()
    val manualPortState by runtime.manualPort.collectAsState()
    val manualTlsState by runtime.manualTls.collectAsState()
    val gatewayTokenState by runtime.gatewayToken.collectAsState()

    var gatewayHost by rememberSaveable { mutableStateOf(manualHostState) }
    var gatewayPort by rememberSaveable { mutableStateOf(manualPortState.toString()) }
    var gatewayTls by rememberSaveable { mutableStateOf(manualTlsState) }
    var gatewayToken by rememberSaveable { mutableStateOf(gatewayTokenState) }
    var showNodeToken by rememberSaveable { mutableStateOf(false) }

    // Update local state if runtime state changes behind the scenes
    LaunchedEffect(manualHostState, manualPortState, manualTlsState, gatewayTokenState) {
        gatewayHost = manualHostState
        gatewayPort = manualPortState.toString()
        gatewayTls = manualTlsState
        gatewayToken = gatewayTokenState
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pendingGatewayTrust != null) {
            GatewayTrustDialog(
                prompt = pendingGatewayTrust!!,
                onAccept = { runtime.acceptGatewayTrustPrompt() },
                onDecline = { runtime.declineGatewayTrustPrompt() }
            )
        }

        // Show pairing required banner
        if (isPairingRequired && deviceId != null) {
            PairingRequiredCard(deviceId = deviceId, displayName = displayName)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Show operator offline warning
        if (isOperatorOffline && deviceId != null) {
            OperatorOfflineCard(deviceId = deviceId, displayName = displayName)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // === SYSTEM STATUS CARD ===
        val displayStatusText = when (nodeStatusText) {
            "Operator Online (Node Offline)" -> stringResource(R.string.status_operator_online_node_offline)
            "Node Online (Operator Offline)" -> stringResource(R.string.status_node_online_operator_offline)
            "Offline" -> stringResource(R.string.status_offline)
            else -> nodeStatusText
        }

        SystemStatusCard(
            connected = nodeConnected,
            statusText = displayStatusText,
            onConnect = {
                runtime.setManualEnabled(true)
                runtime.setManualHost(gatewayHost.trim())
                runtime.setManualPort(gatewayPort.toIntOrNull() ?: 18789)
                runtime.setManualTls(gatewayTls)
                runtime.setGatewayToken(gatewayToken.trim())
                runtime.connectManual()
            },
            onDisconnect = { runtime.disconnect() },
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (missingPermissions.isNotEmpty()) {
            PermissionStatusCard(
                missingPermissions = missingPermissions,
                onRequestPermissions = onRequestPermissions,
                onOpenAppSettings = onOpenAppSettings
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (diagnostic != null || allPermissionsStatus.isNotEmpty()) {
            CollapsibleSection(
                title = stringResource(R.string.diagnostics_title),
                initiallyExpanded = diagnostic?.let { it.sttStatus != DiagnosticStatus.READY || it.ttsStatus != DiagnosticStatus.READY } ?: false
            ) {
                if (diagnostic != null) {
                    DiagnosticPanel(diagnostic, onRefreshDiagnostics)
                }
                if (diagnostic != null && allPermissionsStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (allPermissionsStatus.isNotEmpty()) {
                    PermissionDiagnosticsPanel(allPermissionsStatus, onRefreshDiagnostics)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // === GATEWAY CONFIGURATION ===
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.gateway_configuration), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gatewayHost,
                        onValueChange = { gatewayHost = it },
                        label = { Text(stringResource(R.string.gateway_host)) },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    OutlinedTextField(
                        value = gatewayPort,
                        onValueChange = { gatewayPort = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(R.string.gateway_port)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gatewayToken,
                    onValueChange = { gatewayToken = it },
                    label = { Text(stringResource(R.string.gateway_token)) },
                    trailingIcon = {
                        IconButton(onClick = { showNodeToken = !showNodeToken }) {
                            Icon(
                                if (showNodeToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showNodeToken) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.gateway_use_tls), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.gateway_use_tls_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = gatewayTls,
                        onCheckedChange = { gatewayTls = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        runtime.setManualEnabled(true)
                        runtime.setManualHost(gatewayHost.trim())
                        runtime.setManualPort(gatewayPort.toIntOrNull() ?: 18789)
                        runtime.setManualTls(gatewayTls)
                        runtime.setGatewayToken(gatewayToken.trim())
                        runtime.connectManual()
                        Toast.makeText(context, R.string.gateway_settings_applied, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = gatewayHost.isNotBlank()
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

@Composable
fun DiagnosticPanel(diagnostic: VoiceDiagnostic, onRefresh: () -> Unit) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.diagnostic_engines), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DiagnosticItem(label = "In: ${diagnostic.sttEngine?.take(10) ?: "Def"}", status = diagnostic.sttStatus, modifier = Modifier.weight(1f))
                DiagnosticItem(label = "Out: ${diagnostic.ttsEngine?.split('.')?.lastOrNull() ?: "null"}", status = diagnostic.ttsStatus, modifier = Modifier.weight(1f))
            }
            if (diagnostic.suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                diagnostic.suggestions.forEach { SuggestionItem(it) }
            }
        }
    }
}

@Composable
fun PermissionDiagnosticsPanel(allPermissionsStatus: List<PermissionStatusInfo>, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.diagnostic_app_permissions), fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            allPermissionsStatus.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = if (perm.isGranted) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val icon = if (perm.isGranted) Icons.Default.Check else Icons.Default.Close
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(perm.permissionName, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(
                        if (perm.isGranted) stringResource(R.string.permission_status_granted)
                        else stringResource(R.string.permission_status_denied),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticItem(label: String, status: DiagnosticStatus, modifier: Modifier = Modifier) {
    val color = when (status) { DiagnosticStatus.READY -> Color(0xFF4CAF50); DiagnosticStatus.WARNING -> Color(0xFFFFC107); DiagnosticStatus.ERROR -> Color(0xFFF44336) }
    val icon = when (status) { DiagnosticStatus.READY -> Icons.Default.Check; DiagnosticStatus.WARNING -> Icons.Default.Info; DiagnosticStatus.ERROR -> Icons.Default.Error }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SuggestionItem(suggestion: com.openclaw.assistant.speech.diagnostics.DiagnosticSuggestion) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = suggestion.message, modifier = Modifier.weight(1f), fontSize = 12.sp, lineHeight = 16.sp)
            if (suggestion.actionLabel != null && suggestion.intent != null) {
                TextButton(onClick = { try { context.startActivity(suggestion.intent) } catch (e: Exception) { Toast.makeText(context, context.getString(R.string.state_error), Toast.LENGTH_SHORT).show() } }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(suggestion.actionLabel, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    missingPermissions: List<PermissionInfo>,
    onRequestPermissions: (List<String>) -> Unit,
    onOpenAppSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.permissions_missing_title),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            missingPermissions.forEach { perm ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(perm.nameResId),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = stringResource(perm.descResId),
                            fontSize = 12.sp,
                            color = Color(0xFF795548)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.open_settings), color = Color(0xFFE65100))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onRequestPermissions(missingPermissions.map { it.permission }) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text(stringResource(R.string.permission_grant))
                }
            }
        }
    }
}

@Composable
fun SystemStatusCard(
    connected: Boolean,
    statusText: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val backgroundColor = if (connected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (connected) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    val statusDotColor = if (connected) Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor,
                            containerColor = Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.5f))
                    ) {
                        Text(stringResource(R.string.disconnect))
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                }
            }
        }
    }
}

@Composable
fun OperatorOfflineCard(deviceId: String, displayName: String = "") {
    val context = LocalContext.current
    val safeName = displayName.replace("\\", "\\\\").replace("'", "\\'")
    val safeId = deviceId.replace("\\", "\\\\").replace("'", "\\'")
    val pythonScript = "import sys,json;d=json.load(sys.stdin);ids={'$safeName','$safeId'};r=next((x for x in d.get('pending',[]) if any(str(v) in ids for v in x.values())),None);print(next((str(v) for k,v in (r or {}).items() if k.lower()=='request'),'NOT_FOUND'))"
    val command = "openclaw devices approve \$(openclaw devices list --json | python3 -c \"$pythonScript\")"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.operator_offline_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.operator_offline_message),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Command", command)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.operator_offline_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.operator_offline_copy), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MissingScopeCard(error: String, onAskAi: (String) -> Unit, onOpenSettings: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permission_error_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.permission_error_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.fix_request_label),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                val fixMessage = stringResource(R.string.fix_request_message)
                Text(
                    text = fixMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onAskAi(fixMessage) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_ask_ai))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_open_settings))
                }
            }
        }
    }
}
