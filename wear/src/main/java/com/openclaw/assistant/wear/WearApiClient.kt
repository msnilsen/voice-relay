package com.openclaw.assistant.wear

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Minimal HTTP client for the Wear OS app.
 * Sends messages in OpenAI-compatible chat/completions format.
 * Self-contained: does NOT depend on the phone :app module.
 */
class WearApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(
        chatUrl: String,
        message: String,
        sessionId: String = UUID.randomUUID().toString(),
        authToken: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (chatUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("URL not configured"))
        }
        try {
            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", message)
                })
            }
            val body = JsonObject().apply {
                addProperty("model", "openclaw")
                addProperty("user", sessionId)
                add("messages", messages)
            }

            val requestBuilder = Request.Builder()
                .url(chatUrl)
                .post(gson.toJson(body).toRequestBody(jsonMediaType))

            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: ${response.message}")
                    )
                }
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response"))

                val text = extractContent(responseBody)
                if (text.isNullOrBlank()) {
                    Result.failure(IOException("No content in response"))
                } else {
                    Result.success(text)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractContent(json: String): String? {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)
            // Standard OpenAI format: choices[0].message.content
            obj.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString
        } catch (e: Exception) {
            null
        }
    }
}
