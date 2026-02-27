package com.openclaw.assistant.node

import android.content.Context
import com.openclaw.assistant.CameraHudKind
import com.openclaw.assistant.SecurePrefs
import com.openclaw.assistant.gateway.GatewaySession
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraHandlerTest {
  private val context = mockk<Context>(relaxed = true)
  private val camera = mockk<CameraCaptureManager>()
  private val prefs = mockk<SecurePrefs>()
  private val externalAudioCaptureActive = MutableStateFlow(false)

  private val handler = CameraHandler(
    appContext = context,
    camera = camera,
    prefs = prefs,
    connectedEndpoint = { null },
    externalAudioCaptureActive = externalAudioCaptureActive,
    showCameraHud = { _, _, _ -> },
    triggerCameraFlash = {},
    invokeErrorFromThrowable = { Pair("ERROR", it.message ?: "error") }
  )

  @Test
  fun testHandleListSuccess() = runBlocking {
    coEvery { camera.list() } returns listOf(
      CameraCaptureManager.Device(id = "0", facing = "back"),
      CameraCaptureManager.Device(id = "1", facing = "front")
    )

    val result = handler.handleList(null)

    assertTrue(result.ok)
    assertEquals(
      """{"cameras":[{"id":"0","facing":"back"},{"id":"1","facing":"front"}]}""",
      result.payloadJson
    )
  }

  @Test
  fun testHandleListError() = runBlocking {
    coEvery { camera.list() } throws Exception("camera failure")

    val result = handler.handleList(null)

    assertEquals(false, result.ok)
    assertEquals("ERROR", result.error?.code)
    assertEquals("camera failure", result.error?.message)
  }
}
