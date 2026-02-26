package com.openclaw.assistant.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "VoiceVoxModelManager"

/**
 * Manages VOICEVOX model downloads and OpenJTalk dictionary
 */
class VoiceVoxModelManager(private val context: Context) {
    
    companion object {
        const val DICT_DIR = "open_jtalk_dic_utf_8-1.11"
        
        // VVM model download URLs (GitHub releases - official)
        private const val BASE_URL = "https://github.com/VOICEVOX/voicevox_vvm/releases/download/0.16.3"
        
        private val VVM_FILES = mapOf(
            "0" to "$BASE_URL/0.vvm",
            "1" to "$BASE_URL/1.vvm",
            "2" to "$BASE_URL/2.vvm",
            "3" to "$BASE_URL/3.vvm",
            "4" to "$BASE_URL/4.vvm",
            "5" to "$BASE_URL/5.vvm"
        )
    }
    
    /**
     * Check if OpenJTalk dictionary is ready
     */
    fun isDictionaryReady(): Boolean {
        val dictDir = File(context.filesDir, DICT_DIR)
        return dictDir.exists() && File(dictDir, "sys.dic").exists()
    }
    
    /**
     * Copy OpenJTalk dictionary from assets to internal storage
     */
    suspend fun copyDictionaryFromAssets(): Flow<CopyProgress> = flow {
        emit(CopyProgress.Copying(0))
        
        val assetManager = context.assets
        val destDir = File(context.filesDir, DICT_DIR)
        
        if (destDir.exists()) {
            emit(CopyProgress.Success)
            return@flow
        }
        
        try {
            destDir.mkdirs()
            val files = assetManager.list(DICT_DIR) ?: throw IllegalStateException("Dictionary not found in assets")
            
            val totalFiles = files.size
            files.forEachIndexed { index, fileName ->
                val assetPath = "$DICT_DIR/$fileName"
                val destFile = File(destDir, fileName)
                
                assetManager.open(assetPath).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val progress = ((index + 1) * 100) / totalFiles
                emit(CopyProgress.Copying(progress))
            }
            
            emit(CopyProgress.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy dictionary: ${e.message}", e)
            destDir.deleteRecursively()
            emit(CopyProgress.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if VVM model file exists
     */
    fun isVvmModelReady(vvmFileName: String): Boolean {
        val vvmFile = File(context.filesDir, "$vvmFileName.vvm")
        return vvmFile.exists() && vvmFile.length() > 10_000_000 // At least 10MB
    }
    
    /**
     * Download VVM model file
     */
    suspend fun downloadVvmModel(vvmFileName: String): Flow<DownloadProgress> = flow {
        val urlString = VVM_FILES[vvmFileName] ?: throw IllegalArgumentException("Unknown VVM: $vvmFileName")
        val vvmFile = File(context.filesDir, "$vvmFileName.vvm")
        
        if (vvmFile.exists() && vvmFile.length() > 10_000_000) {
            emit(DownloadProgress.Success)
            return@flow
        }
        
        emit(DownloadProgress.Downloading(0))
        
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 120000
                setRequestProperty("Accept", "*/*")
            }
            
            connection.inputStream.use { input ->
                val totalBytes = connection.contentLength.toLong()
                FileOutputStream(vvmFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            emit(DownloadProgress.Downloading(progress))
                        }
                    }
                }
            }
            
            emit(DownloadProgress.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download VVM: ${e.message}", e)
            vvmFile.delete()
            emit(DownloadProgress.Error(e.message ?: "Unknown error"))
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get VVM file size for display (approximate)
     */
    fun getVvmFileSizeMB(vvmFileName: String): String {
        return when (vvmFileName) {
            "0" -> "約75MB"
            "1" -> "約50MB"
            "2" -> "約55MB"
            "3" -> "約60MB"
            "4" -> "約55MB"
            "5" -> "約50MB"
            else -> "不明"
        }
    }
    
    /**
     * Delete VVM model file
     */
    fun deleteVvmModel(vvmFileName: String): Boolean {
        val vvmFile = File(context.filesDir, "$vvmFileName.vvm")
        return if (vvmFile.exists()) {
            vvmFile.delete()
        } else {
            true // Already deleted
        }
    }
    
    /**
     * Get list of downloaded VVM files
     */
    fun getDownloadedVvmFiles(): List<String> {
        return VVM_FILES.keys.filter { isVvmModelReady(it) }
    }
    
    sealed class CopyProgress {
        data class Copying(val percent: Int) : CopyProgress()
        object Success : CopyProgress()
        data class Error(val message: String) : CopyProgress()
    }
    
    sealed class DownloadProgress {
        data class Downloading(val percent: Int) : DownloadProgress()
        object Success : DownloadProgress()
        data class Error(val message: String) : DownloadProgress()
    }
}
