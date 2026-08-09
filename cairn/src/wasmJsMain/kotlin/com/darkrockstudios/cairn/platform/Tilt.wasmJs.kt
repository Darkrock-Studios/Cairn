package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@JsFun("(cb) => window.addEventListener('deviceorientation', (e) => cb(e.gamma ?? 0, e.beta ?? 0))")
private external fun listenDeviceOrientation(cb: (Double, Double) -> Unit)

// The listener is registered once per page and can't be removed (removal
// would need the same JS function reference); the module-level callback slot
// makes re-composition and disposal safe anyway.
private var tiltCallback: ((Float, Float) -> Unit)? = null
private var listening = false

@Composable
internal actual fun PlatformTiltEffect(enabled: Boolean, onTilt: (Float, Float) -> Unit) {
    DisposableEffect(enabled) {
        if (enabled) {
            tiltCallback = onTilt
            if (!listening) {
                listening = true
                // iOS Safari would need a user-gesture permission request;
                // v1 degrades to hover/touch there by never receiving events.
                listenDeviceOrientation { gamma, beta ->
                    tiltCallback?.invoke(gamma.toFloat(), beta.toFloat())
                }
            }
        }
        onDispose {
            if (tiltCallback === onTilt) tiltCallback = null
        }
    }
}
