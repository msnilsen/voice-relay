package com.openclaw.assistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.openclaw.assistant.node.NotificationManager

/**
 * Captures notifications for the OpenClaw system.
 * Requires BIND_NOTIFICATION_LISTENER_SERVICE permission and user to enable it in Settings.
 */
class OpenClawNotificationListenerService : NotificationListenerService() {

    companion object {
        var manager: NotificationManager? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { manager?.onNotificationPosted(it) }
        Log.d("OpenClawNotification", "Notification posted from ${sbn?.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let { manager?.onNotificationRemoved(it) }
        Log.d("OpenClawNotification", "Notification removed from ${sbn?.packageName}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Initialize active notifications if needed
        activeNotifications?.forEach { sbn ->
            manager?.onNotificationPosted(sbn)
        }
    }
}
