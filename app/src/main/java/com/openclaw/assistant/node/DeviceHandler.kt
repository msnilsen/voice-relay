package com.openclaw.assistant.node

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.openclaw.assistant.BuildConfig
import com.openclaw.assistant.gateway.GatewaySession
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

class DeviceHandler(private val appContext: Context) {

  fun handleDeviceInfo(): GatewaySession.InvokeResult {
    val payload = buildJsonObject {
      put("model", JsonPrimitive(Build.MODEL))
      put("manufacturer", JsonPrimitive(Build.MANUFACTURER))
      put("androidVersion", JsonPrimitive(Build.VERSION.RELEASE))
      put("sdkInt", JsonPrimitive(Build.VERSION.SDK_INT))
      put("appVersion", JsonPrimitive(BuildConfig.VERSION_NAME))
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  fun handleDeviceStatus(): GatewaySession.InvokeResult {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
      appContext.registerReceiver(null, ifilter)
    }
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val level = batteryStatus?.let { intent ->
      val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
      if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else null
    }

    val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isInteractive = powerManager.isInteractive

    val payload = buildJsonObject {
      put("battery", buildJsonObject {
        put("level", if (level != null) JsonPrimitive(level) else JsonPrimitive(-1))
        put("isCharging", JsonPrimitive(isCharging))
      })
      put("screen", buildJsonObject {
        put("isInteractive", JsonPrimitive(isInteractive))
      })
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  fun handleDevicePermissions(): GatewaySession.InvokeResult {
    val cameraGranted = try { hasPermission(Manifest.permission.CAMERA) } catch (e: Throwable) { false }
    val micGranted = try { hasPermission(Manifest.permission.RECORD_AUDIO) } catch (e: Throwable) { false }
    val fineLocationGranted = try { hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) } catch (e: Throwable) { false }
    val coarseLocationGranted = try { hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) } catch (e: Throwable) { false }
    val backgroundLocationGranted = try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
      } else {
        fineLocationGranted || coarseLocationGranted
      }
    } catch (e: Throwable) { false }
    val smsGranted = try { hasPermission(Manifest.permission.SEND_SMS) } catch (e: Throwable) { false }

    val payload = buildJsonObject {
      put("camera", JsonPrimitive(cameraGranted))
      put("microphone", JsonPrimitive(micGranted))
      put("location", buildJsonObject {
        put("fine", JsonPrimitive(fineLocationGranted))
        put("coarse", JsonPrimitive(coarseLocationGranted))
        put("background", JsonPrimitive(backgroundLocationGranted))
      })
      put("sms", JsonPrimitive(smsGranted))
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  fun handleDeviceHealth(): GatewaySession.InvokeResult {
    val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val stat = StatFs(Environment.getDataDirectory().path)
    val totalStorage = stat.totalBytes
    val availableStorage = stat.availableBytes

    val payload = buildJsonObject {
      put("memory", buildJsonObject {
        put("total", JsonPrimitive(memoryInfo.totalMem))
        put("available", JsonPrimitive(memoryInfo.availMem))
        put("lowMemory", JsonPrimitive(memoryInfo.lowMemory))
      })
      put("storage", buildJsonObject {
        put("total", JsonPrimitive(totalStorage))
        put("available", JsonPrimitive(availableStorage))
      })
    }
    return GatewaySession.InvokeResult.ok(payload.toString())
  }

  private fun hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
  }
}
