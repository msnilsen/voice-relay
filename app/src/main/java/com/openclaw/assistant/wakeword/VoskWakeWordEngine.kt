package com.openclaw.assistant.wakeword

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.data.SettingsRepository
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener as VoskRecognitionListener
import org.vosk.android.SpeechService

class VoskWakeWordEngine(
    private val context: Context,
    private val settings: SettingsRepository
) : WakeWordEngine, VoskRecognitionListener {

    companion object {
        private const val TAG = "VoskWakeWordEngine"
        private const val SAMPLE_RATE = 16000.0f
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var onDetectedCallback: (() -> Unit)? = null
    private var audioRetryCount = 0
    private val MAX_AUDIO_RETRIES = 5
    private var retryJob: Job? = null

    override fun isAvailable(): Boolean {
        val prefs = context.getSharedPreferences("hotword_prefs", Context.MODE_PRIVATE)
        return !prefs.getBoolean("vosk_unsupported", false)
    }

    override fun start(onDetected: () -> Unit) {
        onDetectedCallback = onDetected
        if (model != null) {
            startListening()
            return
        }
        initModel()
    }

    override fun stop() {
        retryJob?.cancel()
        speechService?.let {
            try { it.stop(); it.shutdown() } catch (_: Exception) {}
        }
        speechService = null
    }

    override fun release() {
        stop()
        scope.cancel()
        onDetectedCallback = null
    }

    private fun initModel() {
        val prefs = context.getSharedPreferences("hotword_prefs", Context.MODE_PRIVATE)
        val currentVersion = getAppVersionCode()
        val unsupportedSinceVersion = prefs.getInt("vosk_unsupported_version", 0)
        if (prefs.getBoolean("vosk_unsupported", false)) {
            if (unsupportedSinceVersion < currentVersion) {
                prefs.edit().remove("vosk_unsupported").remove("vosk_unsupported_version").apply()
            } else {
                Log.w(TAG, "Vosk is unsupported on this device.")
                return
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                val modelPath = copyAssets()
                if (modelPath != null) {
                    model = Model(modelPath)
                    withContext(Dispatchers.Main) { startListening() }
                }
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Vosk native library not supported", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                prefs.edit()
                    .putBoolean("vosk_unsupported", true)
                    .putInt("vosk_unsupported_version", currentVersion)
                    .apply()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Init error", e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun startListening() {
        if (model == null) return

        speechService?.let {
            try { it.stop(); it.shutdown() } catch (_: Exception) {}
            speechService = null
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            scheduleRetry(); return
        }
        val testRecord = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        } catch (_: Exception) { null }

        if (testRecord == null || testRecord.state != AudioRecord.STATE_INITIALIZED) {
            testRecord?.release(); scheduleRetry(); return
        }
        testRecord.release()
        audioRetryCount = 0

        try {
            val wakeWords = settings.getWakeWords()
            val wakeWordsJson = wakeWords.joinToString("\", \"", "[\"", "\"]")
            val rec = Recognizer(model, SAMPLE_RATE, wakeWordsJson)
            speechService = SpeechService(rec, SAMPLE_RATE)
            speechService?.startListening(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            speechService = null
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        if (audioRetryCount >= MAX_AUDIO_RETRIES) { audioRetryCount = 0; return }
        audioRetryCount++
        val delayMs = (2000L * audioRetryCount).coerceAtMost(10000L)
        retryJob?.cancel()
        retryJob = scope.launch { delay(delayMs); startListening() }
    }

    fun resumeListening() {
        audioRetryCount = 0
        scope.launch { delay(500); if (speechService == null) startListening() }
    }

    override fun onPartialResult(hypothesis: String?) {}

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            try {
                val json = JSONObject(it)
                val text = json.optString("text", "")
                val wakeWords = settings.getWakeWords()
                if (wakeWords.any { w -> text.contains(w) }) {
                    Log.d(TAG, "Wake word detected: $text")
                    onDetectedCallback?.invoke()
                } else Unit
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse result: $it", e)
            }
        }
    }

    override fun onFinalResult(hypothesis: String?) { onResult(hypothesis) }

    override fun onError(exception: Exception?) {
        Log.e(TAG, "Vosk error: ${exception?.message}")
        scope.launch { delay(3000); resumeListening() }
    }

    override fun onTimeout() {
        speechService?.startListening(this)
    }

    private fun getAppVersionCode(): Int = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }
    } catch (_: Exception) { 1 }

    private fun copyAssets(): String? {
        val targetDir = java.io.File(context.filesDir, "model")
        val currentVersion = getAppVersionCode()
        val prefs = context.getSharedPreferences("hotword_prefs", Context.MODE_PRIVATE)
        val savedVersion = prefs.getInt("model_version", 0)

        if (savedVersion == currentVersion && targetDir.exists() && targetDir.list()?.isNotEmpty() == true) {
            return targetDir.absolutePath
        }

        try {
            if (targetDir.exists()) targetDir.deleteRecursively()
            val success = copyAssetFolder(context.assets, "model", targetDir.absolutePath)
            if (success) {
                prefs.edit().putInt("model_version", currentVersion).apply()
                return targetDir.absolutePath
            }
            return null
        } catch (_: Exception) { return null }
    }

    private fun copyAssetFolder(am: android.content.res.AssetManager, from: String, to: String): Boolean {
        try {
            val files = am.list(from) ?: return false
            java.io.File(to).mkdirs()
            var res = true
            for (f in files) {
                res = if (f.contains(".")) res and copyAsset(am, "$from/$f", "$to/$f")
                       else res and copyAssetFolder(am, "$from/$f", "$to/$f")
            }
            return res
        } catch (_: Exception) { return false }
    }

    private fun copyAsset(am: android.content.res.AssetManager, from: String, to: String): Boolean {
        return try {
            am.open(from).use { inp ->
                java.io.File(to).outputStream().use { out -> inp.copyTo(out) }
            }
            true
        } catch (_: Exception) { false }
    }
}
