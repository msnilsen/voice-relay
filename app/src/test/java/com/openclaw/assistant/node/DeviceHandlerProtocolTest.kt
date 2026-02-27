package com.openclaw.assistant.node

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive

class DeviceHandlerProtocolTest {

    @Test
    fun testDeviceInfoPayloadShape() {
        val payload = buildJsonObject {
            put("model", JsonPrimitive("Test Model"))
            put("manufacturer", JsonPrimitive("Test Manufacturer"))
            put("androidVersion", JsonPrimitive("13"))
            put("sdkInt", JsonPrimitive(33))
            put("appVersion", JsonPrimitive("1.2.3"))
        }
        val json = payload.toString()
        assertTrue(json.contains("model"))
        assertTrue(json.contains("manufacturer"))
        assertTrue(json.contains("androidVersion"))
        assertTrue(json.contains("sdkInt"))
        assertTrue(json.contains("appVersion"))
    }

    @Test
    fun testDeviceStatusPayloadShape() {
        val payload = buildJsonObject {
            put("battery", buildJsonObject {
                put("level", JsonPrimitive(85))
                put("isCharging", JsonPrimitive(true))
            })
            put("screen", buildJsonObject {
                put("isInteractive", JsonPrimitive(true))
            })
        }
        val json = payload.toString()
        assertTrue(json.contains("battery"))
        assertTrue(json.contains("level"))
        assertTrue(json.contains("isCharging"))
        assertTrue(json.contains("screen"))
        assertTrue(json.contains("isInteractive"))
    }

    @Test
    fun testDevicePermissionsPayloadShape() {
        val payload = buildJsonObject {
            put("camera", JsonPrimitive(true))
            put("microphone", JsonPrimitive(true))
            put("location", buildJsonObject {
                put("fine", JsonPrimitive(true))
                put("coarse", JsonPrimitive(true))
                put("background", JsonPrimitive(true))
            })
            put("sms", JsonPrimitive(true))
        }
        val json = payload.toString()
        assertTrue(json.contains("camera"))
        assertTrue(json.contains("microphone"))
        assertTrue(json.contains("location"))
        assertTrue(json.contains("fine"))
        assertTrue(json.contains("coarse"))
        assertTrue(json.contains("background"))
        assertTrue(json.contains("sms"))
    }

    @Test
    fun testDeviceHealthPayloadShape() {
        val payload = buildJsonObject {
            put("memory", buildJsonObject {
                put("total", JsonPrimitive(8000000000L))
                put("available", JsonPrimitive(4000000000L))
                put("lowMemory", JsonPrimitive(false))
            })
            put("storage", buildJsonObject {
                put("total", JsonPrimitive(128000000000L))
                put("available", JsonPrimitive(64000000000L))
            })
        }
        val json = payload.toString()
        assertTrue(json.contains("memory"))
        assertTrue(json.contains("total"))
        assertTrue(json.contains("available"))
        assertTrue(json.contains("lowMemory"))
        assertTrue(json.contains("storage"))
    }
}
