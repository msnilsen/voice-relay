package com.openclaw.assistant.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.service.HotwordService
import com.openclaw.assistant.ui.components.CollapsibleSection
import com.openclaw.assistant.utils.SystemInfoProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runtime = remember(context.applicationContext) {
        (context.applicationContext as OpenClawApplication).nodeRuntime
    }

    var ttsEnabled by rememberSaveable { mutableStateOf(settings.ttsEnabled) }
    var ttsSpeed by rememberSaveable { mutableStateOf(settings.ttsSpeed) }
    var continuousMode by rememberSaveable { mutableStateOf(settings.continuousMode) }
    var resumeLatestSession by rememberSaveable { mutableStateOf(settings.resumeLatestSession) }
    var wakeWordPreset by rememberSaveable { mutableStateOf(settings.wakeWordPreset) }
    var customWakeWord by rememberSaveable { mutableStateOf(settings.customWakeWord) }
    var speechSilenceTimeout by rememberSaveable { mutableStateOf(settings.speechSilenceTimeout.toFloat().coerceIn(5000f, 30000f)) }
    var speechLanguage by rememberSaveable { mutableStateOf(settings.speechLanguage) }
    var thinkingSoundEnabled by rememberSaveable { mutableStateOf(settings.thinkingSoundEnabled) }

    var showWakeWordMenu by rememberSaveable { mutableStateOf(false) }
    var showLanguageMenu by rememberSaveable { mutableStateOf(false) }
    var showEngineMenu by rememberSaveable { mutableStateOf(false) }

    var ttsEngine by rememberSaveable { mutableStateOf(settings.ttsEngine) }
    var availableEngines by remember { mutableStateOf<List<com.openclaw.assistant.speech.TTSEngineUtils.EngineInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        availableEngines = com.openclaw.assistant.speech.TTSEngineUtils.getAvailableEngines(context)
    }

    // Wake word options
    val wakeWordOptions = listOf(
        SettingsRepository.WAKE_WORD_OPEN_CLAW to stringResource(R.string.wake_word_openclaw),
        SettingsRepository.WAKE_WORD_HEY_ASSISTANT to stringResource(R.string.wake_word_hey_assistant),
        SettingsRepository.WAKE_WORD_JARVIS to stringResource(R.string.wake_word_jarvis),
        SettingsRepository.WAKE_WORD_COMPUTER to stringResource(R.string.wake_word_computer),
        SettingsRepository.WAKE_WORD_CUSTOM to stringResource(R.string.wake_word_custom)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // === VOICE SECTION ===
        CollapsibleSection(title = stringResource(R.string.voice), initiallyExpanded = true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.read_ai_responses), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = ttsEnabled, onCheckedChange = {
                            ttsEnabled = it
                            settings.ttsEnabled = it
                        })
                    }

                    if (ttsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // TTS Engine Selection
                        ExposedDropdownMenuBox(
                            expanded = showEngineMenu,
                            onExpandedChange = { showEngineMenu = it }
                        ) {
                            val currentLabel = if (ttsEngine.isEmpty()) {
                                stringResource(R.string.tts_engine_auto)
                            } else {
                                availableEngines.find { it.name == ttsEngine }?.label ?: ttsEngine
                            }

                            OutlinedTextField(
                                value = currentLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.tts_engine_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEngineMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = showEngineMenu,
                                onDismissRequest = { showEngineMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tts_engine_auto)) },
                                    onClick = {
                                        ttsEngine = ""
                                        settings.ttsEngine = ""
                                        showEngineMenu = false
                                    }
                                )
                                availableEngines.forEach { engine ->
                                    DropdownMenuItem(
                                        text = { Text(engine.label) },
                                        onClick = {
                                            ttsEngine = engine.name
                                            settings.ttsEngine = engine.name
                                            showEngineMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.continuous_conversation), style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(checked = continuousMode, onCheckedChange = {
                            continuousMode = it
                            settings.continuousMode = it
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === WAKE WORD SECTION ===
        CollapsibleSection(title = stringResource(R.string.wake_word), initiallyExpanded = true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = showWakeWordMenu,
                        onExpandedChange = { showWakeWordMenu = it }
                    ) {
                        OutlinedTextField(
                            value = wakeWordOptions.find { it.first == wakeWordPreset }?.second ?: stringResource(R.string.wake_word_openclaw),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.activation_phrase)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showWakeWordMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showWakeWordMenu,
                            onDismissRequest = { showWakeWordMenu = false }
                        ) {
                            wakeWordOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        wakeWordPreset = value
                                        settings.wakeWordPreset = value
                                        showWakeWordMenu = false
                                        HotwordService.stop(context)
                                        if (settings.hotwordEnabled) HotwordService.start(context)
                                    }
                                )
                            }
                        }
                    }

                    if (wakeWordPreset == SettingsRepository.WAKE_WORD_CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customWakeWord,
                            onValueChange = {
                                customWakeWord = it.lowercase()
                                settings.customWakeWord = it.lowercase()
                            },
                            label = { Text(stringResource(R.string.custom_wake_word)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.resume_latest_session), style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(checked = resumeLatestSession, onCheckedChange = {
                            resumeLatestSession = it
                            settings.resumeLatestSession = it
                        })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === SUPPORT SECTION ===
        CollapsibleSection(title = stringResource(R.string.support_section), initiallyExpanded = false) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            val systemInfo = SystemInfoProvider.getSystemInfoReport(context, settings, runtime.serverVersion.value)
                            val uri = Uri.parse("https://github.com/yuga-hashimoto/openclaw-assistant/issues/new")
                                .buildUpon()
                                .appendQueryParameter("body", systemInfo)
                                .build()
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.report_issue))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val versionName = remember {
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull() ?: ""
                    }

                    Text(
                        text = stringResource(R.string.app_version, versionName),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
