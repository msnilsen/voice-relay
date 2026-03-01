package com.google.firebase

import android.content.Context

/**
 * No-op stub replacing Firebase initialization.
 */
class FirebaseApp private constructor() {
    companion object {
        fun initializeApp(context: Context) {
            // no-op
        }
    }
}
