package com.google.firebase.crashlytics

import android.util.Log

/**
 * No-op stub replacing Firebase Crashlytics.
 * Logs exceptions locally instead of sending to Firebase.
 */
class FirebaseCrashlytics private constructor() {
    fun recordException(e: Throwable) {
        Log.w("Crashlytics", "Exception recorded (local only)", e)
    }

    companion object {
        private val INSTANCE = FirebaseCrashlytics()
        fun getInstance(): FirebaseCrashlytics = INSTANCE
    }
}
