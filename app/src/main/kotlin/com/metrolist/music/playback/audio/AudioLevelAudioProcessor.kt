/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

object AudioLevelMeter {
    const val SLICE_NANOS = 10_000_000L
    private const val SLOTS = 256
    private const val MASK = 255L
    private const val OUTPUT_DELAY_NANOS = 190_000_000L
    private const val MAX_AGE_NANOS = 150_000_000L

    @Volatile
    var enabled = false

    private val times = LongArray(SLOTS)
    private val levels = FloatArray(SLOTS)
    private val lows = FloatArray(SLOTS)

    @Volatile
    private var head = 0L
    private var lastStamp = 0L

    @Synchronized
    fun push(
        level: Float,
        low: Float,
        nanos: Long,
    ) {
        val stamp = maxOf(nanos, lastStamp + SLICE_NANOS)
        lastStamp = stamp
        val index = (head and MASK).toInt()
        times[index] = stamp
        levels[index] = level
        lows[index] = low
        head += 1
    }

    fun read(
        out: FloatArray,
        nowNanos: Long,
    ) {
        val target = nowNanos - OUTPUT_DELAY_NANOS
        val end = head
        val limit = maxOf(0L, end - SLOTS)
        var i = end - 1
        while (i >= limit && times[(i and MASK).toInt()] > target) i--
        val index = (i and MASK).toInt()
        if (i >= limit && target - times[index] < MAX_AGE_NANOS) {
            out[0] = levels[index]
            out[1] = lows[index]
        } else {
            out[0] = 0f
            out[1] = 0f
        }
    }
}

@UnstableApi
class AudioLevelAudioProcessor : BaseAudioProcessor() {
    private var channelCount = 0
    private var bytesPerSample = 2
    private var isFloat = false
    private var analysable = false
    private var alpha = 0f
    private var windowFrames = 441
    private var lowPass = 0f
    private var sumSquares = 0f
    private var sumLow = 0f
    private var framesInWindow = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        analysable = isFloat || inputAudioFormat.encoding == C.ENCODING_PCM_16BIT
        bytesPerSample = if (isFloat) 4 else 2
        channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        alpha = (1.0 - exp(-2.0 * PI * LOW_CUTOFF_HZ / sampleRate)).toFloat()
        windowFrames = (sampleRate / 100).coerceAtLeast(1)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        if (AudioLevelMeter.enabled && analysable && channelCount > 0) analyse(inputBuffer)
        replaceOutputBuffer(remaining).apply {
            put(inputBuffer)
            flip()
        }
    }

    private fun analyse(buffer: ByteBuffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = bytesPerSample * channelCount
        val frames = buffer.remaining() / frameBytes
        val now = System.nanoTime()
        var index = buffer.position()
        var slice = 0
        repeat(frames) {
            val first = readSample(buffer, index)
            val mono = if (channelCount > 1) (first + readSample(buffer, index + bytesPerSample)) * 0.5f else first
            index += frameBytes
            lowPass += alpha * (mono - lowPass)
            sumSquares += mono * mono
            sumLow += lowPass * lowPass
            framesInWindow++
            if (framesInWindow >= windowFrames) {
                AudioLevelMeter.push(
                    sqrt(sumSquares / framesInWindow),
                    sqrt(sumLow / framesInWindow),
                    now + slice * AudioLevelMeter.SLICE_NANOS,
                )
                slice++
                sumSquares = 0f
                sumLow = 0f
                framesInWindow = 0
            }
        }
    }

    private fun readSample(
        buffer: ByteBuffer,
        index: Int,
    ): Float = if (isFloat) buffer.getFloat(index) else buffer.getShort(index) / 32768f

    private fun resetWindow() {
        lowPass = 0f
        sumSquares = 0f
        sumLow = 0f
        framesInWindow = 0
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) = resetWindow()

    override fun onReset() {
        channelCount = 0
        analysable = false
        resetWindow()
    }

    private companion object {
        const val LOW_CUTOFF_HZ = 160.0
    }
}
