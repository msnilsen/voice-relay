package com.openclaw.assistant.wakeword

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import com.openclaw.assistant.data.SettingsRepository

class PorcupineWakeWordEngine(
    private val context: Context,
    private val settings: SettingsRepository
) : WakeWordEngine {

    companion object {
        private const val TAG = "PorcupineWakeWord"

        fun mapPresetToKeyword(preset: String): Porcupine.BuiltInKeyword? = when (preset) {
            SettingsRepository.WAKE_WORD_JARVIS -> Porcupine.BuiltInKeyword.JARVIS
            SettingsRepository.WAKE_WORD_COMPUTER -> Porcupine.BuiltInKeyword.COMPUTER
            SettingsRepository.WAKE_WORD_HEY_ASSISTANT -> Porcupine.BuiltInKeyword.HEY_GOOGLE
            SettingsRepository.WAKE_WORD_OPEN_CLAW -> Porcupine.BuiltInKeyword.PORCUPINE
            else -> null
        }
    }

    private var porcupineManager: PorcupineManager? = null
    private var onDetectedCallback: (() -> Unit)? = null

    override fun isAvailable(): Boolean {
        return settings.porcupineAccessKey.isNotBlank()
    }

    override fun start(onDetected: () -> Unit) {
        onDetectedCallback = onDetected
        val accessKey = settings.porcupineAccessKey
        if (accessKey.isBlank()) {
            Log.w(TAG, "No Porcupine access key configured")
            return
        }

        val keyword = mapPresetToKeyword(settings.wakeWordPreset)
        if (keyword == null) {
            Log.w(TAG, "Wake word preset '${settings.wakeWordPreset}' not supported by Porcupine")
            return
        }

        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(keyword)
                .setSensitivity(0.7f)
                .build(context, PorcupineManagerCallback { keywordIndex ->
                    Log.d(TAG, "Wake word detected (keyword index: $keywordIndex)")
                    onDetectedCallback?.invoke()
                })
            porcupineManager?.start()
            Log.d(TAG, "Porcupine listening for: $keyword")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Porcupine", e)
            porcupineManager = null
        }
    }

    override fun stop() {
        try {
            porcupineManager?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Porcupine", e)
        }
    }

    override fun release() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Porcupine", e)
        }
        porcupineManager = null
        onDetectedCallback = null
    }
}
