package com.openclaw.assistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.atomic.AtomicReference

/**
 * Service to listen for and interact with system notifications.
 * Must be enabled by the user in Android Settings.
 */
class OpenClawNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance.set(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance.set(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance.set(null)
    }

    companion object {
        private val instance = AtomicReference<OpenClawNotificationListenerService?>()

        fun getConnectedInstance(): OpenClawNotificationListenerService? = instance.get()
    }
}
