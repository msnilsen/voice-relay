package com.openclaw.assistant.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openclaw.assistant.BuildConfig
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.TTSProviderType
import com.openclaw.assistant.speech.voicevox.VoiceVoxCharacters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    settings: SettingsRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val voiceVoxEnabled = BuildConfig.VOICEVOX_ENABLED
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("クレジット") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // VOICEVOX Credits (if used)
            if (voiceVoxEnabled && settings.ttsType == TTSProviderType.VOICEVOX) {
                VoiceVoxCreditsSection(settings)
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Open Source Licenses
            Text(
                "オープンソースライセンス",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "このアプリケーションは以下のオープンソースソフトウェアを使用しています：",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val licenses = listOf(
                "Android Jetpack" to "Apache License 2.0",
                "Compose" to "Apache License 2.0",
                "Kotlin" to "Apache License 2.0",
                "OkHttp" to "Apache License 2.0",
                "Gson" to "Apache License 2.0",
                "Vosk" to "Apache License 2.0",
                "Bouncy Castle" to "Bouncy Castle License"
            )
            
            licenses.forEach { (name, license) ->
                Text(
                    "• $name ($license)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (voiceVoxEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                val voiceVoxLicenses = listOf(
                    "VOICEVOX Core" to "MIT License",
                    "onnxruntime" to "MIT License",
                    "Open JTalk" to "Modified BSD License"
                )
                voiceVoxLicenses.forEach { (name, license) ->
                    Text(
                        "• $name ($license)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceVoxCreditsSection(settings: SettingsRepository) {
    val context = LocalContext.current
    
    Text(
        "VOICEVOX音声ライブラリ",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    
    Text(
        "このアプリケーションはVOICEVOXの音声ライブラリを使用しています。",
        style = MaterialTheme.typography.bodyMedium
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Currently selected character
    val character = VoiceVoxCharacters.getCharacterByStyleId(settings.voiceVoxStyleId)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "使用している音声",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            character?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    it.creditNotation,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    it.copyright,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Link to terms
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.termsUrl))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("利用規約を開く")
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // General VOICEVOX credit
    Text(
        "VOICEVOXについて",
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "VOICEVOXは商用・非商用問わず利用可能な日本語音声合成ソフトウェアです。" +
        "詳細な利用規約は各キャラクターの公式サイトをご確認ください。",
        style = MaterialTheme.typography.bodySmall
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    TextButton(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://voicevox.hiroshiba.jp/"))
            context.startActivity(intent)
        }
    ) {
        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text("VOICEVOX公式サイト")
    }
    
    TextButton(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://voicevox.hiroshiba.jp/term/"))
            context.startActivity(intent)
        }
    ) {
        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text("VOICEVOX利用規約")
    }
}
