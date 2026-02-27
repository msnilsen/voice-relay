package com.openclaw.assistant.node

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import com.openclaw.assistant.SecurePrefs
import com.openclaw.assistant.VoiceWakeMode
import com.openclaw.assistant.LocationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.Application
import com.openclaw.assistant.OpenClawApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class DeviceHandlerTest {

  private lateinit var deviceHandler: DeviceHandler

  @Mock private lateinit var mockContext: Context
  @Mock private lateinit var mockPrefs: SecurePrefs
  @Mock private lateinit var mockBatteryManager: BatteryManager

  private val json = Json { ignoreUnknownKeys = true }

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    `when`(mockContext.getSystemService(Context.BATTERY_SERVICE)).thenReturn(mockBatteryManager)

    // Mock SecurePrefs flows
    `when`(mockPrefs.voiceWakeMode).thenReturn(MutableStateFlow(VoiceWakeMode.Off))
    `when`(mockPrefs.locationMode).thenReturn(MutableStateFlow(LocationMode.Off))
    `when`(mockPrefs.preventSleep).thenReturn(MutableStateFlow(false))
    `when`(mockPrefs.instanceId).thenReturn(MutableStateFlow("test-device-id"))
    `when`(mockPrefs.displayName).thenReturn(MutableStateFlow("Test Device"))

    deviceHandler = DeviceHandler(mockContext, mockPrefs)
  }

  @Test
  fun handleStatus_returnsCorrectStructure() {
    `when`(mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)).thenReturn(85)
    `when`(mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)).thenReturn(BatteryManager.BATTERY_STATUS_DISCHARGING)

    val result = deviceHandler.handleStatus()
    assertTrue(result.ok)

    val payload = json.parseToJsonElement(result.payloadJson!!).asObjectOrNull()!!
    assertEquals("85", (payload["batteryLevel"] as JsonPrimitive).content)
    assertEquals("false", (payload["charging"] as JsonPrimitive).content)
    assertEquals("off", (payload["voiceWakeMode"] as JsonPrimitive).content)
    assertEquals("off", (payload["locationMode"] as JsonPrimitive).content)
  }

  @Test
  fun handleInfo_returnsCorrectStructure() {
    val result = deviceHandler.handleInfo()
    assertTrue(result.ok)

    val payload = json.parseToJsonElement(result.payloadJson!!).asObjectOrNull()!!
    assertEquals("test-device-id", (payload["deviceId"] as JsonPrimitive).content)
    assertEquals("Test Device", (payload["name"] as JsonPrimitive).content)
    assertTrue(payload.containsKey("appVersion"))
    assertTrue(payload.containsKey("androidSdk"))
  }

  @Test
  fun handlePermissions_returnsCorrectStructure() {
    `when`(mockContext.checkPermission(eq(Manifest.permission.CAMERA), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_GRANTED)
    `when`(mockContext.checkPermission(eq(Manifest.permission.RECORD_AUDIO), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_DENIED)
    `when`(mockContext.checkPermission(eq(Manifest.permission.ACCESS_FINE_LOCATION), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_DENIED)
    `when`(mockContext.checkPermission(eq(Manifest.permission.ACCESS_COARSE_LOCATION), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_GRANTED)
    `when`(mockContext.checkPermission(eq(Manifest.permission.SEND_SMS), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_DENIED)

    val result = deviceHandler.handlePermissions()
    assertTrue(result.ok)

    val payload = json.parseToJsonElement(result.payloadJson!!).asObjectOrNull()!!
    assertTrue(payload.containsKey("camera"))
    assertTrue(payload.containsKey("microphone"))
    assertTrue(payload.containsKey("location"))
    assertTrue(payload.containsKey("sms"))
  }

  @Test
  fun handleHealth_returnsOk() {
    val result = deviceHandler.handleHealth()
    assertTrue(result.ok)

    val payload = json.parseToJsonElement(result.payloadJson!!).asObjectOrNull()!!
    assertEquals("ok", (payload["status"] as JsonPrimitive).content)
    assertTrue(payload.containsKey("timestamp"))
  }

  // Helper extension
  private fun kotlinx.serialization.json.JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject
}
