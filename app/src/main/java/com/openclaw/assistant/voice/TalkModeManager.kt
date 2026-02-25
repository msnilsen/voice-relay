package com.openclaw.assistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.SpeechRecognizerManager
import com.openclaw.assistant.speech.SpeechResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import okhttp3.*
import okio.ByteString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages full-duplex voice conversations.
 * Handles streaming ASR and TTS (ElevenLabs).
 */
class TalkModeManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSendMessage: (String) -> Unit = {}
) {
    private val TAG = "TalkModeManager"
    private val settings = SettingsRepository.getInstance(context)
    private val speechManager = SpeechRecognizerManager(context)
    private var mediaPlayer: MediaPlayer? = null
    private var mediaDataSource: StreamingMediaDataSource? = null

    private val _state = MutableStateFlow(TalkModeState.IDLE)
    val state: StateFlow<TalkModeState> = _state.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var listeningJob: Job? = null
    private var elevenLabsSocket: WebSocket? = null
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // For streaming
        .build()

    fun start() {
        if (_state.value != TalkModeState.IDLE) return
        startListening()
    }

    fun stop() {
        listeningJob?.cancel()
        listeningJob = null
        stopPlayback()
        closeElevenLabs()
        _state.value = TalkModeState.IDLE
    }

    private fun startListening() {
        listeningJob?.cancel()
        listeningJob = scope.launch {
            _state.value = TalkModeState.LISTENING
            speechManager.startListening(settings.speechLanguage.ifEmpty { null }, settings.speechSilenceTimeout)
                .collectLatest { result ->
                    when (result) {
                        is SpeechResult.PartialResult -> {
                            _partialTranscript.value = result.text
                            if (mediaPlayer?.isPlaying == true) {
                                // Interruption / Talk-over
                                stopPlayback()
                            }
                        }
                        is SpeechResult.Result -> {
                            _partialTranscript.value = ""
                            sendToAssistant(result.text)
                        }
                        is SpeechResult.RmsChanged -> {
                            _audioLevel.value = (result.rmsdB + 2f) / 12f // Normalized 0..1
                        }
                        is SpeechResult.Error -> {
                            Log.e(TAG, "ASR Error: ${result.message}")
                            _state.value = TalkModeState.ERROR
                            delay(2000)
                            if (isActive) startListening()
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun sendToAssistant(text: String) {
        _state.value = TalkModeState.THINKING
        onSendMessage(text)
    }

    fun handleAssistantDelta(text: String, isFinal: Boolean) {
        if (settings.elevenLabsEnabled && text.isNotEmpty()) {
            // Parse and strip directives before sending to TTS
            val (cleanText, directives) = TalkDirectiveParser.parse(text)

            // Apply directives if needed (e.g. voice change)
            // For now we just send the clean text

            if (cleanText.isNotEmpty()) {
                if (elevenLabsSocket == null) {
                    startElevenLabsStreaming()
                }
                sendToElevenLabs(cleanText)
            }
        }
        if (isFinal) {
            markElevenLabsFinished()
        }
    }

    private fun startElevenLabsStreaming() {
        val voiceId = settings.elevenLabsVoiceId.ifEmpty { "21m00Tcm4TlvDq8ikWAM" } // Default: Rachel
        val modelId = settings.elevenLabsModelId.ifEmpty { "eleven_monolingual_v1" }
        val apiKey = settings.elevenLabsApiKey

        if (apiKey.isEmpty()) {
            Log.e(TAG, "ElevenLabs API Key is missing")
            return
        }

        val url = "wss://api.elevenlabs.io/v1/text-to-speech/$voiceId/stream-input?model_id=$modelId"
        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", apiKey)
            .build()

        mediaDataSource = StreamingMediaDataSource()
        setupMediaPlayer()

        elevenLabsSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "ElevenLabs WebSocket opened")
                // Send initial generation settings
                val bos = JSONObject()
                bos.put("text", " ")
                bos.put("voice_settings", JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.8)
                })
                webSocket.send(bos.toString())
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Audio data received
                mediaDataSource?.pushData(bytes.toByteArray())
                if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                    scope.launch(Dispatchers.Main) {
                        try {
                            mediaPlayer?.prepareAsync()
                        } catch (e: Exception) {
                            // Already prepared or preparing
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.has("audio")) {
                    val audioBase64 = json.getString("audio")
                    val audioBytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
                    mediaDataSource?.pushData(audioBytes)

                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        scope.launch(Dispatchers.Main) {
                            try {
                                mediaPlayer?.prepareAsync()
                            } catch (e: Exception) {
                                // Already prepared or preparing
                            }
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "ElevenLabs WebSocket failure", t)
                closeElevenLabs()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                closeElevenLabs()
            }
        })
    }

    private fun sendToElevenLabs(text: String) {
        val payload = JSONObject()
        payload.put("text", text)
        payload.put("try_trigger_generation", true)
        elevenLabsSocket?.send(payload.toString())
    }

    private fun markElevenLabsFinished() {
        val eos = JSONObject()
        eos.put("text", "")
        elevenLabsSocket?.send(eos.toString())
        // Socket will be closed after all audio is received
    }

    private fun setupMediaPlayer() {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .build()
            )
            setDataSource(mediaDataSource)
            setOnPreparedListener {
                it.start()
                _state.value = TalkModeState.SPEAKING
            }
            setOnCompletionListener {
                _state.value = TalkModeState.IDLE
                startListening()
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer Error: $what, $extra")
                _state.value = TalkModeState.ERROR
                false
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaDataSource?.close()
        mediaDataSource = null
    }

    private fun closeElevenLabs() {
        elevenLabsSocket?.close(1000, "Normal closure")
        elevenLabsSocket = null
    }
}

enum class TalkModeState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}
