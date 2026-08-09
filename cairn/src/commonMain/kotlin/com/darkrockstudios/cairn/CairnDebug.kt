package com.darkrockstudios.cairn

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

/**
 * Development aids. Not for production host apps — but harmless if left in:
 * everything here is inert until explicitly poked.
 */
public object CairnDebug {

    /** (rollDegrees, pitchDegrees) or null when no simulation is active. */
    internal val simulatedTilt = mutableStateOf<Offset?>(null)

    /**
     * Feed a simulated device tilt (e.g. from arrow keys in a desktop
     * sample). Roll: positive tips right. Pitch: ~40 is the natural hold;
     * 0 is flat on a table. Overrides the real sensor while set.
     */
    public fun setSimulatedTilt(rollDegrees: Float, pitchDegrees: Float) {
        simulatedTilt.value = Offset(rollDegrees, pitchDegrees)
    }

    public fun clearSimulatedTilt() {
        simulatedTilt.value = null
    }
}
