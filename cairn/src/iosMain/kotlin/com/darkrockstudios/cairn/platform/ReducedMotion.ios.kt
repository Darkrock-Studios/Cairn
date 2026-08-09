package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun isReducedMotionEnabled(): Boolean = UIAccessibilityIsReduceMotionEnabled()
