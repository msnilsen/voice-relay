package com.openclaw.assistant.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Assistant entry point for Wear OS.
 *
 * This Activity's ASSIST intent-filter is the critical fix for Issue #58:
 * Wear OS enumerates Activities (not Services) when building the assistant
 * picker list. By declaring this Activity with android.intent.action.ASSIST,
 * OpenClaw appears in Settings > Apps > Default Apps > Assist app.
 *
 * Flow: ASSIST intent → listen → API call → TTS → finish()
 */
class WearAssistActivity : ComponentActivity() {

    private val settings by lazy { WearSettingsRepository.getInstance(this) }
    private val apiClient = WearApiClient()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var uiState by mutableStateOf(UiState.CHECKING)
    private var displayText by mutableStateOf("")
    private var userQuery by mutableStateOf("")
    private var errorMessage by mutableStateOf<String?>(null)

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else showError(getString(R.string.error_speech))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    ScalingLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(stateColor(uiState))
                                    .clickable {
                                        when (uiState) {
                                            UiState.ERROR -> startListening()
                                            else -> {}
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (uiState) {
                                    UiState.PROCESSING -> CircularProgressIndicator(
                                        indicatorColor = Color.White,
                                        trackColor = Color.Transparent,
                                        strokeWidth = 3.dp
                                    )
                                    UiState.NOT_CONFIGURED, UiState.ERROR -> Icon(
                                        imageVector = Icons.Default.MicOff,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        if (uiState == UiState.NOT_CONFIGURED) {
                            item {
                                Text(
                                    text = getString(R.string.error_config_required),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White
                                )
                            }
                            item {
                                Button(
                                    onClick = {
                                        startActivity(Intent(this@WearAssistActivity, WearMainActivity::class.java))
                                        finish()
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = getString(R.string.action_open_settings),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (userQuery.isNotBlank()) {
                            item {
                                Text(
                                    text = userQuery,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.button,
                                    color = Color.LightGray
                                )
                            }
                        }
                        if (displayText.isNotBlank() && uiState != UiState.LISTENING) {
                            item {
                                Text(
                                    text = displayText,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        }
                        if (errorMessage != null) {
                            item {
                                Text(
                                    text = errorMessage!!,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.caption2,
                                    color = Color.Red
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }

        if (!settings.isConfigured()) {
            uiState = UiState.NOT_CONFIGURED
            return
        }

        initTts()
        checkPermissionAndListen()
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady && settings.speechLanguage.isNotBlank()) {
                tts?.language = Locale.forLanguageTag(settings.speechLanguage)
            }
            tts?.setSpeechRate(settings.ttsSpeed)
        }
    }

    private fun checkPermissionAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showError(getString(R.string.error_speech))
            return
        }
        uiState = UiState.LISTENING
        errorMessage = null
        displayText = ""
        userQuery = ""

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    if (text.isBlank()) {
                        showError(getString(R.string.error_no_response))
                    } else {
                        userQuery = text
                        callApi(text)
                    }
                }
                override fun onError(error: Int) {
                    Log.w(TAG, "Speech error: $error")
                    showError("${getString(R.string.error_speech)} ($error)")
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { uiState = UiState.PROCESSING }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let { userQuery = it }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                if (settings.speechLanguage.isNotBlank()) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.speechLanguage)
                }
            }
            startListening(intent)
        }
    }

    private fun callApi(text: String) {
        uiState = UiState.PROCESSING
        lifecycleScope.launch {
            val result = apiClient.sendMessage(
                chatUrl = settings.getChatUrl(),
                message = text,
                authToken = settings.authToken.takeIf { it.isNotBlank() }
            )
            result.onSuccess { response ->
                displayText = response
                speak(response)
            }.onFailure { e ->
                Log.e(TAG, "API error", e)
                showError(e.message ?: getString(R.string.error_network))
            }
        }
    }

    private fun speak(text: String) {
        if (!settings.ttsEnabled || !ttsReady) {
            finish()
            return
        }
        uiState = UiState.SPEAKING
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { runOnUiThread { finish() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { runOnUiThread { finish() } }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assist_utterance")
    }

    private fun showError(message: String) {
        uiState = UiState.ERROR
        errorMessage = message
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
    }

    private fun stateColor(state: UiState): Color = when (state) {
        UiState.LISTENING -> Color(0xFF4CAF50)
        UiState.PROCESSING -> Color(0xFFFFC107)
        UiState.SPEAKING -> Color(0xFF2196F3)
        UiState.ERROR, UiState.NOT_CONFIGURED -> Color(0xFFF44336)
        UiState.CHECKING -> Color(0xFF9E9E9E)
    }

    private enum class UiState {
        CHECKING, NOT_CONFIGURED, LISTENING, PROCESSING, SPEAKING, ERROR
    }

    companion object {
        private const val TAG = "WearAssistActivity"
    }
}
