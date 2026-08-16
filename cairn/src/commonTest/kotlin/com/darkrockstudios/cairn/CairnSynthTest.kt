package com.darkrockstudios.cairn

import com.darkrockstudios.cairn.sound.CairnSynth
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class CairnSynthTest {

    @Test
    fun buffersHaveExpectedDurations() {
        val sr = CairnSynth.SAMPLE_RATE
        assertTrue(CairnSynth.strike.size == (1.15f * sr).toInt())
        assertTrue(CairnSynth.thock.size == (0.1f * sr).toInt())
        assertTrue(CairnSynth.tap.size == (0.18f * sr).toInt())
        assertTrue(CairnSynth.rezIn.size == (1.2f * sr).toInt())
        assertTrue(CairnSynth.rezOut.size == (0.6f * sr).toInt())
        assertTrue(CairnSynth.humPulse.size == (1.0f * sr).toInt())
        assertTrue(CairnSynth.humLoop.size == 5 * sr)
    }

    @Test
    fun nothingClipsAndNothingIsSilent() {
        listOf(
            CairnSynth.strike,
            CairnSynth.thock,
            CairnSynth.tap,
            CairnSynth.rezIn,
            CairnSynth.rezOut,
            CairnSynth.humPulse,
            CairnSynth.humLoop,
        ).forEach { buf ->
            val peak = buf.maxOf { abs(it) }
            assertTrue(peak <= 1f, "clipping: peak $peak")
            assertTrue(peak > 0.005f, "silent buffer: peak $peak")
        }
    }

    @Test
    fun strikeIsLouderThanItsTail() {
        val strike = CairnSynth.strike
        val head = strike.take(strike.size / 4).maxOf { abs(it) }
        val tail = strike.takeLast(strike.size / 20).maxOf { abs(it) }
        assertTrue(head > tail * 5, "strike should decay (head=$head tail=$tail)")
    }

    @Test
    fun humLoopIsSeamless() {
        // 110/164.8/220 Hz all complete integer cycles in exactly 5s, so the
        // loop boundary must be phase-continuous: the first and last samples
        // sit one sample apart on the same waveform.
        val hum = CairnSynth.humLoop
        val expectedStep = abs(hum[1] - hum[0])
        val boundaryStep = abs(hum[0] - hum[hum.size - 1])
        assertTrue(
            boundaryStep <= expectedStep * 3f + 1e-4f,
            "loop discontinuity: boundary=$boundaryStep step=$expectedStep",
        )
    }
}
