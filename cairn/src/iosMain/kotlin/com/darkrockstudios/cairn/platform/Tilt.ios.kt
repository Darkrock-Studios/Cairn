package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * CMMotionManager gravity → (roll, pitch) in web deviceorientation
 * conventions. CoreMotion's gravity vector points toward the earth, so
 * world-up in device coordinates is its negation.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformTiltEffect(enabled: Boolean, onTilt: (Float, Float) -> Unit) {
    val manager = remember { CMMotionManager() }
    DisposableEffect(enabled, manager) {
        if (!enabled || !manager.deviceMotionAvailable) {
            return@DisposableEffect onDispose {}
        }

        manager.deviceMotionUpdateInterval = 1.0 / 30.0
        manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            val gravity = motion?.gravity ?: return@startDeviceMotionUpdatesToQueue
            gravity.useContents {
                val ux = -x
                val uy = -y
                val uz = -z
                val rollDeg = degrees(atan2(-ux, sqrt(uy * uy + uz * uz)))
                val pitchDeg = degrees(atan2(uy, uz))
                onTilt(rollDeg, pitchDeg)
            }
        }

        onDispose { manager.stopDeviceMotionUpdates() }
    }
}

private fun degrees(radians: Double): Float = (radians * 180.0 / kotlin.math.PI).toFloat()
