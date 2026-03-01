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
    OPENAI;

    companion object {
        fun fromString(s: String): RequestFormat = when (s.lowercase()) {
            "openai" -> OPENAI
            else -> SIMPLE
        }
    }
}

/**
 * HTTP client that POSTs voice commands to a configured webhook endpoint.
 * Supports two request formats:
 *   SIMPLE  - {"query": "...", "session_id": "..."}
 *   OPENAI  - OpenAI Chat Completions format
 */
class WebhookClient(private val ignoreSslErrors: Boolean = false) {

    companion object {
        private const val TAG = "WebhookClient"
    }

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

    private fun buildRequestBody(message: String, sessionId: String, format: RequestFormat): JsonObject {
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
        }
    }

    suspend fun sendMessage(
        httpUrl: String,
        message: String,
        sessionId: String,
        authToken: String? = null,
        agentId: String? = null,
        format: RequestFormat = RequestFormat.SIMPLE
    ): Result<WebhookResponse> = withContext(Dispatchers.IO) {
        if (httpUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Webhook URL is not configured")
            )
        }

        try {
            val requestBody = buildRequestBody(message, sessionId, format)

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
            var requestBuilder = Request.Builder()
                .url(httpUrl)
                .head()

            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${authToken.trim()}")
            }

            var request = requestBuilder.build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext Result.success(true)
                    if (response.code == 405) {
                        // Fallthrough to POST
                    } else {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                // Fallthrough to POST
            }

            val requestBody = buildRequestBody("ping", "connection-test", format)

            val jsonBody = gson.toJson(requestBody)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            requestBuilder = Request.Builder()
                .url(httpUrl)
                .post(jsonBody)
                .addHeader("Content-Type", "application/json")

            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${authToken.trim()}")
            }

            request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
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
