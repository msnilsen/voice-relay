package com.openclaw.assistant.wakeword

import android.content.Context
import com.openclaw.assistant.data.SettingsRepository
import com.openwakeword.OpenWakeWord

/**
 * App-level adapter that reads settings and delegates to the
 * [OpenWakeWord] library module.
 */
class OpenWakeWordEngine(
    private val context: Context,
    private val settings: SettingsRepository
) : WakeWordEngine {

    private var detector: OpenWakeWord? = null

    override fun isAvailable(): Boolean = true

    override fun start(onDetected: () -> Unit) {
        if (detector == null) {
            val builder = OpenWakeWord.Builder(context)
                .setThreshold(settings.wakeWordThreshold)

            val model = presetToModel(settings.wakeWordPreset)
            if (model != null) builder.setModel(model)

            detector = builder.build()
        }
        detector?.start { onDetected() }
    }

    override fun stop() {
        detector?.stop()
    }

    override fun release() {
        detector?.release()
        detector = null
    }

    companion object {
        fun presetToModel(preset: String): OpenWakeWord.BuiltInModel? = when (preset) {
            SettingsRepository.WAKE_WORD_HEY_JARVIS -> OpenWakeWord.BuiltInModel.HEY_JARVIS
            SettingsRepository.WAKE_WORD_ALEXA -> OpenWakeWord.BuiltInModel.ALEXA
            SettingsRepository.WAKE_WORD_HEY_MYCROFT -> OpenWakeWord.BuiltInModel.HEY_MYCROFT
            else -> null
        }
    }
}
