package com.openclaw.assistant.wakeword

interface WakeWordEngine {
    fun start(onDetected: () -> Unit)
    fun stop()
    fun release()
    fun isAvailable(): Boolean
}
