package com.openclaw.assistant.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume

private const val TAG = "OpenAIProvider"
private const val API_URL = "https://api.openai.com/v1/audio/speech"

/**
 * OpenAI TTS Provider
 */
class OpenAIProvider(private val context: Context) : TTSProvider {
    
    private val settings = SettingsRepository.getInstance(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private var mediaPlayer: MediaPlayer? = null
    
    override suspend fun speak(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.e(TAG, "Not configured: ${getConfigurationError()}")
            return@withContext false
        }
        
        try {
            // Request audio from OpenAI API
            val audioData = synthesizeSpeech(text)
            if (audioData == null) {
                Log.e(TAG, "Failed to synthesize speech")
                return@withContext false
            }
            
            // Save to temp file and play
            val tempFile = File.createTempFile("openai_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }

            try {
                playAudioFile(tempFile)
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking: ${e.message}", e)
            false
        }
    }
    
    private suspend fun synthesizeSpeech(text: String): ByteArray? = withContext(Dispatchers.IO) {
        val apiKey = settings.openAiApiKey
        val voice = settings.openAiVoice
        val model = settings.openAiModel
        
        val requestBody = JSONObject().apply {
            put("model", model)
            put("input", text)
            put("voice", voice)
            put("response_format", "mp3")
            put("speed", settings.ttsSpeed.toDouble())
        }.toString()
        
        val request = Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "API error: ${response.code}, $errorBody")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            null
        }
    }
    
    private suspend fun playAudioFile(file: File): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    start()
                }
                setOnCompletionListener {
                    continuation.resume(true)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: $what, $extra")
                    continuation.resume(false)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}", e)
            continuation.resume(false)
        }
        
        continuation.invokeOnCancellation {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                mediaPlayer?.release()
            }
            mediaPlayer = null
        }
    }
    
    override fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping: ${e.message}")
        }
    }
    
    override fun shutdown() {
        stop()
    }
    
    override fun isAvailable(): Boolean = true // Always available if configured
    
    override fun getType(): String = TTSProviderType.OPENAI
    
    override fun getDisplayName(): String = "OpenAI"
    
    override fun isConfigured(): Boolean {
        return settings.openAiApiKey.isNotBlank()
    }
    
    override fun getConfigurationError(): String? {
        return if (settings.openAiApiKey.isBlank()) {
            context.getString(R.string.tts_error_openai_no_apikey)
        } else null
    }
    
    override fun speakWithProgress(text: String): Flow<TTSState> = flow {
        emit(TTSState.Preparing)
        
        if (!isConfigured()) {
            emit(TTSState.Error(getConfigurationError() ?: context.getString(R.string.tts_error_not_initialized)))
            return@flow
        }
        
        // Synthesize speech (API call)
        val audioData = try {
            synthesizeSpeech(text)
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis error", e)
            null
        }
        
        if (audioData == null) {
            emit(TTSState.Error("Failed to synthesize speech"))
            return@flow
        }
        
        // Save to temp file
        val tempFile = try {
            File.createTempFile("openai_", ".mp3", context.cacheDir).apply {
                FileOutputStream(this).use { it.write(audioData) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save audio", e)
            emit(TTSState.Error("Failed to save audio"))
            return@flow
        }
        
        // Play audio
        emit(TTSState.Speaking)
        val success = playAudioFile(tempFile)
        
        // Cleanup
        try {
            tempFile.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete temp file", e)
        }
        
        if (success) {
            emit(TTSState.Done)
        } else {
            emit(TTSState.Error("Failed to play audio"))
        }
    }
    
    /**
     * Available OpenAI voices
     */
    fun getAvailableVoices(): List<OpenAIVoice> {
        return listOf(
            OpenAIVoice("alloy", "Alloy"),
            OpenAIVoice("ash", "Ash"),
            OpenAIVoice("ballad", "Ballad"),
            OpenAIVoice("coral", "Coral"),
            OpenAIVoice("echo", "Echo"),
            OpenAIVoice("fable", "Fable"),
            OpenAIVoice("nova", "Nova"),
            OpenAIVoice("onyx", "Onyx"),
            OpenAIVoice("sage", "Sage"),
            OpenAIVoice("shimmer", "Shimmer"),
            OpenAIVoice("verse", "Verse"),
            OpenAIVoice("marin", "Marin"),
            OpenAIVoice("cedar", "Cedar")
        )
    }
    
    /**
     * Available OpenAI TTS models
     */
    fun getAvailableModels(): List<OpenAIModel> {
        return listOf(
            OpenAIModel("gpt-4o-mini-tts", "GPT-4o Mini TTS", context.getString(R.string.openai_model_desc_latest)),
            OpenAIModel("tts-1", "TTS-1", context.getString(R.string.openai_model_desc_low_latency)),
            OpenAIModel("tts-1-hd", "TTS-1 HD", context.getString(R.string.openai_model_desc_hd))
        )
    }
    
    data class OpenAIVoice(
        val id: String,
        val displayName: String
    )
    
    data class OpenAIModel(
        val id: String,
        val displayName: String,
        val description: String
    )
}
