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
import android.hardware.SensorManager

class MotionHandlerTest {
    private val context = mockk<Context>(relaxed = true)
    private val sensorManager = mockk<SensorManager>(relaxed = true)

    init {
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
    }

    private val handler by lazy { MotionHandler(context) }

    @Test
    fun `handleActivity returns error when permission missing`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) } returns PackageManager.PERMISSION_DENIED

        val result = handler.handleActivity()

        assertEquals(false, result.ok)
        assertEquals("MOTION_PERMISSION_REQUIRED", result.error?.code)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `handleActivity returns activity when permission granted`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) } returns PackageManager.PERMISSION_GRANTED

        val result = handler.handleActivity()

        assertEquals(true, result.ok)
        assertEquals(true, result.payloadJson?.contains("still"))
        unmockkStatic(ContextCompat::class)
    }
}
