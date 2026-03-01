package com.openclaw.assistant.node

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.openclaw.assistant.R
import com.openclaw.assistant.gateway.GatewaySession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class SystemHandler(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun handleNotify(paramsJson: String?): GatewaySession.InvokeResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return GatewaySession.InvokeResult.error("PERMISSION_REQUIRED", "POST_NOTIFICATIONS permission required")
            }
        }

        val params = paramsJson?.let {
            try {
                json.parseToJsonElement(it).jsonObject
            } catch (e: Exception) {
                null
            }
        } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Expected JSON object")

        val title = (params["title"] as? JsonPrimitive)?.content ?: "OpenClaw Assistant"
        val message = (params["message"] as? JsonPrimitive)?.content ?: ""

        if (message.isEmpty()) {
            return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Message is required")
        }

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "openclaw_system"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "OpenClaw System Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        return GatewaySession.InvokeResult.ok("""{"ok":true}""")
    }
}
