package com.darkrockstudios.cairn

import com.darkrockstudios.cairn.sound.CairnSynth
import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.Test

/**
 * Dumps every synth buffer as WAV files under build/preview/sound/ so the
 * raw synthesis can be auditioned outside the app (aplay, any player) —
 * isolates synth bugs from playback-path bugs.
 */
class WavDumpTest {

    @Test
    fun dumpAllSounds() {
        dump("strike", CairnSynth.strike)
        dump("thock", CairnSynth.thock)
        dump("rez-in", CairnSynth.rezIn)
        dump("rez-out", CairnSynth.rezOut)
        dump("hum-loop", CairnSynth.humLoop)
        dump("hum-pulse", CairnSynth.humPulse)
    }

    private fun dump(name: String, samples: FloatArray) {
        val pcm = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, sample ->
            val v = (sample.coerceIn(-1f, 1f) * 32767).toInt()
            pcm[i * 2] = (v and 0xFF).toByte()
            pcm[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        val format = AudioFormat(CairnSynth.SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val stream = AudioInputStream(
            ByteArrayInputStream(pcm),
            format,
            samples.size.toLong(),
        )
        val out = File("build/preview/sound/$name.wav")
        out.parentFile.mkdirs()
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out)
        println("wrote ${out.absolutePath}")
    }
}
