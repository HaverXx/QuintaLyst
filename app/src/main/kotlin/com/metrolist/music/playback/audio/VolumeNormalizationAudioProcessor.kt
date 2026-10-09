package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

@UnstableApi
class VolumeNormalizationAudioProcessor : BaseAudioProcessor() {
    private var encoding = C.ENCODING_INVALID
    private var bytesPerSample = 0

    @Volatile
    var enabled = false
        set(value) {
            if (field != value) {
                field = value
                Timber.tag(TAG).d("Normalization processor enabled: $value")
            }
        }

    private data class GainState(val targetGainMb: Int, val linearGain: Double)

    @Volatile
    private var currentGain = GainState(0, 1.0)

    @Volatile
    private var boostLinear = 1.0

    @Synchronized
    fun setTargetGain(gainMb: Int) {
        if (currentGain.targetGainMb != gainMb) {
            val linearGain = 10.0.pow(gainMb / 2000.0)
            currentGain = GainState(gainMb, linearGain)
            Timber.tag(TAG).d("Target gain set to $gainMb mB (Linear multiplier: $linearGain)")
        }
    }

    /** Extra user gain in dB applied on top of normalization, independent of [enabled]. */
    fun setBoostDb(db: Float) {
        boostLinear = 10.0.pow(db.coerceIn(0f, MAX_BOOST_DB) / 20.0)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        bytesPerSample =
            when (encoding) {
                C.ENCODING_PCM_16BIT -> 2
                C.ENCODING_PCM_24BIT -> 3
                C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
                else -> throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }

        Timber.tag(TAG).d(
            "Configured: sampleRate=${inputAudioFormat.sampleRate}, channels=${inputAudioFormat.channelCount}, encoding=$encoding",
        )
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val gain = (if (enabled) currentGain.linearGain else 1.0) * boostLinear
        val applyGain = gain != 1.0
        // Only peaks pushed above full scale need limiting; attenuation stays bit-transparent.
        val limit = gain > 1.0
        val sampleCount = inputBuffer.remaining() / bytesPerSample
        val output = replaceOutputBuffer(sampleCount * bytesPerSample)

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        when (encoding) {
            C.ENCODING_PCM_16BIT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getShort()
                    output.putShort(
                        if (applyGain) {
                            (softLimit(sample * gain / 32768.0, limit) * 32768.0)
                                .coerceIn(-32768.0, 32767.0)
                                .toInt()
                                .toShort()
                        } else {
                            sample
                        },
                    )
                }

            C.ENCODING_PCM_24BIT ->
                repeat(sampleCount) {
                    val b0 = inputBuffer.get().toInt() and 0xFF
                    val b1 = inputBuffer.get().toInt() and 0xFF
                    val b2 = inputBuffer.get().toInt()
                    val sample = (b2 shl 16) or (b1 shl 8) or b0
                    val processed =
                        if (applyGain) {
                            (softLimit(sample * gain / 8388608.0, limit) * 8388608.0)
                                .coerceIn(-8388608.0, 8388607.0)
                                .toInt()
                        } else {
                            sample
                        }
                    output.put((processed and 0xFF).toByte())
                    output.put(((processed shr 8) and 0xFF).toByte())
                    output.put(((processed shr 16) and 0xFF).toByte())
                }

            C.ENCODING_PCM_32BIT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getInt()
                    output.putInt(
                        if (applyGain) {
                            (softLimit(sample * gain / 2147483648.0, limit) * 2147483648.0)
                                .coerceIn(-2147483648.0, 2147483647.0)
                                .toLong()
                                .toInt()
                        } else {
                            sample
                        },
                    )
                }

            C.ENCODING_PCM_FLOAT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getFloat()
                    output.putFloat(
                        if (applyGain) {
                            softLimit(sample * gain, limit).toFloat()
                        } else {
                            sample
                        },
                    )
                }
        }

        output.flip()
    }

    override fun onReset() {
        encoding = C.ENCODING_INVALID
        bytesPerSample = 0
    }

    private companion object {
        const val TAG = "VolumeNormalizationProcessor"
        const val MAX_BOOST_DB = 12f
        const val KNEE = 0.8

        /**
         * Soft knee limiter: linear up to [KNEE], then a tanh curve that approaches full scale
         * smoothly. Avoids the harsh distortion of hard clipping when gain pushes peaks past 0 dBFS.
         */
        fun softLimit(x: Double, enabled: Boolean): Double {
            val a = abs(x)
            if (!enabled || a <= KNEE) return x
            val limited = KNEE + (1.0 - KNEE) * tanh((a - KNEE) / (1.0 - KNEE))
            return if (x < 0) -limited else limited
        }
    }
}
