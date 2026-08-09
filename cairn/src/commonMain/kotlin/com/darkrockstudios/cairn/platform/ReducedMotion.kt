package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable

/**
 * Whether the platform asks for reduced motion. Read once per open — Cairn
 * screens are short-lived, so a live listener isn't worth the surface.
 */
@Composable
internal expect fun isReducedMotionEnabled(): Boolean
