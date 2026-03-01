package com.openclaw.assistant.wear.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

/**
 * Wear OS Voice Interaction Service entry point.
 * Activated when OpenClaw is selected as the default assistant and the
 * user long-presses the hardware button.
 *
 * NOTE: The android.intent.action.ASSIST intent is on WearAssistActivity,
 * NOT on this service — that is what makes OpenClaw visible in the Wear OS
 * assistant picker (Settings > Apps > Default Apps > Assist app).
 */
class WearVoiceInteractionService : VoiceInteractionService() {

    private var isReady = false
    private var pendingSession = false

    override fun onReady() {
        super.onReady()
        isReady = true
        Log.d(TAG, "onReady")
        if (pendingSession) {
            pendingSession = false
            launchSession()
        }
    }

    override fun onShutdown() {
        super.onShutdown()
        isReady = false
        pendingSession = false
        Log.d(TAG, "onShutdown")
    }

    private fun launchSession() {
        try {
            showSession(Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST)
        } catch (e: Exception) {
            Log.e(TAG, "showSession failed", e)
        }
    }

    companion object {
        private const val TAG = "WearVIS"
    }
}
