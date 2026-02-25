package com.openclaw.assistant.voice

import java.util.Locale

/**
 * Extracts the actual command from a transcript that includes a wake word.
 * Example: "Open Claw what time is it" -> "what time is it"
 */
object VoiceWakeCommandExtractor {

    /**
     * Extracts the command after any of the provided wake words.
     */
    fun extract(transcript: String, wakeWords: List<String>): String {
        val lowerTranscript = transcript.lowercase(Locale.ROOT)
        var bestMatchIdx = -1
        var wakeWordLength = 0

        for (word in wakeWords) {
            val lowerWord = word.lowercase(Locale.ROOT)
            val idx = lowerTranscript.indexOf(lowerWord)
            if (idx != -1) {
                // We want the earliest wake word found
                if (bestMatchIdx == -1 || idx < bestMatchIdx) {
                    bestMatchIdx = idx
                    wakeWordLength = lowerWord.length
                }
            }
        }

        if (bestMatchIdx == -1) return transcript

        val command = transcript.substring(bestMatchIdx + wakeWordLength).trim()
        return command
    }
}
