package com.darkrockstudios.cairn.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * The element-ignite bloom: rises from below while flaring hot, then settles.
 * The flare is a white self-tint (SrcAtop in an offscreen layer) — the
 * portable stand-in for the prototype's `brightness(2.2)` filter.
 */
@Composable
internal fun Modifier.ignite(delayMillis: Int): Modifier {
    val ceremony = LocalCeremony.current ?: return this
    return this
        .graphicsLayer {
            val p = ceremony.igniteProgress(delayMillis)
            alpha = (p / 0.55f).coerceAtMost(1f)
            translationY = 12.dp.toPx() * (1f - p)
            compositingStrategy =
                if (p < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }
        .drawWithContent {
            drawContent()
            val p = ceremony.igniteProgress(delayMillis)
            if (p > 0f && p < 1f) {
                val flare = (1f - p).pow(1.5f) * 0.35f
                drawRect(
                    color = Color.White,
                    alpha = flare,
                    blendMode = BlendMode.SrcAtop,
                )
            }
        }
}
