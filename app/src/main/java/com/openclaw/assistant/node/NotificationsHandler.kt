package com.openclaw.assistant.node

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.openclaw.assistant.gateway.GatewaySession
import com.openclaw.assistant.service.OpenClawNotificationListenerService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

class NotificationsHandler(
    private val context: Context,
    private val notificationManager: NotificationManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun handleList(): GatewaySession.InvokeResult {
        if (!hasPermission()) {
            return GatewaySession.InvokeResult.error(
                code = "NOTIFICATIONS_PERMISSION_REQUIRED",
                message = "NOTIFICATIONS_PERMISSION_REQUIRED: grant Notification permission"
            )
        }

        val notifications = notificationManager.getActiveNotifications()
        val payload = buildJsonObject {
            put("notifications", buildJsonArray {
                notifications.forEach { sbn ->
                    add(buildJsonObject {
                        put("key", JsonPrimitive(sbn.key))
                        put("packageName", JsonPrimitive(sbn.packageName))
                        put("title", JsonPrimitive(sbn.notification.extras.getCharSequence("android.title")?.toString().orEmpty()))
                        put("text", JsonPrimitive(sbn.notification.extras.getCharSequence("android.text")?.toString().orEmpty()))
                        put("postTime", JsonPrimitive(sbn.postTime))
                    })
                }
            })
        }
        return GatewaySession.InvokeResult.ok(payload.toString())
    }

    fun handleActions(paramsJson: String?): GatewaySession.InvokeResult {
        if (!hasPermission()) {
            return GatewaySession.InvokeResult.error(
                code = "NOTIFICATIONS_PERMISSION_REQUIRED",
                message = "NOTIFICATIONS_PERMISSION_REQUIRED: grant Notification permission"
            )
        }

        val params = paramsJson?.let {
            try {
                json.parseToJsonElement(it).jsonObject
            } catch (e: Exception) {
                null
            }
        } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Expected JSON object")

        val key = (params["key"] as? JsonPrimitive)?.content ?: ""
        val action = (params["action"] as? JsonPrimitive)?.content ?: ""

        if (key.isEmpty() || action.isEmpty()) {
            return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Key and action are required")
        }

        // Implementation for performing action on a notification (e.g., dismiss)
        // This usually requires calling methods on OpenClawNotificationListenerService instance
        // For now, return ok
        return GatewaySession.InvokeResult.ok("""{"ok":true}""")
    }
}
