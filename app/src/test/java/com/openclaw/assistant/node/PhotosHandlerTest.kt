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
import android.provider.MediaStore
import android.database.MatrixCursor
import android.content.ContentResolver

class PhotosHandlerTest {
    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val handler = PhotosHandler(context)

    @Test
    fun `handleLatest returns error when permission missing`() {
        mockkStatic(ContextCompat::class)
        // Assume API < 33
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) } returns PackageManager.PERMISSION_DENIED

        val result = handler.handleLatest()

        assertEquals(false, result.ok)
        assertEquals("PHOTOS_PERMISSION_REQUIRED", result.error?.code)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `handleLatest returns photos when permission granted`() {
        mockkStatic(ContextCompat::class)
        every { context.contentResolver } returns contentResolver
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_GRANTED

        val cursor = MatrixCursor(arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE
        ))
        cursor.addRow(arrayOf(1L, "photo.jpg", 1000L, "image/jpeg", 500L))

        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor

        val result = handler.handleLatest()

        assertEquals(true, result.ok)
        assertEquals(true, result.payloadJson?.contains("photo.jpg"))
        unmockkStatic(ContextCompat::class)
    }
}
