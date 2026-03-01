package com.openclaw.assistant.node

import android.content.Context
import com.openclaw.assistant.gateway.GatewaySession
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.service.notification.StatusBarNotification
import io.mockk.unmockkStatic

class NotificationsHandlerTest {
    private val context = mockk<Context>()
    private val notificationManager = mockk<NotificationManager>()
    private val handler = NotificationsHandler(context, notificationManager)

    @Test
    fun `handleList returns error when permission missing`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_DENIED

        val result = handler.handleList()

        assertEquals(false, result.ok)
        assertEquals("NOTIFICATIONS_PERMISSION_REQUIRED", result.error?.code)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `handleList returns notifications when permission granted`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_GRANTED

        val sbn = mockk<StatusBarNotification>()
        every { sbn.key } returns "test_key"
        every { sbn.packageName } returns "com.test"
        every { sbn.postTime } returns 12345L
        every { sbn.notification.extras.getCharSequence("android.title") } returns "Title"
        every { sbn.notification.extras.getCharSequence("android.text") } returns "Text"

        every { notificationManager.getActiveNotifications() } returns listOf(sbn)

        val result = handler.handleList()

        assertEquals(true, result.ok)
        // Check for specific substrings in the JSON instead of exact match if order or formatting varies
        val json = result.payloadJson ?: ""
        assertEquals(true, json.contains("test_key"))
        unmockkStatic(ContextCompat::class)
    }
}
