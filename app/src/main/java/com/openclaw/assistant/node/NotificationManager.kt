package com.openclaw.assistant.node

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.os.Bundle
import com.openclaw.assistant.service.OpenClawNotificationListenerService
import kotlinx.serialization.json.*

class NotificationManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    data class NotificationResult(
        val ok: Boolean,
        val error: String? = null,
        val payloadJson: String,
    )

    fun isAccessGranted(): Boolean {
        return OpenClawNotificationListenerService.getConnectedInstance() != null
    }

    fun listNotifications(): NotificationResult {
        val service = OpenClawNotificationListenerService.getConnectedInstance()
            ?: return NotificationResult(
                ok = false,
                error = "NOTIFICATION_ACCESS_DENIED",
                payloadJson = """{"error":"NOTIFICATION_ACCESS_DENIED: enable notification access for OpenClaw in Android Settings"}"""
            )

        val notifications = try {
            service.activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            return NotificationResult(
                ok = false,
                error = "NOTIFICATION_LIST_FAILED",
                payloadJson = """{"error":"NOTIFICATION_LIST_FAILED: ${e.message}"}"""
            )
        }

        val list = notifications.map { sbn ->
            val n = sbn.notification
            val extras = n.extras
            buildJsonObject {
                put("key", sbn.key)
                put("package", sbn.packageName)
                put("title", extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "")
                put("text", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "")
                put("subText", extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "")
                put("postTime", sbn.postTime)
                put("isClearable", sbn.isClearable)
                put("isOngoing", sbn.isOngoing)

                val actions = n.actions?.mapIndexed { index, action ->
                    buildJsonObject {
                        put("id", index)
                        put("title", action.title?.toString() ?: "")
                        put("hasRemoteInput", action.remoteInputs?.isNotEmpty() == true)
                    }
                } ?: emptyList()
                put("actions", JsonArray(actions))
            }
        }

        return NotificationResult(
            ok = true,
            payloadJson = buildJsonObject {
                put("notifications", JsonArray(list))
            }.toString()
        )
    }

    suspend fun performAction(paramsJson: String?): NotificationResult {
        val service = OpenClawNotificationListenerService.getConnectedInstance()
            ?: return NotificationResult(
                ok = false,
                error = "NOTIFICATION_ACCESS_DENIED",
                payloadJson = """{"error":"NOTIFICATION_ACCESS_DENIED: enable notification access for OpenClaw in Android Settings"}"""
            )

        val params = try {
            paramsJson?.let { json.parseToJsonElement(it).jsonObject }
        } catch (e: Exception) {
            null
        } ?: return NotificationResult(
            ok = false,
            error = "INVALID_REQUEST",
            payloadJson = """{"error":"INVALID_REQUEST: valid JSON params required"}"""
        )

        val key = params["key"]?.jsonPrimitive?.content
            ?: return NotificationResult(
                ok = false,
                error = "INVALID_REQUEST",
                payloadJson = """{"error":"INVALID_REQUEST: 'key' required"}"""
            )

        val action = params["action"]?.jsonPrimitive?.content
            ?: return NotificationResult(
                ok = false,
                error = "INVALID_REQUEST",
                payloadJson = """{"error":"INVALID_REQUEST: 'action' required"}"""
            )

        val sbn = try {
            service.activeNotifications?.find { it.key == key }
        } catch (e: Exception) {
            null
        } ?: return NotificationResult(
            ok = false,
            error = "NOTIFICATION_NOT_FOUND",
            payloadJson = """{"error":"NOTIFICATION_NOT_FOUND: notification with key $key not found"}"""
        )

        return when (action) {
            "dismiss" -> {
                if (sbn.isClearable) {
                    service.cancelNotification(sbn.key)
                    NotificationResult(ok = true, payloadJson = """{"ok":true}""")
                } else {
                    NotificationResult(
                        ok = false,
                        error = "NOTIFICATION_NOT_CLEARABLE",
                        payloadJson = """{"error":"NOTIFICATION_NOT_CLEARABLE"}"""
                    )
                }
            }
            "open" -> {
                try {
                    sbn.notification.contentIntent?.send()
                    NotificationResult(ok = true, payloadJson = """{"ok":true}""")
                } catch (e: Exception) {
                    NotificationResult(
                        ok = false,
                        error = "ACTION_FAILED",
                        payloadJson = """{"error":"${e.message}"}"""
                    )
                }
            }
            "reply" -> {
                val text = params["text"]?.jsonPrimitive?.content
                    ?: return NotificationResult(
                        ok = false,
                        error = "INVALID_REQUEST",
                        payloadJson = """{"error":"INVALID_REQUEST: 'text' required for reply"}"""
                    )

                val replyAction = sbn.notification.actions?.find { it.remoteInputs?.isNotEmpty() == true }
                    ?: return NotificationResult(
                        ok = false,
                        error = "REPLY_NOT_SUPPORTED",
                        payloadJson = """{"error":"REPLY_NOT_SUPPORTED: notification does not support remote input reply"}"""
                    )

                try {
                    val remoteInput = replyAction.remoteInputs[0]
                    val intent = android.content.Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(remoteInput.resultKey, text)
                    RemoteInput.addResultsToIntent(replyAction.remoteInputs, intent, bundle)
                    replyAction.actionIntent.send(context, 0, intent)
                    NotificationResult(ok = true, payloadJson = """{"ok":true}""")
                } catch (e: Exception) {
                    NotificationResult(
                        ok = false,
                        error = "ACTION_FAILED",
                        payloadJson = """{"error":"${e.message}"}"""
                    )
                }
            }
            else -> {
                // Check if it's a numeric action ID
                val actionId = action.toIntOrNull()
                if (actionId != null && sbn.notification.actions != null && actionId >= 0 && actionId < sbn.notification.actions.size) {
                    val customAction = sbn.notification.actions[actionId]
                    try {
                        customAction.actionIntent.send()
                        NotificationResult(ok = true, payloadJson = """{"ok":true}""")
                    } catch (e: Exception) {
                        NotificationResult(
                            ok = false,
                            error = "ACTION_FAILED",
                            payloadJson = """{"error":"${e.message}"}"""
                        )
                    }
                } else {
                    NotificationResult(
                        ok = false,
                        error = "INVALID_ACTION",
                        payloadJson = """{"error":"INVALID_ACTION: $action"}"""
                    )
                }
            }
        }
    }
}
