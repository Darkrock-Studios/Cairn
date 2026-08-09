package com.darkrockstudios.cairn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Offscreen render harness: writes PNGs of the screen to build/preview/ for
 * visual inspection. Not an assertion suite — a development aid that also
 * catches composition crashes (missing resources, layout explosions).
 */
@OptIn(ExperimentalComposeUiApi::class)
class RenderPreviewTest {

    @Test
    fun renderPhone() = renderToFile("cairn-phone.png", widthDp = 420, heightDp = 2000)

    @Test
    fun renderTablet() = renderToFile("cairn-tablet.png", widthDp = 768, heightDp = 1700)

    @Test
    fun renderTabletLandscape() =
        renderToFile("cairn-tablet-landscape.png", widthDp = 1280, heightDp = 1500)

    /**
     * Viewport-sized scene with synthetic input: hover mid-screen for the
     * flashlight, then tap the summit and catch the shockwave mid-flight.
     */
    @Test
    fun renderEffects() {
        val density = 2f
        scene(widthDp = 420, heightDp = 860, density = density).use { scene ->
            var timeNanos = pumpFrames(scene, 0L, frames = 30, sleepMs = 50)

            scene.sendPointerEvent(PointerEventType.Move, Offset(340f * density, 700f * density))
            timeNanos = pumpFrames(scene, timeNanos, frames = 2)

            val summit = Offset(210f * density, 88f * density)
            scene.sendPointerEvent(PointerEventType.Press, summit)
            timeNanos = pumpFrames(scene, timeNanos, frames = 2)
            scene.sendPointerEvent(PointerEventType.Release, summit)

            timeNanos = pumpFrames(scene, timeNanos, frames = 22)
            save(scene.render(timeNanos), "cairn-effects.png")
        }
    }

    /**
     * Seam ignition + card sheen: catch the family seam's ignition ripple
     * shortly after open, then scroll and hover a card for the foil sheen.
     */
    @Test
    fun renderSeamsAndSheen() {
        val density = 2f
        scene(widthDp = 420, heightDp = 860, density = density).use { scene ->
            var timeNanos = pumpFrames(scene, 0L, frames = 20, sleepMs = 30)
            save(scene.render(timeNanos), "cairn-seam-ignition.png")

            val center = Offset(210f * density, 430f * density)
            repeat(6) {
                scene.sendPointerEvent(
                    PointerEventType.Scroll,
                    position = center,
                    scrollDelta = Offset(0f, 3f),
                )
                timeNanos = pumpFrames(scene, timeNanos, frames = 2)
            }
            scene.sendPointerEvent(PointerEventType.Move, Offset(200f * density, 400f * density))
            timeNanos = pumpFrames(scene, timeNanos, frames = 8)
            save(scene.render(timeNanos), "cairn-sheen.png")
        }
    }

    /**
     * The full ceremony over a fake host screen: etch → flood → ignite, then
     * the retreat after tapping close. Times are virtual-clock exact.
     */
    @Test
    fun renderCeremony() {
        val density = 2f
        ImageComposeScene(
            width = (420 * density).toInt(),
            height = (860 * density).toInt(),
            density = Density(density),
        ) {
            Box(Modifier.fillMaxSize().background(Color(0xFF1C0F33))) {
                CairnAboutOverlay(
                    visible = true,
                    config = CairnConfig(currentAppId = "fasttrack", versionName = "5.0.1"),
                    onDismissed = {},
                )
            }
        }.use { scene ->
            // 0 → 240ms: etch lines plotting over the host.
            var timeNanos = pumpFrames(scene, 0L, frames = 15, sleepMs = 30)
            save(scene.render(timeNanos), "ceremony-1-etch.png")

            // → 620ms: basalt flooded, summit igniting.
            timeNanos = pumpFrames(scene, timeNanos, frames = 24, sleepMs = 30)
            save(scene.render(timeNanos), "ceremony-2-flood.png")

            // → 1060ms: content blooming top-to-bottom.
            timeNanos = pumpFrames(scene, timeNanos, frames = 27, sleepMs = 30)
            save(scene.render(timeNanos), "ceremony-3-ignite.png")

            // → steady open.
            timeNanos = pumpFrames(scene, timeNanos, frames = 35)

            // Close (✕ at top-right) and catch the retreat mid-flight.
            val closePos = Offset(393f * density, 27f * density)
            scene.sendPointerEvent(PointerEventType.Press, closePos)
            timeNanos = pumpFrames(scene, timeNanos, frames = 2)
            scene.sendPointerEvent(PointerEventType.Release, closePos)
            timeNanos = pumpFrames(scene, timeNanos, frames = 20)
            save(scene.render(timeNanos), "ceremony-4-retreat.png")
        }
    }

    /**
     * Tilt physics via CairnDebug: strong right-roll should slide the horizon
     * gleam, light every card uniformly (global sheen), shift the grid
     * parallax, and drift the ambient tilt light.
     */
    @Test
    fun renderTilt() {
        val density = 2f
        scene(widthDp = 420, heightDp = 860, density = density).use { scene ->
            var timeNanos = pumpFrames(scene, 0L, frames = 30, sleepMs = 50)

            // Scroll to put horizon + cards on screen together.
            repeat(3) {
                scene.sendPointerEvent(
                    PointerEventType.Scroll,
                    position = Offset(210f * density, 430f * density),
                    scrollDelta = Offset(0f, 3f),
                )
                timeNanos = pumpFrames(scene, timeNanos, frames = 2)
            }
            // Ramp the simulated tilt so the low-pass filter converges.
            listOf(10f, 18f, 24f, 28f, 30f, 29f).forEachIndexed { i, roll ->
                CairnDebug.setSimulatedTilt(roll, 55f + i)
                timeNanos = pumpFrames(scene, timeNanos, frames = 2)
            }
            timeNanos = pumpFrames(scene, timeNanos, frames = 4)
            save(scene.render(timeNanos), "cairn-tilt.png")
            CairnDebug.clearSimulatedTilt()
        }
    }

    private fun scene(widthDp: Int, heightDp: Int, density: Float = 2f): ImageComposeScene =
        ImageComposeScene(
            width = (widthDp * density).toInt(),
            height = (heightDp * density).toInt(),
            density = Density(density),
        ) {
            CairnAboutScreen(
                config = CairnConfig(
                    currentAppId = "fasttrack",
                    versionName = "5.0.1",
                    entrance = CairnEntrance.None,
                ),
                onClose = {},
            )
        }

    private fun pumpFrames(
        scene: ImageComposeScene,
        startNanos: Long,
        frames: Int,
        sleepMs: Long = 0,
    ): Long {
        var timeNanos = startNanos
        repeat(frames) {
            scene.render(timeNanos)
            timeNanos += 16_000_000L
            if (sleepMs > 0) Thread.sleep(sleepMs)
        }
        return timeNanos
    }

    private fun renderToFile(name: String, widthDp: Int, heightDp: Int) {
        scene(widthDp, heightDp).use { sceneRef ->
            val timeNanos = pumpFrames(sceneRef, 0L, frames = 30, sleepMs = 50)
            save(sceneRef.render(timeNanos), name)
        }
    }

    private fun save(image: org.jetbrains.skia.Image, name: String) {
        val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encode failed")
        val out = File("build/preview/$name")
        out.parentFile.mkdirs()
        out.writeBytes(data.bytes)
        println("wrote ${out.absolutePath} (${data.bytes.size} bytes)")
    }
}
