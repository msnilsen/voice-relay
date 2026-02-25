package com.openclaw.assistant.voice

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses inline JSON directives from assistant text.
 * Directives are enclosed in <| and |> markers.
 * Example: "Hello there! <|{"voice_id": "pNInz6obpg8ndclK7BJ3"}|>"
 */
object TalkDirectiveParser {
    private const val TAG = "TalkDirectiveParser"
    private val START_MARKER = "<|"
    private val END_MARKER = "|>"

    private val json = Json { ignoreUnknownKeys = true }

    data class TalkDirectives(
        val voiceId: String? = null,
        val modelId: String? = null,
        val speed: Float? = null,
        val stability: Float? = null,
        val similarityBoost: Float? = null,
        val style: Float? = null,
        val useSpeakerBoost: Boolean? = null
    )

    /**
     * Extracts directives and returns the clean text and the parsed directives.
     */
    fun parse(text: String): Pair<String, TalkDirectives> {
        var cleanText = text
        var directives = TalkDirectives()

        while (true) {
            val startIdx = cleanText.indexOf(START_MARKER)
            if (startIdx == -1) break

            val endIdx = cleanText.indexOf(END_MARKER, startIdx + START_MARKER.length)
            if (endIdx == -1) break

            val jsonStr = cleanText.substring(startIdx + START_MARKER.length, endIdx)
            try {
                val obj = json.parseToJsonElement(jsonStr) as? JsonObject
                if (obj != null) {
                    directives = merge(directives, parseJsonObject(obj))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse directive JSON: $jsonStr", e)
            }

            // Remove the directive from the text
            cleanText = cleanText.removeRange(startIdx, endIdx + END_MARKER.length)
        }

        return cleanText.trim() to directives
    }

    private fun parseJsonObject(obj: JsonObject): TalkDirectives {
        return TalkDirectives(
            voiceId = obj["voice_id"]?.jsonPrimitive?.content,
            modelId = obj["model_id"]?.jsonPrimitive?.content,
            speed = obj["speed"]?.jsonPrimitive?.content?.toFloatOrNull(),
            stability = obj["stability"]?.jsonPrimitive?.content?.toFloatOrNull(),
            similarityBoost = obj["similarity_boost"]?.jsonPrimitive?.content?.toFloatOrNull(),
            style = obj["style"]?.jsonPrimitive?.content?.toFloatOrNull(),
            useSpeakerBoost = obj["use_speaker_boost"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()
        )
    }

    private fun merge(old: TalkDirectives, new: TalkDirectives): TalkDirectives {
        return TalkDirectives(
            voiceId = new.voiceId ?: old.voiceId,
            modelId = new.modelId ?: old.modelId,
            speed = new.speed ?: old.speed,
            stability = new.stability ?: old.stability,
            similarityBoost = new.similarityBoost ?: old.similarityBoost,
            style = new.style ?: old.style,
            useSpeakerBoost = new.useSpeakerBoost ?: old.useSpeakerBoost
        )
    }
}
