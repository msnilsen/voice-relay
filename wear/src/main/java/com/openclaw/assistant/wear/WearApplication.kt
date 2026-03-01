package com.openclaw.assistant.wear

import android.app.Application

class WearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Pre-initialize settings repository singleton
        WearSettingsRepository.getInstance(this)
    }
}
