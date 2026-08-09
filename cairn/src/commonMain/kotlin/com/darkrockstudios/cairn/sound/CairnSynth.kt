package com.darkrockstudios.cairn.sound

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure-Kotlin PCM port of the darkrockstudios.com synth (app.js `sound`):
 * pure sine voices with exponential frequency sweeps and exponential gain
 * envelopes, rendered at 44.1kHz mono. Buffers are lazy and cached; every
 * platform plays the exact same samples.
 */
internal object CairnSynth {
    const val SAMPLE_RATE = 44100

    /** Master gain baked into the samples. The site uses 0.12, but that
     * proved near-inaudible through native audio paths. */
    private const val MASTER = 0.3f

    /** The harmonic triad shared by the strike ring-out and the hum. */
    private val TRIAD = floatArrayOf(110f, 164.8f, 220f)

    /** Hum loudness. The site's 0.17 was near-inaudible on real speakers. */
    private const val HUM_LEVEL = 0.55f

    /** Summit strike: deep boom attack + harmonic ring-out. */
    val strike: FloatArray by lazy {
        render(1.15f) { buf ->
            addVoice(
                buf,
                freqStart = 90f, freqEnd = 32f, freqRampSec = 0.35f,
                gain = listOf(0f to 0.65f, 0.4f to 0.001f),
                stopSec = 0.45f,
                harmonics = 0.6f,
            )
            TRIAD.forEachIndexed { i, freq ->
                addVoice(
                    buf,
                    freqStart = freq, freqEnd = freq, freqRampSec = 0f,
                    gain = listOf(0f to 0.0001f, 0.12f to 0.19f / (i + 1), 1.1f to 0.0001f),
                    stopSec = 1.15f,
                    harmonics = 0.5f,
                )
            }
        }
    }

    /** Seam crossings, card taps: a short falling knock. */
    val thock: FloatArray by lazy {
        render(0.1f) { buf ->
            addVoice(
                buf,
                freqStart = 190f, freqEnd = 70f, freqRampSec = 0.08f,
                gain = listOf(0f to 0.5f, 0.09f to 0.001f),
                stopSec = 0.1f,
            )
        }
    }

    /**
     * Entrance "rez in": the floppy boot. Modeled on a real drive read —
     * motor hiss spins up, the head ENGAGES (kachunk), it reads (discrete
     * mechanical ticks, ~90-170ms apart, over a drone growing underneath),
     * then a second kachunk LOADS the software: mechanics fall silent and
     * the clean drone stands on, holding to the end. Turning on, in front
     * of you, doing work.
     */
    val rezIn: FloatArray by lazy {
        val lock = 0.57f
        render(1.2f) { buf ->
            // Motor hiss: spins up fast, runs while reading, drops to a
            // whisper once loaded.
            addNoise(
                buf,
                gain = listOf(
                    0f to 0.0001f, 0.08f to 0.02f, lock to 0.024f,
                    lock + 0.07f to 0.006f, 1.2f to 0.0001f,
                ),
                stopSec = 1.2f,
                cutoffAlpha = 0.2f,
            )
            // Head engage, reading ticks, and the load.
            val mech = Random(0xF109)
            addTick(buf, atSec = 0.12f, strength = 0.5f, durSec = 0.045f, random = mech)
            floatArrayOf(0.26f, 0.36f, 0.48f).forEach { at ->
                addTick(
                    buf,
                    atSec = at + (mech.nextFloat() - 0.5f) * 0.03f,
                    strength = 0.16f + mech.nextFloat() * 0.12f,
                    durSec = 0.02f,
                    random = mech,
                )
            }
            addTick(buf, atSec = lock, strength = 0.62f, durSec = 0.055f, random = mech)

            // The drone: grows quietly under the read, bumps at the load,
            // then holds — it is ON.
            listOf(
                Triple(55.1f, 0.36f, 0.4f),
                Triple(110.2f, 0.46f, 0.5f),
                Triple(165.3f, 0.26f, 0.5f),
                Triple(220.4f, 0.16f, 0.4f),
            ).forEach { (freq, peak, h) ->
                addVoice(
                    buf,
                    freqStart = freq, freqEnd = freq, freqRampSec = 0f,
                    gain = listOf(
                        0f to 0.0001f,
                        0.22f to 0.0001f,
                        lock - 0.02f to peak * 0.5f,
                        lock + 0.06f to peak * 1.12f, // the load clicks it into place
                        lock + 0.18f to peak,
                        1.05f to peak * 0.92f,        // holds — presence, not passage
                        1.2f to 0.002f,               // short settle, not a recede
                    ),
                    stopSec = 1.2f,
                    harmonics = h,
                )
            }
        }
    }

    /**
     * Exit "rez out": the eject — the boot played backwards. The drone is
     * ON at the cut, the EJECT kachunk fires almost immediately, and the
     * motor spins down: the whine falls a full octave while the hiss dies,
     * with a couple of small mechanical settles on the way to silence.
     */
    val rezOut: FloatArray by lazy {
        render(0.6f) { buf ->
            // Motor hiss: at running level when the eject hits, gone by 0.45.
            addNoise(
                buf,
                gain = listOf(0f to 0.02f, 0.1f to 0.018f, 0.45f to 0.0001f),
                stopSec = 0.5f,
                cutoffAlpha = 0.2f,
            )
            // The EJECT, then the carriage settling.
            val mech = Random(0xE0EC)
            addTick(buf, atSec = 0.05f, strength = 0.68f, durSec = 0.055f, random = mech)
            addTick(buf, atSec = 0.27f, strength = 0.18f, durSec = 0.022f, random = mech)
            addTick(buf, atSec = 0.4f, strength = 0.1f, durSec = 0.018f, random = mech)

            // Spin-down whine: the boot's drone, dropping an octave into
            // nothing. Upper voices carry it on small speakers.
            listOf(
                Triple(110.2f, 0.4f, 0.7f),
                Triple(165.3f, 0.3f, 0.7f),
                Triple(220.4f, 0.22f, 0.6f),
                Triple(330.6f, 0.1f, 0.4f),
            ).forEach { (freq, peak, h) ->
                addVoice(
                    buf,
                    freqStart = freq, freqEnd = freq * 0.5f, freqRampSec = 0.42f,
                    gain = listOf(
                        0f to peak,            // running when the plug pulls
                        0.09f to peak * 0.9f,  // survives the eject kachunk
                        0.3f to peak * 0.35f,
                        0.52f to 0.0001f,
                    ),
                    stopSec = 0.58f,
                    harmonics = h,
                    flutter = 0.2f,
                )
            }
        }
    }

    /**
     * The proximity hum: the triad at full level, as a seamless 5.0s loop.
     * 5s is exact because 110, 164.8, and 220 Hz all complete integer cycle
     * counts in 5 seconds (550 / 824 / 1100). Playback gain = proximity².
     */
    val humLoop: FloatArray by lazy {
        render(5.0f) { buf ->
            TRIAD.forEachIndexed { i, freq ->
                val weight = 1f / (i + 1)
                addVoice(
                    buf,
                    freqStart = freq, freqEnd = freq, freqRampSec = 0f,
                    gain = listOf(0f to HUM_LEVEL * weight),
                    stopSec = 5.0f,
                    harmonics = 0.6f,
                )
            }
        }
    }

    /**
     * The touch hum: a tap on the horizon blooms the triad in fast (~80ms)
     * and lets it dissipate over the next second. A one-shot, so touch
     * platforms get the hum's character without needing proximity.
     */
    val humPulse: FloatArray by lazy {
        render(1.0f) { buf ->
            TRIAD.forEachIndexed { i, freq ->
                val weight = 1f / (i + 1)
                addVoice(
                    buf,
                    freqStart = freq, freqEnd = freq, freqRampSec = 0f,
                    gain = listOf(
                        0f to 0.0001f,
                        0.08f to HUM_LEVEL * 1.15f * weight,
                        1.0f to 0.0001f,
                    ),
                    stopSec = 1.0f,
                    harmonics = 0.6f,
                )
            }
        }
    }

    private inline fun render(durationSec: Float, block: (FloatArray) -> Unit): FloatArray {
        val buf = FloatArray((durationSec * SAMPLE_RATE).toInt())
        block(buf)
        return buf
    }

    /**
     * Adds one sine voice: exponential frequency sweep over [freqRampSec],
     * then constant; gain follows exponential ramps between [gain] points
     * (WebAudio `exponentialRampToValueAtTime` semantics).
     *
     * @param harmonics 0 = pure sine. Otherwise adds 2nd and 3rd partials at
     *   this ratio (and its square), normalized to keep peak level — puts
     *   energy above small-speaker rolloff without changing the note.
     */
    private fun addVoice(
        buf: FloatArray,
        freqStart: Float,
        freqEnd: Float,
        freqRampSec: Float,
        gain: List<Pair<Float, Float>>,
        stopSec: Float,
        harmonics: Float = 0f,
        /** 0 = steady. Otherwise irregular amplitude wobble at this depth —
         * two non-commensurate LFOs, so it throbs instead of pulsing. */
        flutter: Float = 0f,
    ) {
        val dt = 1.0 / SAMPLE_RATE
        val stop = minOf((stopSec * SAMPLE_RATE).toInt(), buf.size)
        var phase = 0.0
        val freqRatio = (freqEnd / freqStart).toDouble()
        val h2 = harmonics.toDouble()
        val h3 = (harmonics * harmonics).toDouble()
        val norm = 1.0 / (1.0 + h2 + h3)

        for (i in 0 until stop) {
            val t = (i * dt).toFloat()
            val freq = if (freqRampSec > 0f && t < freqRampSec) {
                freqStart * freqRatio.pow((t / freqRampSec).toDouble()).toFloat()
            } else if (freqRampSec > 0f) {
                freqEnd
            } else {
                freqStart
            }
            phase += 2.0 * PI * freq * dt
            var wave = (sin(phase) + h2 * sin(2.0 * phase) + h3 * sin(3.0 * phase)) * norm
            if (flutter > 0f) {
                val wobble = 0.6 * sin(2.0 * PI * 7.3 * t) + 0.4 * sin(2.0 * PI * 13.1 * t)
                wave *= 1.0 + flutter * wobble
            }
            buf[i] += (wave * gainAt(gain, t) * MASTER).toFloat()
        }
    }

    /**
     * A mechanical tick/kachunk: a bright damped noise burst over a small
     * falling thump (200→110Hz), the anatomy of a drive-head event pulled
     * from a real floppy recording — mid-heavy burst, ~8ms attack shape,
     * fast polynomial decay.
     */
    private fun addTick(
        buf: FloatArray,
        atSec: Float,
        strength: Float,
        durSec: Float,
        random: Random,
    ) {
        val start = (atSec * SAMPLE_RATE).toInt()
        val dur = (durSec * SAMPLE_RATE).toInt()
        val dt = 1.0 / SAMPLE_RATE
        var noise = 0f
        var phase = 0.0
        for (i in 0 until dur) {
            val index = start + i
            if (index >= buf.size) break
            val t = i.toFloat() / dur
            // Sharp rise over the first ~15%, then polynomial decay.
            val envelope = if (t < 0.15f) t / 0.15f else ((1f - t) / 0.85f).pow(2.2f)
            val x = random.nextFloat() * 2f - 1f
            noise += 0.45f * (x - noise)
            val thumpFreq = 200.0 - 90.0 * t
            phase += 2.0 * PI * thumpFreq * dt
            val sample = (noise * 0.8f + sin(phase).toFloat() * 0.55f) * envelope * strength
            buf[index] += sample * MASTER
        }
    }

    /**
     * A lowpassed white-noise layer — electrical static. [cutoffAlpha] is a
     * one-pole coefficient (~0.12 lands near 900Hz): higher = hissier.
     * Seeded so buffers stay deterministic for the tests.
     */
    private fun addNoise(
        buf: FloatArray,
        gain: List<Pair<Float, Float>>,
        stopSec: Float,
        cutoffAlpha: Float = 0.12f,
    ) {
        val dt = 1.0 / SAMPLE_RATE
        val stop = minOf((stopSec * SAMPLE_RATE).toInt(), buf.size)
        val random = Random(0xCA1214)
        var y = 0f
        for (i in 0 until stop) {
            val t = (i * dt).toFloat()
            val x = random.nextFloat() * 2f - 1f
            y += cutoffAlpha * (x - y)
            buf[i] += y * gainAt(gain, t) * MASTER * 3f
        }
    }

    private fun gainAt(points: List<Pair<Float, Float>>, t: Float): Float {
        if (points.size == 1) return points[0].second
        var previous = points[0]
        for (index in 1 until points.size) {
            val next = points[index]
            if (t <= next.first) {
                val (t0, v0) = previous
                val (t1, v1) = next
                if (t1 - t0 <= 0f) return v1
                val frac = ((t - t0) / (t1 - t0)).coerceIn(0f, 1f)
                return v0 * (v1 / v0).pow(frac)
            }
            previous = next
        }
        return points.last().second
    }
}
