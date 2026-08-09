package com.darkrockstudios.cairn.sample

import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.darkrockstudios.cairn.CairnDebug

fun main() = application {
    // Arrow keys simulate device tilt (R resets): left/right = roll,
    // up/down = pitch around the natural ~40° hold.
    var roll = 0f
    var pitch = 40f

    Window(
        onCloseRequest = ::exitApplication,
        title = "Cairn Sample — arrows: tilt · R: reset tilt",
        state = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(420.dp, 860.dp),
        ),
        onKeyEvent = { event ->
            if (event.type != KeyEventType.KeyDown) return@Window false
            when (event.key) {
                Key.DirectionLeft -> { roll = (roll - 4f).coerceIn(-30f, 30f) }
                Key.DirectionRight -> { roll = (roll + 4f).coerceIn(-30f, 30f) }
                Key.DirectionUp -> { pitch = (pitch + 4f).coerceIn(10f, 70f) }
                Key.DirectionDown -> { pitch = (pitch - 4f).coerceIn(10f, 70f) }
                Key.R -> {
                    roll = 0f
                    pitch = 40f
                    CairnDebug.clearSimulatedTilt()
                    return@Window true
                }
                else -> return@Window false
            }
            CairnDebug.setSimulatedTilt(roll, pitch)
            true
        },
    ) {
        App()
    }
}
