package com.openclaw.assistant.api

import android.util.Log
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
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

enum class RequestFormat {
    SIMPLE,
    OPENAI,
    CUSTOM;

    companion object {
        fun fromString(s: String): RequestFormat = when (s.lowercase()) {
            "openai" -> OPENAI
            "custom" -> CUSTOM
            else -> SIMPLE
        }
    }
}

/**
 * HTTP client that POSTs voice commands to a configured webhook endpoint.
 * Supports three request formats:
 *   SIMPLE  - {"query": "...", "session_id": "..."}
 *   OPENAI  - OpenAI Chat Completions format
 *   CUSTOM  - User-defined JSON template with {{query}} and {{session_id}} placeholders
 */
class WebhookClient(
    private val ignoreSslErrors: Boolean = false,
    private val customJsonTemplate: String? = null
) {

    private val client: OkHttpClient = run {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        if (ignoreSslErrors) {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustAll), null)
            builder.sslSocketFactory(sslContext.socketFactory, trustAll)
                .hostnameVerifier { _, _ -> true }
        }
        builder.build()
    }

    private val gson = Gson()

    private fun buildRequestBody(
        message: String,
        sessionId: String,
        format: RequestFormat,
        customTemplate: String? = null,
        context: Map<String, String> = emptyMap()
    ): JsonObject {
        return when (format) {
            RequestFormat.SIMPLE -> JsonObject().apply {
                addProperty("query", message)
                addProperty("session_id", sessionId)
            }
            RequestFormat.OPENAI -> JsonObject().apply {
                addProperty("model", "default")
                addProperty("user", sessionId)
                val messagesArray = JsonArray()
                val userMessage = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", message)
                }
                messagesArray.add(userMessage)
                add("messages", messagesArray)
            }
            RequestFormat.CUSTOM -> {
                val template = customTemplate ?: DEFAULT_CUSTOM_TEMPLATE
                var filled = template
                    .replace("{{query}}", gson.toJson(message).let { it.substring(1, it.length - 1) })
                    .replace("{{session_id}}", gson.toJson(sessionId).let { it.substring(1, it.length - 1) })
                for ((key, value) in context) {
                    filled = filled.replace("{{$key}}", gson.toJson(value).let { it.substring(1, it.length - 1) })
                }
                try {
                    gson.fromJson(filled, JsonObject::class.java)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid custom JSON template, falling back to SIMPLE", e)
                    JsonObject().apply {
                        addProperty("query", message)
                        addProperty("session_id", sessionId)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebhookClient"
        const val DEFAULT_CUSTOM_TEMPLATE = """{"query": "{{query}}", "session_id": "{{session_id}}"}"""
    }

    suspend fun sendMessage(
        httpUrl: String,
        message: String,
        sessionId: String,
        authToken: String? = null,
        agentId: String? = null,
        format: RequestFormat = RequestFormat.SIMPLE,
        context: Map<String, String> = emptyMap()
    ): Result<WebhookResponse> = withContext(Dispatchers.IO) {
        if (httpUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Webhook URL is not configured")
            )
        }

        try {
            val requestBody = buildRequestBody(message, sessionId, format, customJsonTemplate, context)

            val jsonBody = gson.toJson(requestBody)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBuilder = Request.Builder()
                .url(httpUrl)
                .post(jsonBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")

            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${authToken.trim()}")
            }

            if (!agentId.isNullOrBlank()) {
                requestBuilder.addHeader("X-Agent-Id", agentId)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: response.message
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $errorBody")
                    )
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IOException("Empty response")
                    )
                }

                val text = extractResponseText(responseBody)
                Result.success(WebhookResponse(response = text ?: responseBody))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(
        httpUrl: String,
        authToken: String?,
        format: RequestFormat = RequestFormat.SIMPLE
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (httpUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Webhook URL is not configured")
            )
        }

        try {
            val requestBody = buildRequestBody("ping", "connection-test", format, customJsonTemplate)
            val jsonBody = gson.toJson(requestBody)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBuilder = Request.Builder()
                .url(httpUrl)
                .post(jsonBody)
                .addHeader("Content-Type", "application/json")

            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${authToken.trim()}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody = response.body?.string() ?: response.message
                    Result.failure(IOException("HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extract response text from various JSON formats.
     * Tries OpenAI format first, then common simple formats.
     */
    private fun extractResponseText(json: String): String? {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)

            obj.getAsJsonObject("error")?.let { error ->
                val errorMsg = error.get("message")?.asString ?: "Unknown error"
                throw IOException("API Error: $errorMsg")
            }

            // OpenAI format: choices[0].message.content
            obj.getAsJsonArray("choices")?.let { choices ->
                choices.firstOrNull()?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString
            }
            // Simple/generic formats
            ?: obj.get("response")?.asString
            ?: obj.get("text")?.asString
            ?: obj.get("message")?.asString
            ?: obj.get("content")?.asString
            ?: obj.get("result")?.asString
            ?: obj.get("answer")?.asString
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

data class WebhookResponse(
    val response: String? = null,
    val error: String? = null
) {
    fun getResponseText(): String? = response
}

// Type aliases for backward compatibility during migration
typealias OpenClawClient = WebhookClient
typealias OpenClawResponse = WebhookResponse
