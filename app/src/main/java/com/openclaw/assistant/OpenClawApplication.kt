package com.openclaw.assistant

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.data.SettingsRepository

class OpenClawApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedAppLocale()
        if (BuildConfig.DEBUG && BuildConfig.FIREBASE_ENABLED) {
            FirebaseApp.initializeApp(this)
        }
    }

    private fun applySavedAppLocale() {
        val tag = SettingsRepository.getInstance(this).appLanguage.trim()
        val locales = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
