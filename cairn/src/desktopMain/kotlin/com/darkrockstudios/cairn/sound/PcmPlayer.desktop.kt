package com.darkrockstudios.cairn.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * One persistent SourceDataLine with software mixing. Per-shot Clips crackle
 * on Linux (each open renegotiates with the mixer), and Clip MASTER_GAIN
 * floors misbehave — so everything mixes into a single always-open stream
 * and gain is plain multiplication.
 */
internal class DesktopPcmPlayer : PcmPlayer {

    private class Shot(val samples: FloatArray) {
        var pos = 0
    }

    private val lock = Any()
    private val shots = ArrayList<Shot>()
    private var loop: FloatArray? = null
    private var loopPos = 0
    private var loopGain = 0f
    private var loopGainTarget = 0f

    @Volatile
    private var running = true

    init {
        Thread(::pump, "CairnAudio").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY - 1
            start()
        }
    }

    private fun pump() {
        // Stereo 16-bit is the universally well-trodden format; mono lines
        // hit rarely-exercised upmix paths on some Linux audio stacks.
        val format = AudioFormat(SAMPLE_RATE.toFloat(), 16, 2, true, false)
        val line: SourceDataLine = runCatching {
            AudioSystem.getSourceDataLine(format).apply {
                // 2 chunks of stereo 16-bit (~46ms): tight enough that sound
                // beats land on their ceremony frames.
                open(format, CHUNK_FRAMES * 4 * 2)
                start()
            }
        }.getOrNull() ?: return

        val mix = FloatArray(CHUNK_FRAMES)
        val out = ByteArray(CHUNK_FRAMES * 4)

        while (running) {
            mix.fill(0f)
            synchronized(lock) {
                val iterator = shots.iterator()
                while (iterator.hasNext()) {
                    val shot = iterator.next()
                    var i = 0
                    while (i < CHUNK_FRAMES && shot.pos < shot.samples.size) {
                        mix[i] += shot.samples[shot.pos]
                        i++
                        shot.pos++
                    }
                    if (shot.pos >= shot.samples.size) iterator.remove()
                }

                // ~23ms smoothing steps keep hum gain changes zipper-free.
                loopGain += (loopGainTarget - loopGain) * 0.25f
                val loopBuf = loop
                if (loopBuf != null) {
                    if (loopGain > 0.0005f) {
                        for (i in 0 until CHUNK_FRAMES) {
                            mix[i] += loopBuf[loopPos] * loopGain
                            loopPos = (loopPos + 1) % loopBuf.size
                        }
                    } else {
                        // Hold phase advancing so re-entry stays seamless.
                        loopPos = (loopPos + CHUNK_FRAMES) % loopBuf.size
                    }
                }
            }

            var j = 0
            for (i in 0 until CHUNK_FRAMES) {
                val v = (mix[i].coerceIn(-1f, 1f) * 32767).toInt()
                val lo = (v and 0xFF).toByte()
                val hi = ((v shr 8) and 0xFF).toByte()
                out[j++] = lo // left
                out[j++] = hi
                out[j++] = lo // right
                out[j++] = hi
            }
            // Blocking write paces the loop at real time.
            line.write(out, 0, out.size)
        }

        runCatching {
            line.drain()
            line.close()
        }
    }

    override fun playOneShot(samples: FloatArray, sampleRate: Int) {
        synchronized(lock) {
            if (shots.size >= MAX_SHOTS) shots.removeAt(0)
            shots.add(Shot(samples))
        }
    }

    override fun startLoop(samples: FloatArray, sampleRate: Int) {
        synchronized(lock) {
            loop = samples
            loopPos = 0
        }
    }

    override fun setLoopGain(gain: Float) {
        synchronized(lock) {
            loopGainTarget = gain.coerceIn(0f, 1f)
        }
    }

    override fun stopLoop() {
        synchronized(lock) {
            loop = null
            loopGainTarget = 0f
            loopGain = 0f
        }
    }

    override fun unlock() = Unit

    override fun dispose() {
        running = false
    }

    private companion object {
        const val SAMPLE_RATE = CairnSynth.SAMPLE_RATE
        const val CHUNK_FRAMES = 1024
        const val MAX_SHOTS = 6
    }
}

@Composable
internal actual fun createPcmPlayer(): PcmPlayer = remember { DesktopPcmPlayer() }
