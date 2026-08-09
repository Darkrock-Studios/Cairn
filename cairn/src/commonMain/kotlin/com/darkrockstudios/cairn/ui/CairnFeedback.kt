package com.darkrockstudios.cairn.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Semantic haptic events mapped onto Compose's built-in multiplatform
 * haptics (real on Android + iOS, no-op elsewhere). No expect/actual and no
 * VIBRATE permission — the whole reason we use the built-in.
 */
internal class CairnFeedback(
    private val haptics: HapticFeedback,
    private val enabled: Boolean,
) {
    /** Seam ignitions and crossings: the lightest tick available. */
    fun seamTick() = perform(HapticFeedbackType.SegmentFrequentTick)

    /** Card and chip taps. */
    fun tap() = perform(HapticFeedbackType.ContextClick)

    /** The summit strike: the heavy one. */
    fun strike() = perform(HapticFeedbackType.LongPress)

    /** The heart's beat. */
    fun heartbeat() = perform(HapticFeedbackType.Confirm)

    /**
     * One grain of the horizon hold-rumble: the finest tick available,
     * fired every ~30ms while held — reads as a continuous fine buzz.
     */
    fun rumbleTick() = perform(HapticFeedbackType.TextHandleMove)

    private fun perform(type: HapticFeedbackType) {
        if (enabled) haptics.performHapticFeedback(type)
    }
}

internal val LocalCairnFeedback = staticCompositionLocalOf<CairnFeedback?> { null }

@Composable
internal fun rememberCairnFeedback(enabled: Boolean): CairnFeedback {
    val haptics = LocalHapticFeedback.current
    return remember(haptics, enabled) { CairnFeedback(haptics, enabled) }
}
