package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.darkrockstudios.cairn.CairnDebug

/**
 * Platform tilt feed: reports device orientation as (rollDegrees,
 * pitchDegrees) using the web deviceorientation conventions the prototype
 * was tuned against — roll (gamma): positive when the right edge dips;
 * pitch (beta): 0 flat on a table, ~90 upright.
 *
 * No-op where the platform has no orientation source (desktop) or when
 * [enabled] is false (a simulated tilt is active).
 */
@Composable
internal expect fun PlatformTiltEffect(enabled: Boolean, onTilt: (Float, Float) -> Unit)

/** Simulated tilt (CairnDebug) beats the platform sensor. */
@Composable
internal fun TiltSource(onTilt: (Float, Float) -> Unit) {
    val simulated by CairnDebug.simulatedTilt
    LaunchedEffect(simulated) {
        simulated?.let { onTilt(it.x, it.y) }
    }
    PlatformTiltEffect(enabled = simulated == null, onTilt = onTilt)
}

/**
 * The prototype's smoothing + normalization: exponential low-pass
 * (0.7 old / 0.3 new), roll normalized over ±25°, pitch around the natural
 * ~40° hold over ±25°.
 */
internal class TiltFilter {
    private var roll = 0f
    private var pitch = 40f

    fun update(rollDegrees: Float, pitchDegrees: Float): Pair<Float, Float> {
        roll = roll * 0.7f + rollDegrees * 0.3f
        pitch = pitch * 0.7f + pitchDegrees * 0.3f
        val nx = (roll / 25f).coerceIn(-1f, 1f)
        val ny = ((pitch - 40f) / 25f).coerceIn(-1f, 1f)
        return nx to ny
    }
}
