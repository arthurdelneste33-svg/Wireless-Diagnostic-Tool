package com.example.nfc

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * High-tech synthetic audio synthesizer for NFC tag detection.
 * Generates a semi-long (~800ms) futuristic chime sequence with ascending cyber arpeggio
 * and resonating harmonics without requiring external audio asset files.
 */
object NfcAudioSynthesizer {

    private const val SAMPLE_RATE = 44100

    /**
     * Plays a rich, semi-long sci-fi detection chime in the background:
     * - Phase 1: 523 Hz (C5) short chime (120ms)
     * - Phase 2: 659 Hz (E5) ascending note (120ms)
     * - Phase 3: 784 Hz (G5) bright harmonic (160ms)
     * - Phase 4: 1046 Hz (C6) resonating sustained futuristic cyber tail (400ms)
     * Total duration: ~800ms
     */
    fun playDetectionChime() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val totalDurationMs = 800
                val totalSamples = (SAMPLE_RATE * (totalDurationMs / 1000.0)).toInt()
                val pcmData = ShortArray(totalSamples)

                // Frequency timeline points (time in sec, freq in Hz, gain)
                // 0.00s -> 523Hz (C5)
                // 0.12s -> 659Hz (E5)
                // 0.24s -> 784Hz (G5)
                // 0.40s -> 1046Hz (C6) with shimmering harmonic at 2093Hz
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val freq = when {
                        t < 0.12 -> 523.25
                        t < 0.24 -> 659.25
                        t < 0.40 -> 783.99
                        else -> 1046.50
                    }

                    // Harmonic shimmer
                    val shimmerFreq = freq * 2.0

                    // Smooth amplitude envelope with attack and long exponential decay
                    val envelope = when {
                        t < 0.02 -> (t / 0.02) // quick attack
                        t < 0.40 -> 0.85
                        else -> {
                            val decayT = (t - 0.40) / 0.40
                            0.85 * (1.0 - decayT) * (1.0 - decayT) // quadratic decay
                        }
                    }

                    // Composite waveform: main sine + 20% harmonic sparkle
                    val wave = 0.8 * sin(2.0 * Math.PI * freq * t) + 0.2 * sin(2.0 * Math.PI * shimmerFreq * t)
                    val sampleVal = (wave * envelope * Short.MAX_VALUE * 0.75).toInt().coerceIn(-32768, 32767)
                    pcmData[i] = sampleVal.toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmData.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcmData, 0, pcmData.size)
                audioTrack.play()

                // Wait for playback then release
                Thread.sleep(totalDurationMs.toLong() + 100L)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio hardware errors gracefully
            }
        }
    }
}
