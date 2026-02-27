package com.openclaw.assistant.node

import com.openclaw.assistant.gateway.GatewaySession

class NotificationHandler(
    private val notificationManager: NotificationManager
) {
    fun handleList(): GatewaySession.InvokeResult {
        val result = notificationManager.listNotifications()
        return if (result.ok) {
            GatewaySession.InvokeResult.ok(result.payloadJson)
        } else {
            GatewaySession.InvokeResult.error(
                code = result.error ?: "NOTIFICATION_LIST_FAILED",
                message = result.payloadJson
            )
        }
    }

    suspend fun handleAction(paramsJson: String?): GatewaySession.InvokeResult {
        val result = notificationManager.performAction(paramsJson)
        return if (result.ok) {
            GatewaySession.InvokeResult.ok(result.payloadJson)
        } else {
            GatewaySession.InvokeResult.error(
                code = result.error ?: "NOTIFICATION_ACTION_FAILED",
                message = result.payloadJson
            )
        }
    }
}
