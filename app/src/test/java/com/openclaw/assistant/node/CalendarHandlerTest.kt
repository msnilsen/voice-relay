package com.openclaw.assistant.node

import android.content.Context
import com.openclaw.assistant.gateway.GatewaySession
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.provider.CalendarContract
import android.database.MatrixCursor
import android.content.ContentResolver

class CalendarHandlerTest {
    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val handler = CalendarHandler(context)

    @Test
    fun `handleEvents returns error when permission missing`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) } returns PackageManager.PERMISSION_DENIED

        val result = handler.handleEvents("""{"startTime":"123"}""")

        assertEquals(false, result.ok)
        assertEquals("CALENDAR_READ_PERMISSION_REQUIRED", result.error?.code)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `handleEvents returns events when permission granted`() {
        mockkStatic(ContextCompat::class)
        every { context.contentResolver } returns contentResolver
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) } returns PackageManager.PERMISSION_GRANTED

        val cursor = MatrixCursor(arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        ))
        cursor.addRow(arrayOf(1L, "Event", 1000L, 2000L))

        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor

        val result = handler.handleEvents("""{"startTime":"100"}""")

        assertEquals(true, result.ok)
        assertEquals(true, result.payloadJson?.contains("Event"))
        unmockkStatic(ContextCompat::class)
    }
}
