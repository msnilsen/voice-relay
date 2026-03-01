package com.openclaw.assistant.node

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.openclaw.assistant.gateway.GatewaySession
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

class MotionHandler(private val appContext: Context) : SensorEventListener {

    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var currentSteps: Float = 0f

    init {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun handleActivity(): GatewaySession.InvokeResult {
        if (!hasPermission()) {
            return GatewaySession.InvokeResult.error(
                code = "MOTION_PERMISSION_REQUIRED",
                message = "MOTION_PERMISSION_REQUIRED: grant Activity Recognition permission"
            )
        }
        val payload = buildJsonObject {
            put("activity", JsonPrimitive("still")) // Activity Recognition API placeholder
        }
        return GatewaySession.InvokeResult.ok(payload.toString())
    }

    fun handlePedometer(): GatewaySession.InvokeResult {
        if (!hasPermission()) {
            return GatewaySession.InvokeResult.error(
                code = "MOTION_PERMISSION_REQUIRED",
                message = "MOTION_PERMISSION_REQUIRED: grant Activity Recognition permission"
            )
        }
        val payload = buildJsonObject {
            put("steps", JsonPrimitive(currentSteps.toInt()))
        }
        return GatewaySession.InvokeResult.ok(payload.toString())
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            currentSteps = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
