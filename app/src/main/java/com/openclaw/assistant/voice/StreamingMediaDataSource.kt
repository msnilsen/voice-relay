package com.openclaw.assistant.voice

import android.media.MediaDataSource
import android.util.Log
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Real-time audio streaming data source for MediaPlayer.
 * Buffers incoming chunks and provides them to the player.
 */
class StreamingMediaDataSource : MediaDataSource() {
    private val TAG = "StreamingMediaDataSource"
    private val bufferQueue = LinkedBlockingQueue<ByteArray>()
    private var currentChunk: ByteArray? = null
    private var chunkOffset = 0
    private var totalBytesPushed = 0L
    private var isFinished = false

    fun pushData(data: ByteArray) {
        if (data.isNotEmpty()) {
            bufferQueue.put(data)
            totalBytesPushed += data.size
        }
    }

    fun markFinished() {
        isFinished = true
        // Push an empty chunk to unblock any waiting read
        bufferQueue.put(ByteArray(0))
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (size <= 0) return 0

        // MediaPlayer might ask for data at a position we haven't reached yet or have already passed.
        // For a live stream, we generally expect sequential reads.

        var bytesRead = 0
        while (bytesRead < size) {
            if (currentChunk == null || chunkOffset >= currentChunk!!.size) {
                currentChunk = bufferQueue.poll(5, TimeUnit.SECONDS)
                chunkOffset = 0

                if (currentChunk == null) {
                    return if (isFinished) {
                        if (bytesRead > 0) bytesRead else -1
                    } else {
                        // Timeout waiting for data
                        if (bytesRead > 0) bytesRead else 0
                    }
                }

                if (currentChunk!!.isEmpty()) {
                    isFinished = true
                    return if (bytesRead > 0) bytesRead else -1
                }
            }

            val remainingInChunk = currentChunk!!.size - chunkOffset
            val toCopy = minOf(remainingInChunk, size - bytesRead)

            System.arraycopy(currentChunk!!, chunkOffset, buffer, offset + bytesRead, toCopy)
            bytesRead += toCopy
            chunkOffset += toCopy
        }

        return bytesRead
    }

    override fun getSize(): Long {
        // Return -1 for unknown/streaming size
        return -1
    }

    override fun close() {
        bufferQueue.clear()
        isFinished = true
    }
}
