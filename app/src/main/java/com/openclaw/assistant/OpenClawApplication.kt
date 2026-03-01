package com.openclaw.assistant

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openclaw.assistant.data.SettingsRepository
import java.security.Security

class OpenClawApplication : Application() {

    val nodeRuntime: com.openclaw.assistant.node.NodeRuntime by lazy {
        com.openclaw.assistant.node.NodeRuntime(this)
    }

    override fun onCreate() {
        super.onCreate()
        applySavedAppLocale()
        // In debug builds, FirebaseInitProvider is removed from the manifest so that fork PRs
        // (which lack a real API key) do not crash on launch. Initialize Firebase manually here
        // when the build flag indicates a real key is present.
        if (BuildConfig.DEBUG && BuildConfig.FIREBASE_ENABLED) {
            FirebaseApp.initializeApp(this)
        }
        // Register Bouncy Castle as highest-priority provider for Ed25519 support
        try {
            val bcProvider = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
                .getDeclaredConstructor().newInstance() as java.security.Provider
            Security.removeProvider("BC")
            Security.insertProviderAt(bcProvider, 1)
        } catch (e: Throwable) {
            Log.e("OpenClawApp", "Failed to register Bouncy Castle provider", e)
            if (BuildConfig.FIREBASE_ENABLED) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
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
