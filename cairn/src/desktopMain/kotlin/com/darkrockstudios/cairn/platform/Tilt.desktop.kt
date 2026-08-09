package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformTiltEffect(enabled: Boolean, onTilt: (Float, Float) -> Unit) {
    // No orientation source on desktop; hover carries the attractor and
    // CairnDebug.setSimulatedTilt() can drive tilt for development.
}
