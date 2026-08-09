package com.darkrockstudios.cairn.effects

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Tracks every seam section: seams light once when their section scrolls
 * into view (≥12% visible, silently — scroll-driven pulses felt bad on
 * device), and ripple when a hover pointer crosses a lit seam.
 */
internal class SeamRegistry(private val effects: GridEffectsState) {

    internal class Entry(
        val accent: Color,
        val hasSeam: Boolean,
    ) {
        var coords: LayoutCoordinates? = null
        val lit = mutableStateOf(false)
        var lastSide = 0
    }

    private val entries = ArrayList<Entry>()

    var onCross: (Entry) -> Unit = {}

    fun register(accent: Color, hasSeam: Boolean): Entry =
        Entry(accent, hasSeam).also { entries += it }

    fun unregister(entry: Entry) {
        entries -= entry
    }

    fun checkIgnitions() {
        val view = effects.visibleRect()
        if (view.height <= 0f) return
        entries.forEach { entry ->
            if (entry.lit.value) return@forEach
            val bounds = boundsInContent(entry) ?: return@forEach
            val overlap = (minOf(bounds.bottom, view.bottom) - maxOf(bounds.top, view.top))
            if (overlap / bounds.height.coerceAtLeast(1f) >= 0.12f) {
                // Light quietly: no ripple, no sound — scroll should never pulse.
                entry.lit.value = true
            }
        }
    }

    /** Pointer moved (touch drag or hover), in content space. */
    fun onPointer(position: Offset) {
        entries.forEach { entry ->
            if (!entry.lit.value || !entry.hasSeam) return@forEach
            val bounds = boundsInContent(entry) ?: return@forEach
            val side = if (position.y < bounds.top) -1 else 1
            val previous = entry.lastSide
            entry.lastSide = side
            if (previous != 0 && previous != side) {
                effects.spawnSeamRipple(
                    yContent = bounds.top + 1f,
                    leftX = bounds.left,
                    rightX = bounds.right,
                    originX = position.x.coerceIn(bounds.left, bounds.right),
                    accent = entry.accent,
                )
                onCross(entry)
            }
        }
    }

    fun clearSides() {
        entries.forEach { it.lastSide = 0 }
    }

    private fun boundsInContent(entry: Entry): Rect? {
        val coords = entry.coords ?: return null
        val content = effects.contentCoords ?: return null
        if (!coords.isAttached || !content.isAttached) return null
        return content.localBoundingBoxOf(coords, clipBounds = false)
    }
}

internal val LocalGridEffects = staticCompositionLocalOf<GridEffectsState> {
    error("GridEffectsState not provided")
}

internal val LocalSeamRegistry = staticCompositionLocalOf<SeamRegistry> {
    error("SeamRegistry not provided")
}
