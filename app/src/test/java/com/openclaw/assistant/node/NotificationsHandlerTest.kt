package com.openclaw.assistant.node

import android.content.Context
import com.openclaw.assistant.gateway.GatewaySession
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import android.service.notification.StatusBarNotification
import io.mockk.unmockkStatic
import android.provider.Settings
import android.content.ContentResolver

class NotificationsHandlerTest {
    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val notificationManager = mockk<NotificationManager>()
    private val handler = NotificationsHandler(context, notificationManager)

    @Test
    fun `handleList returns error when service disabled`() {
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.openclaw.assistant"
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(contentResolver, "enabled_notification_listeners") } returns ""

        val result = handler.handleList()

        assertEquals(false, result.ok)
        assertEquals("NOTIFICATIONS_PERMISSION_REQUIRED", result.error?.code)
        unmockkStatic(Settings.Secure::class)
    }

    @Test
    fun `handleList returns notifications when service enabled`() {
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.openclaw.assistant"
        mockkStatic(Settings.Secure::class)
        every { Settings.Secure.getString(contentResolver, "enabled_notification_listeners") } returns "com.openclaw.assistant"

        val sbn = mockk<StatusBarNotification>()
        every { sbn.key } returns "test_key"
        every { sbn.packageName } returns "com.test"
        every { sbn.postTime } returns 12345L
        every { sbn.notification.extras.getCharSequence("android.title") } returns "Title"
        every { sbn.notification.extras.getCharSequence("android.text") } returns "Text"

        every { notificationManager.getActiveNotifications() } returns listOf(sbn)

        val result = handler.handleList()

        assertEquals(true, result.ok)
        val json = result.payloadJson ?: ""
        assertEquals(true, json.contains("test_key"))
        unmockkStatic(Settings.Secure::class)
    }
}
