package com.darkrockstudios.cairn.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.darkrockstudios.cairn.platform.CairnPrefs
import com.darkrockstudios.cairn.platform.PREF_MUTED
import kotlin.time.TimeSource

/**
 * Cairn's sound façade. Mute persists across opens; thocks are rate-gated at
 * 70ms like the site; the hum's loop gain follows attractor proximity².
 */
internal class SoundEngine(
    private val player: PcmPlayer,
    private val prefs: CairnPrefs,
    soundDefault: Boolean,
) {
    var muted: Boolean by mutableStateOf(
        prefs.getBoolean(PREF_MUTED, default = !soundDefault),
    )
        private set

    private val clock = TimeSource.Monotonic
    private var lastThock = clock.markNow()
    private var loopStarted = false

    fun toggleMuted() {
        muted = !muted
        prefs.putBoolean(PREF_MUTED, muted)
        if (muted) humLevel(0f)
    }

    fun onUserGesture() = player.unlock()

    fun strike() = play(CairnSynth.strike)

    /** Entrance: the interface materializes. */
    fun rezIn() = play(CairnSynth.rezIn)

    /** Exit: it collapses back out. */
    fun rezOut() = play(CairnSynth.rezOut)

    fun thock() {
        if (muted) return
        if (lastThock.elapsedNow().inWholeMilliseconds < 70) return
        lastThock = clock.markNow()
        play(CairnSynth.thock)
    }

    /** A tap on the horizon: the hum blooms fast and dissipates. */
    fun humPulse() = play(CairnSynth.humPulse)

    /** Proximity 0..1; the loudness curve is near². */
    fun humLevel(near: Float) {
        if (muted && near > 0f) return
        if (near > 0f && !loopStarted) {
            player.startLoop(CairnSynth.humLoop, CairnSynth.SAMPLE_RATE)
            loopStarted = true
        }
        if (loopStarted) {
            player.setLoopGain((near * near).coerceIn(0f, 1f))
        }
    }

    fun dispose() = player.dispose()

    private fun play(samples: FloatArray) {
        if (muted) return
        player.playOneShot(samples, CairnSynth.SAMPLE_RATE)
    }
}

internal val LocalSoundEngine = staticCompositionLocalOf<SoundEngine?> { null }

@Composable
internal fun rememberSoundEngine(soundDefault: Boolean): SoundEngine {
    val player = createPcmPlayer()
    val prefs = com.darkrockstudios.cairn.platform.rememberCairnPrefs()
    val engine = remember(player) { SoundEngine(player, prefs, soundDefault) }
    DisposableEffect(engine) {
        onDispose { engine.dispose() }
    }
    return engine
}
