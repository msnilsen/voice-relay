package com.openclaw.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.R
import com.openclaw.assistant.ui.components.CapabilityCard

@Composable
fun CanvasScreen() {
    val context = LocalContext.current
    val runtime = remember(context.applicationContext) {
        (context.applicationContext as OpenClawApplication).nodeRuntime
    }

    val screenRecordActive by runtime.screenRecordActive.collectAsState()
    var showScreenCaptureDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.capability_screen),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (screenRecordActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (screenRecordActive) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (screenRecordActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (screenRecordActive) stringResource(R.string.capability_screen_active) else stringResource(R.string.capability_off),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showScreenCaptureDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (screenRecordActive) stringResource(R.string.stop) else stringResource(R.string.start))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.screen_capture_explain_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showScreenCaptureDialog) {
        AlertDialog(
            onDismissRequest = { showScreenCaptureDialog = false },
            title = { Text(stringResource(R.string.screen_capture_title)) },
            text = { Text(stringResource(R.string.screen_capture_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showScreenCaptureDialog = false
                    runtime.setScreenRecordActive(!screenRecordActive)
                }) {
                    Text(stringResource(if (screenRecordActive) R.string.stop else R.string.start))
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenCaptureDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
