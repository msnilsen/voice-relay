package com.openclaw.assistant.speech

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Dummy implementation of VoiceVoxModelManager for standard flavor.
 * VOICEVOX functionality is only available in full flavor.
 */
class VoiceVoxModelManager(private val context: Context) {
    
    fun isDictionaryReady(): Boolean = false
    
    fun isVvmModelReady(vvmFileName: String): Boolean = false
    
    fun getDownloadedVvmFiles(): List<String> = emptyList()
    
    fun getVvmFileSizeMB(vvmFileName: String): String = "N/A"
    
    fun deleteVvmModel(vvmFileName: String): Boolean = false
    
    data class DownloadProgress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val isCompleted: Boolean = false,
        val error: String? = null
    )
    
    fun downloadVvmModel(vvmFileName: String): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(0, 0, error = "VOICEVOX is only available in full flavor"))
    }
    
    data class CopyProgress(
        val filesCopied: Int,
        val totalFiles: Int,
        val currentFile: String? = null,
        val isCompleted: Boolean = false,
        val error: String? = null
    )
    
    suspend fun copyDictionaryFromAssets(): Flow<CopyProgress> = flow {
        emit(CopyProgress(0, 0, error = "VOICEVOX is only available in full flavor"))
    }
}
