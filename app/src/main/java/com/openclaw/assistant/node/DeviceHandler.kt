package com.openclaw.assistant.node

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.openclaw.assistant.BuildConfig
import com.openclaw.assistant.SecurePrefs
import com.openclaw.assistant.gateway.GatewaySession
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class DeviceHandler(
  private val appContext: Context,
  private val prefs: SecurePrefs,
) {

  fun handleStatus(): GatewaySession.InvokeResult {
    val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

    val payload = buildJsonObject {
      put("batteryLevel", JsonPrimitive(level))
      put("charging", JsonPrimitive(isCharging))
      put("voiceWakeMode", JsonPrimitive(prefs.voiceWakeMode.value.rawValue))
      put("locationMode", JsonPrimitive(prefs.locationMode.value.rawValue))
      put("screenPreventSleep", JsonPrimitive(prefs.preventSleep.value))
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  fun handleInfo(): GatewaySession.InvokeResult {
    val payload = buildJsonObject {
      put("deviceId", JsonPrimitive(prefs.instanceId.value))
      put("name", JsonPrimitive(prefs.displayName.value))
      put("appVersion", JsonPrimitive(BuildConfig.VERSION_NAME))
      put("appBuild", JsonPrimitive(BuildConfig.VERSION_CODE))
      put("androidSdk", JsonPrimitive(android.os.Build.VERSION.SDK_INT))
      put("model", JsonPrimitive(android.os.Build.MODEL))
      put("manufacturer", JsonPrimitive(android.os.Build.MANUFACTURER))
      put("brand", JsonPrimitive(android.os.Build.BRAND))
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  fun handlePermissions(): GatewaySession.InvokeResult {
    val camera = hasPermission(Manifest.permission.CAMERA)
    val mic = hasPermission(Manifest.permission.RECORD_AUDIO)
    val loc = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    val sms = hasPermission(Manifest.permission.SEND_SMS)

    val payload = buildJsonObject {
      put("camera", JsonPrimitive(camera))
      put("microphone", JsonPrimitive(mic))
      put("location", JsonPrimitive(loc))
      put("sms", JsonPrimitive(sms))
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  fun handleHealth(): GatewaySession.InvokeResult {
    val payload = buildJsonObject {
      put("status", JsonPrimitive("ok"))
      put("timestamp", JsonPrimitive(System.currentTimeMillis()))
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  private fun hasPermission(perm: String): Boolean {
    return ContextCompat.checkSelfPermission(appContext, perm) == PackageManager.PERMISSION_GRANTED
  }
}
