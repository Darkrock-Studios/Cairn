package com.darkrockstudios.cairn.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.khronos.webgl.Float32Array
import org.khronos.webgl.set

// Minimal WebAudio externals — just what Cairn touches.
internal external class AudioContext {
    val destination: AudioNode
    val state: String
    val currentTime: Double
    fun resume()
    fun createGain(): GainNode
    fun createBufferSource(): AudioBufferSourceNode
    fun createBuffer(numberOfChannels: Int, length: Int, sampleRate: Int): AudioBuffer
}

internal open external class AudioNode {
    fun connect(destination: AudioNode): AudioNode
    fun disconnect()
}

internal external class GainNode : AudioNode {
    val gain: AudioParam
}

internal external class AudioParam {
    var value: Float
    fun setTargetAtTime(target: Float, startTime: Double, timeConstant: Double)
}

internal external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?
    var loop: Boolean
    fun start()
    fun stop()
}

internal external class AudioBuffer {
    fun copyToChannel(source: Float32Array, channelNumber: Int)
}

/**
 * WebAudio playback. Browsers refuse audio before a user gesture, so the
 * context is created/resumed only inside [unlock] (wired to pointer-down);
 * anything played before then is silently skipped — including the very
 * first entrance boom, matching the website's behavior.
 */
internal class WasmPcmPlayer : PcmPlayer {

    private var context: AudioContext? = null
    private var loopSource: AudioBufferSourceNode? = null
    private var loopGain: GainNode? = null
    private var pendingLoop: Pair<FloatArray, Int>? = null

    override fun unlock() {
        val ctx = context ?: runCatching { AudioContext() }.getOrNull()?.also { context = it }
        ctx ?: return
        if (ctx.state == "suspended") runCatching { ctx.resume() }
        pendingLoop?.let { (samples, rate) ->
            pendingLoop = null
            startLoop(samples, rate)
        }
    }

    override fun playOneShot(samples: FloatArray, sampleRate: Int) {
        val ctx = readyContext() ?: return
        runCatching {
            val source = ctx.createBufferSource()
            source.buffer = toBuffer(ctx, samples, sampleRate)
            source.connect(ctx.destination)
            source.start()
        }
    }

    override fun startLoop(samples: FloatArray, sampleRate: Int) {
        val ctx = readyContext()
        if (ctx == null) {
            pendingLoop = samples to sampleRate
            return
        }
        runCatching {
            stopLoop()
            val gain = ctx.createGain()
            gain.gain.value = 0f
            gain.connect(ctx.destination)
            val source = ctx.createBufferSource()
            source.buffer = toBuffer(ctx, samples, sampleRate)
            source.loop = true
            source.connect(gain)
            source.start()
            loopSource = source
            loopGain = gain
        }
    }

    override fun setLoopGain(gain: Float) {
        val ctx = context ?: return
        loopGain?.gain?.setTargetAtTime(gain.coerceIn(0f, 1f), ctx.currentTime, 0.06)
    }

    override fun stopLoop() {
        runCatching {
            loopSource?.stop()
            loopSource?.disconnect()
            loopGain?.disconnect()
        }
        loopSource = null
        loopGain = null
    }

    override fun dispose() = stopLoop()

    private fun readyContext(): AudioContext? {
        val ctx = context ?: return null
        return if (ctx.state == "running") ctx else null
    }

    private fun toBuffer(ctx: AudioContext, samples: FloatArray, sampleRate: Int): AudioBuffer {
        val f32 = Float32Array(samples.size)
        samples.forEachIndexed { i, v -> f32[i] = v }
        val buffer = ctx.createBuffer(1, samples.size, sampleRate)
        buffer.copyToChannel(f32, 0)
        return buffer
    }
}

@Composable
internal actual fun createPcmPlayer(): PcmPlayer = remember { WasmPcmPlayer() }
