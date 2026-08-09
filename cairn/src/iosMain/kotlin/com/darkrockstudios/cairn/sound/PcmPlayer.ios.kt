package com.darkrockstudios.cairn.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatFloat32
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive

/**
 * AVAudioEngine playback. Session category Ambient: respects the silent
 * switch and mixes with other audio instead of interrupting it. A small
 * rotation of player nodes lets one-shots overlap.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosPcmPlayer : PcmPlayer {

    private val engine = AVAudioEngine()
    private val format = AVAudioFormat(
        commonFormat = AVAudioPCMFormatFloat32,
        sampleRate = 44100.0,
        channels = 1u,
        interleaved = false,
    )
    private val shotNodes = List(3) { AVAudioPlayerNode() }
    private var shotIndex = 0
    private val loopNode = AVAudioPlayerNode()
    private var started = false

    private fun ensureStarted(): Boolean {
        if (started) return true
        return runCatching {
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, error = null)
            AVAudioSession.sharedInstance().setActive(true, error = null)
            (shotNodes + loopNode).forEach { node ->
                engine.attachNode(node)
                engine.connect(node, engine.mainMixerNode, format)
            }
            engine.startAndReturnError(null)
            started = true
        }.isSuccess
    }

    override fun playOneShot(samples: FloatArray, sampleRate: Int) {
        if (!ensureStarted()) return
        runCatching {
            val buffer = toBuffer(samples) ?: return
            val node = shotNodes[shotIndex]
            shotIndex = (shotIndex + 1) % shotNodes.size
            if (!node.playing) node.play()
            node.scheduleBuffer(buffer, null)
        }
    }

    override fun startLoop(samples: FloatArray, sampleRate: Int) {
        if (!ensureStarted()) return
        runCatching {
            val buffer = toBuffer(samples) ?: return
            loopNode.volume = 0f
            if (!loopNode.playing) loopNode.play()
            loopNode.scheduleBuffer(buffer, null, AVAudioPlayerNodeBufferLoops, null)
        }
    }

    override fun setLoopGain(gain: Float) {
        loopNode.volume = gain.coerceIn(0f, 1f)
    }

    override fun stopLoop() {
        runCatching { loopNode.stop() }
    }

    override fun unlock() = Unit

    override fun dispose() {
        runCatching {
            (shotNodes + loopNode).forEach { it.stop() }
            engine.stop()
        }
        started = false
    }

    private fun toBuffer(samples: FloatArray): AVAudioPCMBuffer? {
        val buffer = AVAudioPCMBuffer(format, samples.size.toUInt())
        val channel = buffer.floatChannelData?.get(0) ?: return null
        samples.forEachIndexed { i, v -> channel[i] = v }
        buffer.frameLength = samples.size.toUInt()
        return buffer
    }
}

@Composable
internal actual fun createPcmPlayer(): PcmPlayer = remember { IosPcmPlayer() }
