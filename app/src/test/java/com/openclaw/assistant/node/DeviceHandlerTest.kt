package com.openclaw.assistant.node

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import com.openclaw.assistant.LocationMode
import com.openclaw.assistant.SecurePrefs
import com.openclaw.assistant.VoiceWakeMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceHandlerTest {

  private val mockContext = mockk<Context>(relaxed = true)
  private val mockPrefs = mockk<SecurePrefs>()
  private val mockBatteryManager = mockk<BatteryManager>()
  private val json = Json { ignoreUnknownKeys = true }

  private val deviceHandler: DeviceHandler

  init {
    every { mockContext.getSystemService(Context.BATTERY_SERVICE) } returns mockBatteryManager
    every { mockPrefs.voiceWakeMode } returns MutableStateFlow(VoiceWakeMode.Off)
    every { mockPrefs.locationMode } returns MutableStateFlow(LocationMode.Off)
    every { mockPrefs.preventSleep } returns MutableStateFlow(false)
    every { mockPrefs.instanceId } returns MutableStateFlow("test-device-id")
    every { mockPrefs.displayName } returns MutableStateFlow("Test Device")
    deviceHandler = DeviceHandler(mockContext, mockPrefs)
  }

  @Test
  fun handleStatus_returnsCorrectStructure() {
    every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 85
    every { mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

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
    every { mockContext.checkSelfPermission(Manifest.permission.CAMERA) } returns PackageManager.PERMISSION_GRANTED
    every { mockContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) } returns PackageManager.PERMISSION_DENIED
    every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) } returns PackageManager.PERMISSION_DENIED
    every { mockContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
    every { mockContext.checkSelfPermission(Manifest.permission.SEND_SMS) } returns PackageManager.PERMISSION_DENIED

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

  private fun kotlinx.serialization.json.JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject
}
