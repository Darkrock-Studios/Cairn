package com.darkrockstudios.cairn.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.effects.LocalSeamRegistry
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.LocalCairnAccent
import com.darkrockstudios.cairn.theme.LocalCairnEndGutter
import com.darkrockstudios.cairn.theme.LocalCairnWideLayout
import com.darkrockstudios.cairn.theme.cairnType

private val SeamEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/**
 * A content section bounded above by its accent seam, with the accent's soft
 * radial bleed below it. Seams are full-bleed; content is constrained to a
 * centered column in wide layouts.
 *
 * The seam ignites the first time the section scrolls into view (via
 * [LocalSeamRegistry]): the line blooms from 25% width / 12% alpha to full,
 * the bleed fades in, and a ripple races outward from center.
 */
@Composable
internal fun SeamSection(
    accent: Color,
    modifier: Modifier = Modifier,
    showSeam: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val wide = LocalCairnWideLayout.current
    val registry = LocalSeamRegistry.current

    val entry = remember { registry.register(accent, hasSeam = showSeam) }
    DisposableEffect(registry, entry) {
        onDispose { registry.unregister(entry) }
    }

    val lit by entry.lit
    val seamProgress by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = SeamEasing),
        label = "seamIgnition",
    )
    val bleedProgress by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, delayMillis = 150),
        label = "seamBleed",
    )

    CompositionLocalProvider(LocalCairnAccent provides accent) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { entry.coords = it }
                .drawBehind {
                    if (showSeam) {
                        // Ignition: scaleX 0.25→1, alpha 0.12→1.
                        val scaleX = 0.25f + 0.75f * seamProgress
                        val alpha = 0.12f + 0.88f * seamProgress
                        scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(size.width / 2f, 0f)) {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.15f to accent.copy(alpha = accent.alpha * alpha),
                                    0.85f to accent.copy(alpha = accent.alpha * alpha),
                                    1f to Color.Transparent,
                                ),
                                start = Offset(0f, 1.dp.toPx()),
                                end = Offset(size.width, 1.dp.toPx()),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                    }
                    // Accent bleed: soft elliptical wash below the seam —
                    // ~75% of the width but never taller than ~320dp, so wide
                    // layouts don't read as a flat tinted panel.
                    if (bleedProgress > 0.01f) {
                        val bleedRadius = (size.width * 0.375f).coerceAtLeast(1f)
                        val squash = (320.dp.toPx() / bleedRadius).coerceAtMost(1f)
                        scale(scaleX = 1f, scaleY = squash, pivot = Offset(size.width / 2f, 0f)) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.07f * bleedProgress),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width / 2f, 0f),
                                    radius = bleedRadius,
                                ),
                                size = Size(size.width, bleedRadius),
                            )
                        }
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = if (wide) 664.dp else Dp.Unspecified)
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp + LocalCairnEndGutter.current)
                    .padding(top = 34.dp, bottom = 24.dp),
                content = content,
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val accent = LocalCairnAccent.current
    BasicText(
        text = text.uppercase(),
        style = cairnType().sectionLabel.copy(
            color = lerp(CairnColors.BoneDim, accent, 0.65f),
        ),
        modifier = modifier.padding(bottom = 16.dp),
    )
}
