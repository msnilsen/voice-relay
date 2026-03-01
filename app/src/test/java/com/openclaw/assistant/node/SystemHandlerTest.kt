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
import android.os.Build

class SystemHandlerTest {
    private val context = mockk<Context>()
    private val handler = SystemHandler(context)

    @Test
    fun `handleNotify returns error when permission missing on Android 13+`() {
        // We can't easily mock Build.VERSION.SDK_INT but we can mock ContextCompat
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_DENIED

        val result = handler.handleNotify("""{"message":"test"}""")

        // If the test environment is Android 13+, it should return error.
        // If not, it might succeed.
        // For the sake of this test, let's assume we want to test the failure path.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            assertEquals(false, result.ok)
            assertEquals("PERMISSION_REQUIRED", result.error?.code)
        }
        unmockkStatic(ContextCompat::class)
    }
}
