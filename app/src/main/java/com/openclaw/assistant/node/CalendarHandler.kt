package com.openclaw.assistant.node

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.openclaw.assistant.gateway.GatewaySession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.util.TimeZone

class CalendarHandler(private val appContext: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun hasReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun handleEvents(paramsJson: String?): GatewaySession.InvokeResult {
        if (!hasReadPermission()) {
            return GatewaySession.InvokeResult.error(
                code = "CALENDAR_READ_PERMISSION_REQUIRED",
                message = "CALENDAR_READ_PERMISSION_REQUIRED: grant Calendar read permission"
            )
        }

        val params = paramsJson?.let {
            try {
                json.parseToJsonElement(it).jsonObject
            } catch (e: Exception) {
                null
            }
        } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Expected JSON object")

        val startTime = (params["startTime"] as? JsonPrimitive)?.content?.toLongOrNull() ?: System.currentTimeMillis()
        val endTime = (params["endTime"] as? JsonPrimitive)?.content?.toLongOrNull() ?: (startTime + 86400000)

        val events = mutableListOf<JsonObject>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startTime.toString(), endTime.toString())
        val cursor = try {
            appContext.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
        } catch (e: SecurityException) {
            return GatewaySession.InvokeResult.error(
                code = "CALENDAR_READ_PERMISSION_REQUIRED",
                message = "CALENDAR_READ_PERMISSION_REQUIRED: ${e.message}"
            )
        }

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            var count = 0
            while (it.moveToNext() && count < 10) {
                val id = it.getLong(idIndex)
                val title = it.getString(titleIndex)
                val start = it.getLong(startIndex)
                val end = it.getLong(endIndex)
                events.add(buildJsonObject {
                    put("id", JsonPrimitive(id))
                    put("title", JsonPrimitive(title))
                    put("startTime", JsonPrimitive(start))
                    put("endTime", JsonPrimitive(end))
                })
                count++
            }
        }

        val payload = buildJsonObject {
            put("events", buildJsonArray {
                events.forEach { add(it) }
            })
        }
        return GatewaySession.InvokeResult.ok(payload.toString())
    }

    fun handleAdd(paramsJson: String?): GatewaySession.InvokeResult {
        if (!hasWritePermission()) {
            return GatewaySession.InvokeResult.error(
                code = "CALENDAR_WRITE_PERMISSION_REQUIRED",
                message = "CALENDAR_WRITE_PERMISSION_REQUIRED: grant Calendar write permission"
            )
        }

        val params = paramsJson?.let {
            try {
                json.parseToJsonElement(it).jsonObject
            } catch (e: Exception) {
                null
            }
        } ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Expected JSON object")

        val title = (params["title"] as? JsonPrimitive)?.content ?: ""
        val startTime = (params["startTime"] as? JsonPrimitive)?.content?.toLongOrNull() ?: return GatewaySession.InvokeResult.error("INVALID_REQUEST", "startTime is required")
        val endTime = (params["endTime"] as? JsonPrimitive)?.content?.toLongOrNull() ?: (startTime + 3600000)

        if (title.isEmpty()) {
            return GatewaySession.InvokeResult.error("INVALID_REQUEST", "Title is required")
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTime)
            put(CalendarContract.Events.DTEND, endTime)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.CALENDAR_ID, 1) // Assuming default calendar
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return try {
            val uri = appContext.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                GatewaySession.InvokeResult.ok("""{"ok":true,"id":${uri.lastPathSegment}}""")
            } else {
                GatewaySession.InvokeResult.error("CALENDAR_ADD_FAILED", "CALENDAR_ADD_FAILED: insert returned null")
            }
        } catch (e: Exception) {
            GatewaySession.InvokeResult.error("CALENDAR_ADD_FAILED", "CALENDAR_ADD_FAILED: ${e.message}")
        }
    }
}
