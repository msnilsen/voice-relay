package com.openclaw.assistant.voice

import android.content.Context
import android.util.Log
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.service.HotwordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages continuous wake word detection and command extraction.
 */
class VoiceWakeManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "VoiceWakeManager"
    private val settings = SettingsRepository.getInstance(context)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun start() {
        if (settings.hotwordEnabled) {
            HotwordService.start(context)
            _isListening.value = true
        }
    }

    fun stop() {
        HotwordService.stop(context)
        _isListening.value = false
    }

    /**
     * Called when a possible wake word + command is detected.
     */
    fun onWakeWordDetected(transcript: String): String {
        val wakeWords = settings.getWakeWords()
        val command = VoiceWakeCommandExtractor.extract(transcript, wakeWords)
        Log.d(TAG, "Extracted command: $command from transcript: $transcript")
        return command
    }
}
