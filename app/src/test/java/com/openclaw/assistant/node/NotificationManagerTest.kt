package com.openclaw.assistant.node

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationManagerTest {

    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        // We use a real instance but it won't have the service connected in test environment
        notificationManager = NotificationManager(FakeContext())
    }

    @Test
    fun `isAccessGranted returns false when service is not connected`() {
        assertFalse(notificationManager.isAccessGranted())
    }

    @Test
    fun `listNotifications returns error when access is denied`() {
        val result = notificationManager.listNotifications()

        assertFalse(result.ok)
        assertEquals("NOTIFICATION_ACCESS_DENIED", result.error)
        assertTrue(result.payloadJson.contains("NOTIFICATION_ACCESS_DENIED"))
    }

    @Test
    fun `performAction returns error when access is denied`() = kotlinx.coroutines.test.runTest {
        val result = notificationManager.performAction("""{"key":"test","action":"dismiss"}""")

        assertFalse(result.ok)
        assertEquals("NOTIFICATION_ACCESS_DENIED", result.error)
        assertTrue(result.payloadJson.contains("NOTIFICATION_ACCESS_DENIED"))
    }

    private class FakeContext : android.content.ContextWrapper(null) {
        override fun getPackageName(): String = "com.openclaw.assistant"
    }
}
