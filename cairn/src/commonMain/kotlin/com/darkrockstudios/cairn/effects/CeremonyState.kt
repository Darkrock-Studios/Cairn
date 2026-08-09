package com.darkrockstudios.cairn.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import com.darkrockstudios.cairn.CairnEntrance
import kotlin.math.pow

private val EtchEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val DeEtchEasing = CubicBezierEasing(0.55f, 0f, 0.8f, 0.4f)

/**
 * The entrance ("the survey") and exit ("the retreat") state machine,
 * timed against the rez-in sound's mechanical beats: head-engage at 120ms
 * (etch underway), read ticks through the etch/flood, and the LOAD kachunk
 * at 570ms — where the basalt lands and the summit ignites.
 *
 * Enter: etch-v 0–330, etch-h 100–430, flood 260–560, about-in 420–650,
 *        element ignites 570…840 (+400ms each), etch settle 700–1000.
 * Exit:  power-down fade+flare 0–280, de-etch h 100–500 / v 220–620,
 *        drain 260–640. Exits are faster: the user already decided to leave.
 *
 * Quick collapses to flood+ignite (~700ms); None (or reduced motion) is a
 * 200ms fade.
 */
internal class CeremonyState(
    mode: CairnEntrance,
    reducedMotion: Boolean,
) {
    enum class Phase { Hidden, Entering, Open, Leaving }

    val effectiveMode: CairnEntrance =
        if (reducedMotion) CairnEntrance.None else mode

    var phase by mutableStateOf(Phase.Hidden)
        private set

    /** Milliseconds since the current phase began. */
    private var clock by mutableFloatStateOf(0f)

    private val enterTotal: Float = when (effectiveMode) {
        CairnEntrance.Full -> 1250f
        CairnEntrance.Quick -> 700f
        CairnEntrance.None -> 200f
    }
    private val exitTotal: Float = when (effectiveMode) {
        CairnEntrance.None -> 200f
        else -> 700f
    }

    /** The horizon foil's sweep delay, synchronized to the ceremony. */
    val foilSweepDelayMillis: Int = when (effectiveMode) {
        CairnEntrance.Full -> 800
        CairnEntrance.Quick -> 300
        CairnEntrance.None -> 0
    }

    suspend fun runEnter() {
        if (phase == Phase.Entering || phase == Phase.Open) return
        phase = Phase.Entering
        runClock(enterTotal)
        phase = Phase.Open
    }

    suspend fun runExit() {
        if (phase == Phase.Leaving || phase == Phase.Hidden) return
        phase = Phase.Leaving
        runClock(exitTotal)
        phase = Phase.Hidden
    }

    private suspend fun runClock(totalMillis: Float) {
        clock = 0f
        val start = withFrameNanos { it }
        while (clock < totalMillis) {
            withFrameNanos { now ->
                clock = (now - start) / 1_000_000f
            }
        }
    }

    // ---- derived values; all read [clock]/[phase] so callers subscribe ----

    private fun ramp(start: Float, duration: Float, easing: (Float) -> Float = { it }): Float =
        easing(((clock - start) / duration).coerceIn(0f, 1f))

    fun scrimAlpha(): Float = when (phase) {
        Phase.Hidden -> 0f
        Phase.Open -> 1f
        Phase.Entering -> when (effectiveMode) {
            CairnEntrance.Full -> ramp(260f, 300f)
            CairnEntrance.Quick -> ramp(0f, 150f)
            CairnEntrance.None -> ramp(0f, 200f)
        }
        Phase.Leaving -> when (effectiveMode) {
            CairnEntrance.None -> 1f - ramp(0f, 200f)
            else -> 1f - ramp(260f, 380f)
        }
    }

    fun aboutAlpha(): Float = when (phase) {
        Phase.Hidden -> 0f
        Phase.Open -> 1f
        Phase.Entering -> when (effectiveMode) {
            CairnEntrance.Full -> ramp(420f, 230f)
            CairnEntrance.Quick -> ramp(0f, 250f)
            CairnEntrance.None -> ramp(0f, 200f)
        }
        Phase.Leaving -> when (effectiveMode) {
            CairnEntrance.None -> 1f - ramp(0f, 200f)
            else -> 1f - ramp(0f, 280f)
        }
    }

    /** White flare on the content as it powers down. */
    fun aboutFlare(): Float =
        if (phase == Phase.Leaving && effectiveMode != CairnEntrance.None) {
            ramp(0f, 280f) * 0.25f
        } else {
            0f
        }

    /** Reveal fraction of the vertical etch lines (left→right). */
    fun etchVClip(): Float = when {
        effectiveMode != CairnEntrance.Full -> 0f
        phase == Phase.Entering -> ramp(0f, 330f) { EtchEasing.transform(it) }
        phase == Phase.Leaving -> 1f - ramp(220f, 400f) { DeEtchEasing.transform(it) }
        else -> 0f
    }

    /** Reveal fraction of the horizontal etch lines (top→bottom). */
    fun etchHClip(): Float = when {
        effectiveMode != CairnEntrance.Full -> 0f
        phase == Phase.Entering -> ramp(100f, 330f) { EtchEasing.transform(it) }
        phase == Phase.Leaving -> 1f - ramp(100f, 400f) { DeEtchEasing.transform(it) }
        else -> 0f
    }

    fun etchAlpha(): Float = when {
        effectiveMode != CairnEntrance.Full -> 0f
        phase == Phase.Entering -> 1f - ramp(700f, 300f)
        phase == Phase.Leaving -> {
            // Lines pop back and hold until 75% of their retreat, then fade.
            val h = ramp(100f, 400f)
            val v = ramp(220f, 400f)
            val late = maxOf(h, v)
            if (late < 0.75f) 1f else 1f - (late - 0.75f) / 0.25f
        }
        else -> 0f
    }

    /**
     * The load-kachunk grid surge: the whole grid flashes bright on the
     * sound's BONG (560ms) and decays as the interface blooms.
     */
    fun gridSurge(): Float {
        if (effectiveMode != CairnEntrance.Full || phase != Phase.Entering) return 0f
        val t = clock - 560f
        return when {
            t < 0f -> 0f
            t < 60f -> t / 60f
            else -> (1f - ((t - 60f) / 400f).coerceIn(0f, 1f)).pow(1.7f)
        }
    }

    /**
     * Seed for the surge's ragged flicker: re-rolls every ~45ms (~22Hz) so
     * per-line brightness jitters like unstable current, not a clean flash.
     */
    fun surgeSeed(): Int = (clock / 45f).toInt()

    /** Per-element ignite: 0 before its cue, eased 0→1 over 500ms after it. */
    fun igniteProgress(delayMillis: Int): Float = when (phase) {
        Phase.Hidden -> 0f
        Phase.Open, Phase.Leaving -> 1f
        Phase.Entering -> when (effectiveMode) {
            CairnEntrance.None -> 1f
            CairnEntrance.Quick -> {
                val quickDelay = 100f + (delayMillis - 550f).coerceAtLeast(0f) * 0.4f
                ramp(quickDelay, 350f)
            }
            CairnEntrance.Full -> ramp(delayMillis.toFloat(), 400f)
        }
    }
}

/** Ignite cue sheet, milliseconds into the full entrance. */
internal object IgniteCues {
    const val SUMMIT = 570
    const val TITLE = 615
    const val MISSION = 655
    const val ETHOS = 695
    const val CHIPS = 735
    const val SECTIONS = 800
    const val FOOTER = 840
}

internal val LocalCeremony = staticCompositionLocalOf<CeremonyState?> { null }
