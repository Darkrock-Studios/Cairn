package com.darkrockstudios.cairn.theme

import androidx.compose.ui.graphics.Color

/**
 * The Dark Rock Studios palette, mirrored from darkrockstudios.com's CSS tokens.
 */
internal object CairnColors {
    val Basalt = Color(0xFF141517)
    val Slate = Color(0xFF1D2024)
    val Scree = Color(0xFF2F343A)
    val Bone = Color(0xFFE9E6E0)
    val BoneDim = Color(0xFF9AA1A9)
    val Prose = Color(0xFFC9CDD3)
    val Steel = Color(0xFF7D8896)

    /** Resting survey-grid line. Brighter than the website's 0.021 — that
     *  value was tuned for browser gamma and disappears on many screens. */
    val GridLine = Color.White.copy(alpha = 0.035f)

    /** Grid brightened by the attractor flashlight. */
    val GridBright = Color.White.copy(alpha = 0.17f)

    /** Warm tone for shockwaves and click pulses. */
    val ShockWarm = Color(0xFFFFCDA0)

    /** Warm tone for stray grid sparks and specular gleams. */
    val SparkWarm = Color(0xFFFFEBD2)

    /** The support section's seam. */
    val SupportOrange = Color(0xFFFF6A00)

    /** Neutral section seam — steel dimmed toward scree so its glow doesn't fog. */
    val NeutralSeam = Color(0xFF4D5761)

    /** Horizon base line color. */
    val HorizonBase = Color(0xFF3A4048)
}
