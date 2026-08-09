package com.darkrockstudios.cairn.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * AudioTrack-backed playback on the media stream (sonification usage routes
 * to STREAM_SYSTEM, which most users keep muted via "touch sounds" off — we
 * were silent on real devices). Never requests focus, so playing media is
 * neither paused nor ducked; our short accents simply mix over it. Static
 * tracks are cached per buffer identity and rewound on replay.
 */
internal class AndroidPcmPlayer : PcmPlayer {

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val oneShots = HashMap<FloatArray, AudioTrack>()
    private var loopTrack: AudioTrack? = null

    override fun playOneShot(samples: FloatArray, sampleRate: Int) {
        runCatching {
            val track = oneShots.getOrPut(samples) { buildStatic(samples, sampleRate) }
            track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    override fun startLoop(samples: FloatArray, sampleRate: Int) {
        runCatching {
            stopLoop()
            val track = buildStatic(samples, sampleRate)
            track.setLoopPoints(0, samples.size, -1)
            track.setVolume(0f)
            track.play()
            loopTrack = track
        }
    }

    override fun setLoopGain(gain: Float) {
        runCatching { loopTrack?.setVolume(gain.coerceIn(0f, 1f)) }
    }

    override fun stopLoop() {
        loopTrack?.runCatching {
            stop()
            release()
        }
        loopTrack = null
    }

    override fun unlock() = Unit

    override fun dispose() {
        stopLoop()
        oneShots.values.forEach { it.runCatching { release() } }
        oneShots.clear()
    }

    private fun buildStatic(samples: FloatArray, sampleRate: Int): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * Float.SIZE_BYTES)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        return track
    }
}

@Composable
internal actual fun createPcmPlayer(): PcmPlayer = remember { AndroidPcmPlayer() }
