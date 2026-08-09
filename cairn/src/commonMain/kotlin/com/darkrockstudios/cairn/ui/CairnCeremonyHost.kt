package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.darkrockstudios.cairn.sound.rememberSoundEngine
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.CairnConfig
import com.darkrockstudios.cairn.effects.CeremonyState
import com.darkrockstudios.cairn.theme.CairnColors

private val EtchLine = Color(0xFFFFEBD2).copy(alpha = 0.14f)

/**
 * The ceremony layer stack, bottom to top: whatever the host drew (visible
 * through us while entering/leaving) → basalt scrim → etch lines → the about
 * screen itself. Matches the prototype's z-order exactly.
 */
@Composable
internal fun CairnCeremonyHost(
    config: CairnConfig,
    ceremony: CeremonyState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sound = rememberSoundEngine(config.soundDefault)

    // The ceremony's audio accents: rez in, rez out.
    LaunchedEffect(ceremony.phase) {
        when (ceremony.phase) {
            CeremonyState.Phase.Entering -> sound.rezIn()
            CeremonyState.Phase.Leaving -> sound.rezOut()
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Scrim: the basalt flood.
                val scrim = ceremony.scrimAlpha()
                if (scrim > 0f) {
                    drawRect(CairnColors.Basalt, alpha = scrim)
                }
                // Etch: survey lines plotted over reality.
                val etchAlpha = ceremony.etchAlpha()
                if (etchAlpha > 0.01f) {
                    drawEtch(ceremony, etchAlpha)
                }
                // The BONG: whole-grid surge on the load kachunk. Uneven on
                // purpose — each line carries its own randomly flickering
                // share of the current.
                val surge = ceremony.gridSurge()
                if (surge > 0.01f) {
                    drawSurgeGrid(surge, ceremony.surgeSeed())
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = ceremony.aboutAlpha()
                    compositingStrategy =
                        if (ceremony.aboutFlare() > 0f) CompositingStrategy.Offscreen
                        else CompositingStrategy.Auto
                }
                .drawWithContent {
                    drawContent()
                    val flare = ceremony.aboutFlare()
                    if (flare > 0f) {
                        drawRect(Color.White, alpha = flare, blendMode = BlendMode.SrcAtop)
                    }
                },
        ) {
            CairnRoot(
                config = config,
                onClose = onClose,
                ceremony = ceremony,
                sound = sound,
            )
        }
    }
}

private fun DrawScope.drawSurgeGrid(surge: Float, seed: Int) {
    val spacing = 32.dp.toPx()
    // Peak per-line alpha; flicker weights average ~0.6 of it, so the surge
    // as a whole still lands as bright as a uniform 0.18 flash would.
    val peak = 0.34f * surge
    var index = 0
    var x = spacing
    while (x < size.width) {
        val alpha = peak * flickerWeight(index++, seed)
        drawLine(EtchLine.copy(alpha = alpha), Offset(x, 0f), Offset(x, size.height), 1f)
        x += spacing
    }
    var y = spacing
    while (y < size.height) {
        val alpha = peak * flickerWeight(index++, seed)
        drawLine(EtchLine.copy(alpha = alpha), Offset(0f, y), Offset(size.width, y), 1f)
        y += spacing
    }
}

/** Stable hash of (line, seed) → [0.2, 1]: some lines slam bright, some barely carry. */
private fun flickerWeight(line: Int, seed: Int): Float {
    var h = line * 374761393 + seed * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return 0.2f + (h and 0xFFFF) / 65535f * 0.8f
}

private fun DrawScope.drawEtch(ceremony: CeremonyState, etchAlpha: Float) {
    val spacing = 32.dp.toPx()
    val color = EtchLine.copy(alpha = EtchLine.alpha * etchAlpha)

    val vReveal = ceremony.etchVClip()
    if (vReveal > 0f) {
        clipRect(right = size.width * vReveal) {
            var x = spacing
            while (x < size.width) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
                x += spacing
            }
        }
    }
    val hReveal = ceremony.etchHClip()
    if (hReveal > 0f) {
        clipRect(bottom = size.height * hReveal) {
            var y = spacing
            while (y < size.height) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
                y += spacing
            }
        }
    }
}
