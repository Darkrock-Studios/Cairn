package com.darkrockstudios.cairn.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Accelerometer → (roll, pitch) in web deviceorientation conventions.
 * The accelerometer reports world-up in device coordinates (flat face-up:
 * z≈+9.81; upright: y≈+9.81; right edge dipped: x goes negative).
 */
@Composable
internal actual fun PlatformTiltEffect(enabled: Boolean, onTilt: (Float, Float) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(enabled, context) {
        if (!enabled) return@DisposableEffect onDispose {}

        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val rollDeg = Math.toDegrees(
                    atan2(-x.toDouble(), sqrt((y * y + z * z).toDouble())),
                ).toFloat()
                val pitchDeg = Math.toDegrees(atan2(y.toDouble(), z.toDouble())).toFloat()
                onTilt(rollDeg, pitchDeg)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // ~30Hz: the screen lives for seconds, and the low-pass smooths it.
        manager.registerListener(listener, sensor, 33_000)

        onDispose { manager.unregisterListener(listener) }
    }
}
