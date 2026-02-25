package com.openclaw.assistant.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.service.OpenClawAssistantService

@Composable
fun VoiceTabScreen(
    settings: SettingsRepository
) {
    val context = LocalContext.current
    val wakeWord = settings.getWakeWordDisplayName()
    val hotwordEnabled = settings.hotwordEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.voice),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        LargeFloatingActionButton(
            onClick = {
                val intent = Intent(context, OpenClawAssistantService::class.java).apply {
                    action = OpenClawAssistantService.ACTION_SHOW_ASSISTANT
                }
                context.startService(intent)
            },
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = stringResource(R.string.listening),
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.hotword_title, wakeWord),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = if (hotwordEnabled) stringResource(R.string.hotword_listening) else stringResource(R.string.disabled),
            style = MaterialTheme.typography.bodyMedium,
            color = if (hotwordEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.how_to_use),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.step_1, wakeWord),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
