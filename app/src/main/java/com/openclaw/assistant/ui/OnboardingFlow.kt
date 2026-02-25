package com.openclaw.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.node.NodeRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OnboardingStep {
    Welcome,
    Gateway,
    Permissions,
    Connect
}

@Composable
fun OnboardingFlow(
    runtime: NodeRuntime,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.Welcome) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step content
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    OnboardingStep.Welcome -> WelcomeStep()
                    OnboardingStep.Gateway -> GatewayStep(runtime)
                    OnboardingStep.Permissions -> PermissionsStep()
                    OnboardingStep.Connect -> ConnectStep(runtime)
                }
            }

            // Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(com.openclaw.assistant.R.string.onboarding_skip))
                }

                Row {
                    if (currentStep != OnboardingStep.Welcome) {
                        TextButton(onClick = { currentStep = getPreviousStep(currentStep) }) {
                            Text(stringResource(com.openclaw.assistant.R.string.onboarding_back))
                        }
                    }

                    Button(onClick = {
                        if (currentStep == OnboardingStep.Connect) {
                            onFinish()
                        } else {
                            currentStep = getNextStep(currentStep)
                        }
                    }) {
                        Text(stringResource(if (currentStep == OnboardingStep.Connect) com.openclaw.assistant.R.string.onboarding_finish else com.openclaw.assistant.R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GatewayStep(runtime: NodeRuntime) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var useSetupCode by remember { mutableStateOf(true) }
    var setupCode by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    var setupCodeError by remember { mutableStateOf<String?>(null) }

    val currentHost by runtime.manualHost.collectAsState()
    val currentPort by runtime.manualPort.collectAsState()
    val currentTls by runtime.manualTls.collectAsState()
    val currentToken by runtime.gatewayToken.collectAsState()

    var host by remember { mutableStateOf(currentHost) }
    var port by remember { mutableStateOf(currentPort.toString()) }
    var tls by remember { mutableStateOf(currentTls) }
    var token by remember { mutableStateOf(currentToken) }

    var showToken by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_gateway_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_gateway_desc),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(selectedTabIndex = if (useSetupCode) 0 else 1) {
            Tab(selected = useSetupCode, onClick = { useSetupCode = true }) {
                Text(stringResource(com.openclaw.assistant.R.string.onboarding_setup_code), modifier = Modifier.padding(12.dp))
            }
            Tab(selected = !useSetupCode, onClick = { useSetupCode = false }) {
                Text(stringResource(com.openclaw.assistant.R.string.onboarding_manual_config), modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (useSetupCode) {
            OutlinedTextField(
                value = setupCode,
                onValueChange = {
                    setupCode = it.take(10)
                    setupCodeError = null
                },
                label = { Text(stringResource(com.openclaw.assistant.R.string.onboarding_setup_code)) },
                placeholder = { Text(stringResource(com.openclaw.assistant.R.string.onboarding_setup_code_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = setupCodeError != null,
                supportingText = {
                    if (setupCodeError != null) {
                        Text(setupCodeError!!)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isResolving = true
                    scope.launch {
                        kotlinx.coroutines.delay(1500)
                        isResolving = false
                        setupCodeError = context.getString(R.string.onboarding_setup_code_unsupported)
                    }
                },
                enabled = setupCode.length >= 6 && !isResolving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isResolving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.onboarding_setup_code))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(com.openclaw.assistant.R.string.gateway_host)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { newValue -> port = newValue.filter { it.isDigit() } },
                    label = { Text(stringResource(com.openclaw.assistant.R.string.gateway_port)) },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(com.openclaw.assistant.R.string.gateway_token)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showToken = !showToken }) {
                        Icon(
                            if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = stringResource(if (showToken) com.openclaw.assistant.R.string.hide_instructions else com.openclaw.assistant.R.string.show_instructions)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = tls, onCheckedChange = { tls = it })
                Text(stringResource(com.openclaw.assistant.R.string.gateway_use_tls))
            }
        }

        DisposableEffect(host, port, tls, token, useSetupCode) {
            onDispose {
                if (!useSetupCode) {
                    runtime.setManualHost(host)
                    runtime.setManualPort(port.toIntOrNull() ?: 18789)
                    runtime.setManualTls(tls)
                    runtime.setGatewayToken(token)
                    runtime.setManualEnabled(true)
                }
            }
        }
    }
}

@Composable
fun PermissionsStep() {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var permissionStatus by remember {
        mutableStateOf(permissionsToRequest.associateWith {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionStatus = permissionsToRequest.associateWith {
            result[it] ?: (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_permissions_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_permissions_desc),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        permissionsToRequest.forEach { permission ->
            val isGranted = permissionStatus[permission] ?: false
            val label = when (permission) {
                Manifest.permission.RECORD_AUDIO -> stringResource(com.openclaw.assistant.R.string.permission_record_audio)
                Manifest.permission.CAMERA -> stringResource(com.openclaw.assistant.R.string.permission_camera)
                Manifest.permission.ACCESS_FINE_LOCATION -> stringResource(com.openclaw.assistant.R.string.permission_location_fine)
                Manifest.permission.ACCESS_COARSE_LOCATION -> stringResource(com.openclaw.assistant.R.string.permission_location_coarse)
                Manifest.permission.POST_NOTIFICATIONS -> stringResource(com.openclaw.assistant.R.string.permission_notifications)
                else -> permission
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (isGranted) {
                    Text(stringResource(com.openclaw.assistant.R.string.permission_status_granted), color = Color(0xFF4CAF50))
                } else {
                    Button(onClick = { launcher.launch(arrayOf(permission)) }) {
                        Text(stringResource(com.openclaw.assistant.R.string.permission_grant))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { launcher.launch(permissionsToRequest.toTypedArray()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(com.openclaw.assistant.R.string.grant_permission))
        }
    }
}

@Composable
fun ConnectStep(runtime: NodeRuntime) {
    val isConnected by runtime.isConnected.collectAsState()
    val statusText by runtime.statusText.collectAsState()

    LaunchedEffect(Unit) {
        if (!isConnected) {
            runtime.connectManual()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_connect_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(com.openclaw.assistant.R.string.onboarding_connect_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isConnected) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(com.openclaw.assistant.R.string.status_online),
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        val displayStatusText = when (statusText) {
            "Operator Online (Node Offline)" -> stringResource(com.openclaw.assistant.R.string.status_operator_online_node_offline)
            "Node Online (Operator Offline)" -> stringResource(com.openclaw.assistant.R.string.status_node_online_operator_offline)
            "Offline" -> stringResource(com.openclaw.assistant.R.string.status_offline)
            else -> statusText
        }

        Text(
            text = displayStatusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (!isConnected) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { runtime.connectManual() }) {
                Text(stringResource(com.openclaw.assistant.R.string.action_try_again))
            }
        }
    }
}

private fun getNextStep(current: OnboardingStep): OnboardingStep {
    return when (current) {
        OnboardingStep.Welcome -> OnboardingStep.Gateway
        OnboardingStep.Gateway -> OnboardingStep.Permissions
        OnboardingStep.Permissions -> OnboardingStep.Connect
        OnboardingStep.Connect -> OnboardingStep.Connect
    }
}

private fun getPreviousStep(current: OnboardingStep): OnboardingStep {
    return when (current) {
        OnboardingStep.Welcome -> OnboardingStep.Welcome
        OnboardingStep.Gateway -> OnboardingStep.Welcome
        OnboardingStep.Permissions -> OnboardingStep.Gateway
        OnboardingStep.Connect -> OnboardingStep.Permissions
    }
}
