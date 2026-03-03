package com.openclaw.assistant.wakeword

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.openclaw.assistant.data.SettingsRepository
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class OpenWakeWordEngine(
    private val context: Context,
    private val settings: SettingsRepository
) : WakeWordEngine {

    companion object {
        private const val TAG = "OpenWakeWordEngine"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_SAMPLES = 1280 // 80ms
        private const val MEL_CONTEXT_SAMPLES = 480 // 160 * 3 overlap for proper windowing
        private const val MEL_BINS = 32
        private const val MEL_WINDOW_FRAMES = 76
        private const val EMBEDDING_DIM = 96
        private const val FEATURE_WINDOW = 16 // wake word model input frames
        private const val FEATURE_BUFFER_MAX = 120 // ~10 seconds of history
        private const val MEL_BUFFER_MAX = 970 // ~10 seconds of mel frames
        private const val DETECTION_DEBOUNCE_MS = 2000L
        private const val SKIP_INITIAL_PREDICTIONS = 5
        private const val RAW_BUFFER_SECONDS = 10

        fun getModelForPreset(preset: String): String = when (preset) {
            SettingsRepository.WAKE_WORD_HEY_JARVIS -> "openwakeword/hey_jarvis_v0.1.onnx"
            SettingsRepository.WAKE_WORD_ALEXA -> "openwakeword/alexa_v0.1.onnx"
            SettingsRepository.WAKE_WORD_HEY_MYCROFT -> "openwakeword/hey_mycroft_v0.1.onnx"
            else -> "openwakeword/hey_jarvis_v0.1.onnx"
        }
    }

    private var ortEnv: OrtEnvironment? = null
    private var melSpecSession: OrtSession? = null
    private var embeddingSession: OrtSession? = null
    private var wakeWordSession: OrtSession? = null
    private var wakeWordInputName: String? = null

    private var audioRecord: AudioRecord? = null
    private var processingThread: Thread? = null
    @Volatile private var isRunning = false

    private var onDetectedCallback: (() -> Unit)? = null
    private var lastDetectionTime = 0L
    private var predictionCount = 0

    // Ring buffer for raw audio (int16 as float)
    private lateinit var rawBuffer: FloatArray
    private var rawWritePos = 0
    private var rawTotalWritten = 0L

    // Mel spectrogram frame buffer: each entry is MEL_BINS floats
    private val melBuffer = ArrayList<FloatArray>()

    // Embedding feature buffer: each entry is EMBEDDING_DIM floats
    private val featureBuffer = ArrayList<FloatArray>()

    override fun isAvailable(): Boolean = true

    override fun start(onDetected: () -> Unit) {
        if (isRunning) return
        onDetectedCallback = onDetected
        isRunning = true

        processingThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            try {
                initModels()
                initBuffers()
                if (!initAudioRecord()) {
                    Log.e(TAG, "Failed to initialize AudioRecord")
                    return@Thread
                }
                audioLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Error in wake word processing thread", e)
            } finally {
                releaseAudioRecord()
            }
        }, "OpenWakeWord")
        processingThread?.start()
    }

    override fun stop() {
        isRunning = false
        processingThread?.join(3000)
        processingThread = null
        releaseAudioRecord()
    }

    override fun release() {
        stop()
        try {
            wakeWordSession?.close()
            embeddingSession?.close()
            melSpecSession?.close()
            ortEnv?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing ONNX sessions", e)
        }
        wakeWordSession = null
        embeddingSession = null
        melSpecSession = null
        ortEnv = null
        onDetectedCallback = null
    }

    private fun initModels() {
        ortEnv = OrtEnvironment.getEnvironment()
        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(1)
            setInterOpNumThreads(1)
        }

        melSpecSession = ortEnv!!.createSession(loadAsset("openwakeword/melspectrogram.onnx"), opts)
        embeddingSession = ortEnv!!.createSession(loadAsset("openwakeword/embedding_model.onnx"), opts)

        val modelAsset = getModelForPreset(settings.wakeWordPreset)
        wakeWordSession = ortEnv!!.createSession(loadAsset(modelAsset), opts)
        wakeWordInputName = wakeWordSession!!.inputNames.first()

        Log.d(TAG, "Models loaded. Wake word model: $modelAsset, input: $wakeWordInputName")
    }

    private fun loadAsset(name: String): ByteArray {
        return context.assets.open(name).use { it.readBytes() }
    }

    private fun initBuffers() {
        rawBuffer = FloatArray(SAMPLE_RATE * RAW_BUFFER_SECONDS)
        rawWritePos = 0
        rawTotalWritten = 0L

        melBuffer.clear()
        for (i in 0 until MEL_WINDOW_FRAMES) {
            melBuffer.add(FloatArray(MEL_BINS) { 1.0f })
        }

        featureBuffer.clear()
        for (i in 0 until FEATURE_WINDOW) {
            featureBuffer.add(FloatArray(EMBEDDING_DIM))
        }

        predictionCount = 0
        lastDetectionTime = 0L
    }

    private fun initAudioRecord(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return false

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) return false

        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize.coerceAtLeast(FRAME_SAMPLES * 4)
            )
            audioRecord?.state == AudioRecord.STATE_INITIALIZED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            false
        }
    }

    private fun releaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    private fun audioLoop() {
        audioRecord?.startRecording() ?: return
        val frame = ShortArray(FRAME_SAMPLES)
        val threshold = settings.wakeWordThreshold

        while (isRunning) {
            val read = audioRecord?.read(frame, 0, FRAME_SAMPLES) ?: break
            if (read != FRAME_SAMPLES) continue

            addToRawBuffer(frame)

            // Need enough raw data for mel context
            if (rawTotalWritten < FRAME_SAMPLES + MEL_CONTEXT_SAMPLES) continue

            // 1. Compute mel spectrogram from latest audio + context
            val melInputSize = FRAME_SAMPLES + MEL_CONTEXT_SAMPLES
            val audioSlice = getLastNSamples(melInputSize)
            val newMelFrames = computeMelSpectrogram(audioSlice) ?: continue

            // 2. Append new mel frames
            for (melFrame in newMelFrames) {
                melBuffer.add(melFrame)
            }
            while (melBuffer.size > MEL_BUFFER_MAX) {
                melBuffer.removeAt(0)
            }

            // 3. Compute embedding from last 76 mel frames
            if (melBuffer.size >= MEL_WINDOW_FRAMES) {
                val melWindow = ArrayList<FloatArray>(MEL_WINDOW_FRAMES)
                val start = melBuffer.size - MEL_WINDOW_FRAMES
                for (i in start until melBuffer.size) {
                    melWindow.add(melBuffer[i])
                }

                val embedding = computeEmbedding(melWindow) ?: continue
                featureBuffer.add(embedding)
                while (featureBuffer.size > FEATURE_BUFFER_MAX) {
                    featureBuffer.removeAt(0)
                }
            }

            // 4. Run wake word detection
            if (featureBuffer.size >= FEATURE_WINDOW) {
                predictionCount++
                if (predictionCount <= SKIP_INITIAL_PREDICTIONS) continue

                val features = ArrayList<FloatArray>(FEATURE_WINDOW)
                val fStart = featureBuffer.size - FEATURE_WINDOW
                for (i in fStart until featureBuffer.size) {
                    features.add(featureBuffer[i])
                }

                val score = runWakeWordModel(features)

                if (score >= threshold) {
                    val now = System.currentTimeMillis()
                    if (now - lastDetectionTime > DETECTION_DEBOUNCE_MS) {
                        lastDetectionTime = now
                        Log.d(TAG, "Wake word detected! score=$score threshold=$threshold")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onDetectedCallback?.invoke()
                        }
                    }
                }
            }
        }
    }

    private fun addToRawBuffer(frame: ShortArray) {
        for (sample in frame) {
            rawBuffer[rawWritePos] = sample.toFloat()
            rawWritePos = (rawWritePos + 1) % rawBuffer.size
        }
        rawTotalWritten += frame.size
    }

    private fun getLastNSamples(n: Int): FloatArray {
        val result = FloatArray(n)
        val available = minOf(n, rawBuffer.size, rawTotalWritten.toInt())
        var readPos = (rawWritePos - available + rawBuffer.size) % rawBuffer.size
        for (i in 0 until available) {
            result[n - available + i] = rawBuffer[readPos]
            readPos = (readPos + 1) % rawBuffer.size
        }
        return result
    }

    private fun computeMelSpectrogram(audio: FloatArray): List<FloatArray>? {
        val env = ortEnv ?: return null
        val session = melSpecSession ?: return null

        return try {
            val inputTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(audio), longArrayOf(1, audio.size.toLong())
            )
            inputTensor.use { input ->
                session.run(mapOf("input" to input)).use { result ->
                    val outputTensor = result[0] as OnnxTensor
                    val shape = outputTensor.info.shape // [time, 1, clip_dim, 32]
                    val flatData = outputTensor.floatBuffer
                    val numFrames = shape[0].toInt()

                    val frames = ArrayList<FloatArray>(numFrames)
                    for (f in 0 until numFrames) {
                        val melFrame = FloatArray(MEL_BINS)
                        // The output has shape [time, 1, clip_dim, 32].
                        // Total elements per frame = 1 * clip_dim * 32.
                        // We want the last 32 values of each frame.
                        val elementsPerFrame = (shape[1] * shape[2] * shape[3]).toInt()
                        val frameOffset = f * elementsPerFrame
                        // Read the last 32 values (the mel bins)
                        val melStart = frameOffset + elementsPerFrame - MEL_BINS
                        for (b in 0 until MEL_BINS) {
                            melFrame[b] = flatData.get(melStart + b) / 10.0f + 2.0f
                        }
                        frames.add(melFrame)
                    }
                    frames
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mel spectrogram error", e)
            null
        }
    }

    private fun computeEmbedding(melWindow: List<FloatArray>): FloatArray? {
        val env = ortEnv ?: return null
        val session = embeddingSession ?: return null

        return try {
            // Input: [1, 76, 32, 1]
            val flatData = FloatArray(MEL_WINDOW_FRAMES * MEL_BINS)
            for (i in 0 until MEL_WINDOW_FRAMES) {
                System.arraycopy(melWindow[i], 0, flatData, i * MEL_BINS, MEL_BINS)
            }

            val inputTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(flatData),
                longArrayOf(1, MEL_WINDOW_FRAMES.toLong(), MEL_BINS.toLong(), 1)
            )
            inputTensor.use { input ->
                session.run(mapOf("input_1" to input)).use { result ->
                    val outputTensor = result[0] as OnnxTensor
                    // Output: [1, 1, 1, 96] -> extract 96 floats
                    val buffer = outputTensor.floatBuffer
                    FloatArray(EMBEDDING_DIM) { buffer.get(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Embedding error", e)
            null
        }
    }

    private fun runWakeWordModel(features: List<FloatArray>): Float {
        val env = ortEnv ?: return 0f
        val session = wakeWordSession ?: return 0f
        val inputName = wakeWordInputName ?: return 0f

        return try {
            // Input: [1, 16, 96]
            val flatData = FloatArray(FEATURE_WINDOW * EMBEDDING_DIM)
            for (i in 0 until FEATURE_WINDOW) {
                System.arraycopy(features[i], 0, flatData, i * EMBEDDING_DIM, EMBEDDING_DIM)
            }

            val inputTensor = OnnxTensor.createTensor(
                env, FloatBuffer.wrap(flatData),
                longArrayOf(1, FEATURE_WINDOW.toLong(), EMBEDDING_DIM.toLong())
            )
            inputTensor.use { input ->
                session.run(mapOf(inputName to input)).use { result ->
                    val outputTensor = result[0] as OnnxTensor
                    outputTensor.floatBuffer.get(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wake word model error", e)
            0f
        }
    }
}
