package com.openclaw.assistant.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.openclaw.assistant.R
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.voicevox.VoiceVoxCharacters
import jp.hiroshiba.voicevoxcore.blocking.Onnxruntime
import jp.hiroshiba.voicevoxcore.blocking.OpenJtalk
import jp.hiroshiba.voicevoxcore.blocking.Synthesizer
import jp.hiroshiba.voicevoxcore.blocking.VoiceModelFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

private const val TAG = "VoiceVoxProvider"

/**
 * VOICEVOX TTS Provider (full flavor only)
 * Uses voicevox_core AAR for local synthesis
 */
class VoiceVoxProvider(private val context: Context) : TTSProvider {
    
    private val settings = SettingsRepository.getInstance(context)
    private val modelManager = VoiceVoxModelManager(context)
    
    private var synthesizer: Synthesizer? = null
    private var openJtalk: OpenJtalk? = null
    private var currentModel: VoiceModelFile? = null
    private var currentVvmFile: String? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private var isInitialized = false
    private var initializationError: String? = null
    
    /**
     * Initialize VOICEVOX (call this after dictionary is copied)
     */
    fun initialize(): Boolean {
        if (isInitialized) return true
        
        try {
            // Check if dictionary exists
            if (!modelManager.isDictionaryReady()) {
                val dictPath = File(context.filesDir, VoiceVoxModelManager.DICT_DIR)
                Log.e(TAG, "initialize: dictionary not ready at $dictPath")
                initializationError = context.getString(R.string.tts_error_voicevox_dict_missing)
                return false
            }
            
            // Load VoiceVox's custom ONNX Runtime (libvoicevox_onnxruntime.so).
            // The standard Microsoft ORT does not support the "vv-bin" model format used by VoiceVox.
            Log.d(TAG, "Loading ORT: libvoicevox_onnxruntime.so")
            val onnxruntime = Onnxruntime.loadOnce().perform()
            
            // Initialize OpenJtalk
            val dictPath = File(context.filesDir, VoiceVoxModelManager.DICT_DIR).absolutePath
            openJtalk = OpenJtalk(dictPath)
            
            // Initialize Synthesizer
            synthesizer = Synthesizer.builder(onnxruntime, openJtalk).build()
            
            isInitialized = true
            Log.d(TAG, "VOICEVOX initialized successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VOICEVOX: ${e.message}", e)
            initializationError = e.message
            isInitialized = false
            return false
        }
    }
    
    /**
     * Check if dictionary needs to be copied
     */
    fun needsDictionarySetup(): Boolean {
        return !modelManager.isDictionaryReady()
    }
    
    /**
     * Copy dictionary from assets
     */
    suspend fun setupDictionary(): Flow<VoiceVoxModelManager.CopyProgress> {
        return modelManager.copyDictionaryFromAssets()
    }
    
    /**
     * Check if VVM model needs to be downloaded
     */
    fun needsVvmModelDownload(styleId: Int): Boolean {
        val vvmFileName = getVvmFileNameForStyle(styleId)
        return !modelManager.isVvmModelReady(vvmFileName)
    }
    
    /**
     * Download VVM model
     */
    suspend fun downloadVvmModel(styleId: Int): Flow<VoiceVoxModelManager.DownloadProgress> {
        val vvmFileName = getVvmFileNameForStyle(styleId)
        return modelManager.downloadVvmModel(vvmFileName)
    }
    
    /**
     * Get VVM file size for display
     */
    fun getVvmModelSize(styleId: Int): String {
        val vvmFileName = getVvmFileNameForStyle(styleId)
        return modelManager.getVvmFileSizeMB(vvmFileName)
    }
    
    /**
     * VVM file name for a given style ID.
     * Delegates to VoiceVoxCharacterData (single source of truth) so that
     * this mapping stays in sync with credit display and SettingsActivity.
     */
    private fun getVvmFileNameForStyle(styleId: Int): String {
        return VoiceVoxCharacters.getCharacterByStyleId(styleId)?.vvmFile ?: "0"
    }
    
    private fun loadVoiceModel(styleId: Int): Boolean {
        if (!isInitialized) return false
        
        val vvmFileName = getVvmFileNameForStyle(styleId)
        val vvmFile = File(context.filesDir, "$vvmFileName.vvm")
        
        if (!vvmFile.exists()) {
            Log.e(TAG, "VVM file not found: ${vvmFile.absolutePath}")
            return false
        }
        
        try {
            // Load model only if it's different from currently loaded one
            if (currentModel == null || vvmFileName != currentVvmFile) {
                Log.d(TAG, "Loading VVM model: $vvmFileName.vvm for style $styleId")
                currentModel?.close()
                currentModel = VoiceModelFile(vvmFile.absolutePath)
                synthesizer?.loadVoiceModel(currentModel!!)
                currentVvmFile = vvmFileName
                Log.d(TAG, "VVM model loaded successfully")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load voice model: ${e.message}", e)
            currentVvmFile = null
            return false
        }
    }
    
    override suspend fun speak(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized: $initializationError")
            return@withContext false
        }
        
        if (!settings.voiceVoxTermsAccepted) {
            Log.e(TAG, "VOICEVOX terms not accepted")
            return@withContext false
        }
        
        try {
            val styleId = settings.voiceVoxStyleId
            Log.d(TAG, "Speaking with styleId: $styleId")
            
            if (!loadVoiceModel(styleId)) {
                Log.e(TAG, "Failed to load voice model")
                return@withContext false
            }
            
            val synthesizer = this@VoiceVoxProvider.synthesizer ?: return@withContext false

            // Get audio query and adjust speed
            val audioQuery = synthesizer.createAudioQuery(text, styleId)
            audioQuery.speedScale = settings.ttsSpeed.toDouble()
            
            // Synthesize with modified query
            val wavData = synthesizer.synthesis(audioQuery, styleId).perform()
            
            playWav(wavData)
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking: ${e.message}", e)
            false
        }
    }
    
    private suspend fun playWav(wavData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("voicevox_", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(wavData) }
            
            suspendCancellableCoroutine { continuation ->
                try {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        setOnPreparedListener { start() }
                        setOnCompletionListener {
                            continuation.resume(true)
                        }
                        setOnErrorListener { _, _, _ ->
                            continuation.resume(false)
                            true
                        }
                        prepare()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing audio: ${e.message}")
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
        } catch (e: Exception) {
            Log.e(TAG, "Error playing WAV: ${e.message}", e)
            false
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
        try {
            currentModel?.close()
            currentModel = null
            currentVvmFile = null
            openJtalk = null
            synthesizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down: ${e.message}")
        }
        isInitialized = false
    }
    
    override fun isAvailable(): Boolean = isInitialized
    
    override fun getType(): String = TTSProviderType.VOICEVOX
    
    override fun getDisplayName(): String = "VOICEVOX"
    
    override fun isConfigured(): Boolean {
        return isInitialized && settings.voiceVoxTermsAccepted
    }
    
    override fun getConfigurationError(): String? {
        return when {
            !modelManager.isDictionaryReady() -> context.getString(R.string.tts_error_voicevox_dict_required)
            !isInitialized -> initializationError ?: context.getString(R.string.tts_error_voicevox_not_initialized)
            !settings.voiceVoxTermsAccepted -> context.getString(R.string.tts_error_voicevox_terms_required)
            else -> null
        }
    }
    
    override fun speakWithProgress(text: String): Flow<TTSState> = flow {
        emit(TTSState.Preparing)
        
        if (!isConfigured()) {
            emit(TTSState.Error(getConfigurationError() ?: "Not configured"))
            return@flow
        }
        
        emit(TTSState.Speaking)
        
        val success = speak(text)
        if (success) {
            emit(TTSState.Done)
        } else {
            emit(TTSState.Error("Failed to synthesize or play"))
        }
    }
    
    fun getAvailableCharacters(): List<com.openclaw.assistant.speech.voicevox.VoiceVoxCharacter> {
        return VoiceVoxCharacters.getAllCharacters()
    }
}
