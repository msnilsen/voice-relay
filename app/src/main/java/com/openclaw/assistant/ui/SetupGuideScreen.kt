package com.openclaw.assistant.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.openclaw.assistant.R
import com.openclaw.assistant.api.RequestFormat
import com.openclaw.assistant.api.WebhookClient
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.ui.components.ConnectionState
import com.openclaw.assistant.ui.components.StatusIndicator
import kotlinx.coroutines.launch

private enum class SetupStep(val index: Int) {
    Welcome(1),
    Connection(2),
    Permissions(3),
    FinalCheck(4)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    settings: SettingsRepository,
    onComplete: () -> Unit
) {
    var currentStep by rememberSaveable { mutableStateOf(SetupStep.Welcome) }

    var webhookUrl by rememberSaveable { mutableStateOf(settings.httpUrl) }
    var authToken by rememberSaveable { mutableStateOf(settings.authToken) }
    var requestFormat by rememberSaveable { mutableStateOf(settings.requestFormat) }
    var customJsonTemplate by rememberSaveable { mutableStateOf(settings.customJsonTemplate) }

    val totalSteps = SetupStep.entries.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.setup_guide_first_run),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.setup_guide_step_format, currentStep.index, totalSteps),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    if (currentStep.index > 1) {
                        IconButton(onClick = {
                            val prevIndex = currentStep.index - 1
                            currentStep = SetupStep.entries.first { it.index == prevIndex }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            when (currentStep) {
                SetupStep.Welcome -> WelcomeStep(
                    onNext = { currentStep = SetupStep.Connection }
                )
                SetupStep.Connection -> ConnectionStep(
                    webhookUrl = webhookUrl,
                    authToken = authToken,
                    requestFormat = requestFormat,
                    customJsonTemplate = customJsonTemplate,
                    onWebhookUrlChange = { webhookUrl = it },
                    onAuthTokenChange = { authToken = it },
                    onRequestFormatChange = { requestFormat = it },
                    onCustomJsonTemplateChange = { customJsonTemplate = it },
                    onNext = {
                        settings.httpUrl = webhookUrl.trim()
                        settings.authToken = authToken.trim()
                        settings.requestFormat = requestFormat
                        settings.customJsonTemplate = customJsonTemplate
                        currentStep = SetupStep.Permissions
                    }
                )
                SetupStep.Permissions -> PermissionsStep(
                    onNext = { currentStep = SetupStep.FinalCheck }
                )
                SetupStep.FinalCheck -> FinalCheckStep(
                    settings = settings,
                    onFinish = {
                        settings.hasCompletedSetup = true
                        onComplete()
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Launch,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.setup_guide_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(32.dp))

        BulletPoint(stringResource(R.string.setup_guide_welcome_bullet_1))
        BulletPoint(stringResource(R.string.setup_guide_welcome_bullet_2))
        BulletPoint(stringResource(R.string.setup_guide_welcome_bullet_3))
        BulletPoint(stringResource(R.string.setup_guide_welcome_bullet_4))

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.setup_guide_next), fontSize = 18.sp)
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(24.dp).padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionStep(
    webhookUrl: String,
    authToken: String,
    requestFormat: String,
    customJsonTemplate: String,
    onWebhookUrlChange: (String) -> Unit,
    onAuthTokenChange: (String) -> Unit,
    onRequestFormatChange: (String) -> Unit,
    onCustomJsonTemplateChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.setup_guide_connection_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = webhookUrl,
            onValueChange = onWebhookUrlChange,
            label = { Text(stringResource(R.string.webhook_url_label)) },
            placeholder = { Text(stringResource(R.string.webhook_url_hint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = authToken,
            onValueChange = onAuthTokenChange,
            label = { Text(stringResource(R.string.setup_guide_manual_token)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.request_format_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = requestFormat == SettingsRepository.REQUEST_FORMAT_SIMPLE,
                onClick = { onRequestFormatChange(SettingsRepository.REQUEST_FORMAT_SIMPLE) },
                label = { Text(stringResource(R.string.request_format_simple)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = requestFormat == SettingsRepository.REQUEST_FORMAT_OPENAI,
                onClick = { onRequestFormatChange(SettingsRepository.REQUEST_FORMAT_OPENAI) },
                label = { Text(stringResource(R.string.request_format_openai)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = requestFormat == SettingsRepository.REQUEST_FORMAT_CUSTOM,
                onClick = { onRequestFormatChange(SettingsRepository.REQUEST_FORMAT_CUSTOM) },
                label = { Text(stringResource(R.string.request_format_custom)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (requestFormat) {
                SettingsRepository.REQUEST_FORMAT_OPENAI -> stringResource(R.string.request_format_openai_desc)
                SettingsRepository.REQUEST_FORMAT_CUSTOM -> stringResource(R.string.request_format_custom_desc)
                else -> stringResource(R.string.request_format_simple_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (requestFormat == SettingsRepository.REQUEST_FORMAT_CUSTOM) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = customJsonTemplate,
                onValueChange = onCustomJsonTemplateChange,
                label = { Text(stringResource(R.string.custom_json_template_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                maxLines = 10
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            enabled = webhookUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.setup_guide_next), fontSize = 18.sp)
        }
    }
}

@Composable
private fun PermissionsStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val permissions = remember {
        mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var permissionsStatus by remember {
        mutableStateOf(permissions.associateWith {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsStatus = results.mapValues { it.value }
    }

    Column {
        Text(
            text = stringResource(R.string.setup_guide_permissions_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_guide_permissions_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        PermissionItem(
            icon = Icons.Default.Mic,
            name = stringResource(R.string.permission_record_audio),
            desc = stringResource(R.string.permission_record_audio_desc),
            isGranted = permissionsStatus[Manifest.permission.RECORD_AUDIO] == true
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionItem(
                icon = Icons.Default.Notifications,
                name = stringResource(R.string.permission_notifications),
                desc = stringResource(R.string.permission_post_notifications_desc),
                isGranted = permissionsStatus[Manifest.permission.POST_NOTIFICATIONS] == true
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { launcher.launch(permissions.toTypedArray()) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.grant_permission), fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.setup_guide_next), fontSize = 18.sp)
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    name: String,
    desc: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isGranted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Bold)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isGranted) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
        }
    }
}

@Composable
private fun FinalCheckStep(
    settings: SettingsRepository,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val apiClient = remember {
        WebhookClient(
            ignoreSslErrors = settings.httpIgnoreSslErrors,
            customJsonTemplate = settings.customJsonTemplate
        )
    }
    var testStatus by remember { mutableStateOf<TestStatus>(TestStatus.Idle) }

    val finish: () -> Unit = {
        if (testStatus is TestStatus.Success) {
            settings.isVerified = true
        }
        onFinish()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.setup_guide_final_check_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.setup_guide_final_check_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (testStatus) {
            TestStatus.Idle -> {
                Text(
                    text = stringResource(R.string.setup_guide_test_connection_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            TestStatus.Testing -> {
                StatusIndicator(
                    state = ConnectionState.Connecting,
                    label = stringResource(R.string.test_connection_button),
                    modifier = Modifier.padding(16.dp)
                )
            }
            is TestStatus.Success -> {
                StatusIndicator(
                    state = ConnectionState.Connected,
                    label = (testStatus as TestStatus.Success).message,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is TestStatus.Failed -> {
                StatusIndicator(
                    state = ConnectionState.Disconnected,
                    label = stringResource(R.string.connection_failed_title),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (testStatus as TestStatus.Failed).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val successMsg = stringResource(R.string.webhook_configured)
        when (testStatus) {
            TestStatus.Idle -> {
                Button(
                    onClick = {
                        testStatus = TestStatus.Testing
                        scope.launch {
                            val testUrl = settings.getWebhookUrl()
                            val format = RequestFormat.fromString(settings.requestFormat)
                            val result = apiClient.testConnection(testUrl, settings.authToken.ifBlank { null }, format)
                            testStatus = if (result.isSuccess) {
                                TestStatus.Success(successMsg)
                            } else {
                                TestStatus.Failed(result.exceptionOrNull()?.message ?: "Connection failed")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.test_connection_button), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = finish,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.setup_guide_finish), fontSize = 16.sp)
                }
            }
            TestStatus.Testing -> {
                // Show nothing extra while testing
            }
            is TestStatus.Success -> {
                Button(
                    onClick = finish,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.setup_guide_finish), fontSize = 18.sp)
                }
            }
            is TestStatus.Failed -> {
                Button(
                    onClick = {
                        testStatus = TestStatus.Idle
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.test_connection_button), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = finish,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.setup_guide_finish), fontSize = 16.sp)
                }
            }
        }
    }
}

private sealed class TestStatus {
    data object Idle : TestStatus()
    data object Testing : TestStatus()
    data class Success(val message: String) : TestStatus()
    data class Failed(val message: String) : TestStatus()
}
