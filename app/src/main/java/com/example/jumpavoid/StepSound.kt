package com.example.jumpavoid

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*

class StepSound {

    private val sampleRate = 44100
    private val durationMs = 35
    private val numSamples = sampleRate * durationMs / 1000  // 1543 samples

    private val audioTrack: AudioTrack = run {
        // Synthesize a light crisp tap: decaying sine wave at 400Hz
        val buffer = ShortArray(numSamples) { i ->
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 90.0)          // very fast exponential decay
            val wave = sin(2.0 * PI * 400.0 * t)   // 400Hz light tap tone
            (wave * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(numSamples * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, numSamples)
        track
    }

    /** Play the step sound. Safe to call from the game thread. */
    fun play() {
        if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
            audioTrack.stop()
            audioTrack.setPlaybackHeadPosition(0)
            audioTrack.play()
        }
    }

    /** Call from surfaceDestroyed after the game thread has joined. */
    fun release() {
        audioTrack.release()
    }
}
