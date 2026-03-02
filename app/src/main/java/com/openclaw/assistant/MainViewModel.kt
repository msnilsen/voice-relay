package com.openclaw.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.openclaw.assistant.data.SettingsRepository

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository.getInstance(app)

    val isWebhookConfigured: Boolean
        get() = settings.httpUrl.isNotBlank()
}
