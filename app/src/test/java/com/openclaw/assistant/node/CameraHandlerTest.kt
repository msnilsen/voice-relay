package com.openclaw.assistant.node

import android.content.Context
import com.openclaw.assistant.CameraHudKind
import com.openclaw.assistant.SecurePrefs
import com.openclaw.assistant.gateway.GatewayEndpoint
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraHandlerTest {

  private val context = mockk<Context>()
  private val camera = mockk<CameraCaptureManager>()
  private val prefs = mockk<SecurePrefs>()
  private val endpoint = mockk<GatewayEndpoint>()
  private val audioState = MutableStateFlow(false)
  private val showHud = mockk<(String, CameraHudKind, Long?) -> Unit>(relaxed = true)
  private val flash = mockk<() -> Unit>(relaxed = true)
  private val errorConverter = { err: Throwable -> Pair("ERROR", err.message ?: "error") }

  private val handler = CameraHandler(
    appContext = context,
    camera = camera,
    prefs = prefs,
    connectedEndpoint = { endpoint },
    externalAudioCaptureActive = audioState,
    showCameraHud = showHud,
    triggerCameraFlash = flash,
    invokeErrorFromThrowable = errorConverter
  )

  @Test
  fun `handleList returns correct json`() = runTest {
    coEvery { camera.list() } returns listOf(
      CameraCaptureManager.Device("0", "back"),
      CameraCaptureManager.Device("1", "front")
    )

    val result = handler.handleList(null)

    assertTrue(result.ok)
    // The JSON string construction in handleList uses joinToString with default separator ","
    // and manually adds brackets and curlies.
    // expected: {"cameras":[{"id":"0","facing":"back"},{"id":"1","facing":"front"}]}
    assertEquals(
      """{"cameras":[{"id":"0","facing":"back"},{"id":"1","facing":"front"}]}""",
      result.payloadJson
    )
  }

  @Test
  fun `handleList handles empty list`() = runTest {
    coEvery { camera.list() } returns emptyList()

    val result = handler.handleList(null)

    assertTrue(result.ok)
    assertEquals("""{"cameras":[]}""", result.payloadJson)
  }

  @Test
  fun `handleList handles errors`() = runTest {
    coEvery { camera.list() } throws RuntimeException("camera boom")

    val result = handler.handleList(null)

    assertTrue(!result.ok)
    assertEquals("ERROR", result.error?.code)
    assertEquals("camera boom", result.error?.message)
  }
}
