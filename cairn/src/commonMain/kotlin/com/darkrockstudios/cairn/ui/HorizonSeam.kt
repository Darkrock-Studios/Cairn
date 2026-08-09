package com.darkrockstudios.cairn.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.catalog.cairnCatalog
import com.darkrockstudios.cairn.effects.AttractorSource
import com.darkrockstudios.cairn.effects.LocalCeremony
import com.darkrockstudios.cairn.effects.LocalGridEffects
import com.darkrockstudios.cairn.sound.LocalSoundEngine
import com.darkrockstudios.cairn.sound.SoundEngine
import com.darkrockstudios.cairn.theme.CairnColors
import kotlinx.coroutines.delay

private val SweepEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/**
 * The horizon: one seam holding every app's color — the site's signature.
 * Sweeps in on first composition, breathes on an 8s cycle, and carries a
 * white-hot specular gleam that follows the attractor when it comes near
 * (hover/touch now; device tilt joins in the tilt pass).
 */
@Composable
internal fun HorizonSeam(modifier: Modifier = Modifier) {
    val accents = remember { cairnCatalog.map { it.accent } }
    val effects = LocalGridEffects.current

    val ceremony = LocalCeremony.current
    val sound = LocalSoundEngine.current
    var swept by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { swept = true }
    val sweep by animateFloatAsState(
        targetValue = if (swept) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            delayMillis = ceremony?.foilSweepDelayMillis ?: 500,
            easing = SweepEasing,
        ),
        label = "horizonSweep",
    )

    // The 8s breathing pulse — the one deliberately always-on animation.
    val breathe by rememberInfiniteTransition(label = "horizonBreathe").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val feedback = LocalCairnFeedback.current

    // A generous tap strip around the 2dp seam: tapping the horizon blooms
    // the hum and lets it dissipate — the touch equivalent of hover-hum.
    // Holding it sustains a fine haptic rumble: current, under the finger.
    // Observed on the Initial pass without consuming, so the tap still
    // fires the generic grid click-pulse.
    var held by remember { mutableStateOf(false) }
    LaunchedEffect(held) {
        while (held) {
            feedback?.rumbleTick()
            delay(30)
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(sound) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        when (event.type) {
                            PointerEventType.Press -> {
                                sound?.humPulse()
                                feedback?.seamTick()
                                held = true
                            }
                            // The rumble lives only while the finger stays
                            // on the strip: lifting or dragging off ends it.
                            PointerEventType.Release, PointerEventType.Exit -> held = false
                            PointerEventType.Move -> {
                                if (held && change != null &&
                                    (change.position.y < 0f || change.position.y > size.height)
                                ) {
                                    held = false
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .onGloballyPositioned { coords = it }
                .drawBehind {
                    drawBaseLine()
                    if (sweep > 0.01f) {
                        scale(scaleX = sweep, scaleY = 1f, pivot = center) {
                            drawFoil(accents, alphaBoost = breathe * 0.35f)
                        }
                    }
                    drawGleam(effects, coords, sound, held)
                },
        )
    }
}

private fun DrawScope.drawBaseLine() {
    drawLine(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.2f to CairnColors.HorizonBase,
            0.8f to CairnColors.HorizonBase,
            1f to Color.Transparent,
        ),
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = size.height,
    )
}

private fun DrawScope.drawFoil(accents: List<Color>, alphaBoost: Float) {
    val foilWidth = size.width * 0.78f
    val start = (size.width - foilWidth) / 2f
    val stops = buildList {
        add(0f to Color.Transparent)
        accents.forEachIndexed { i, accent ->
            add((i + 1) / (accents.size + 1).toFloat() to accent)
        }
        add(1f to Color.Transparent)
    }
    val brush = Brush.horizontalGradient(
        *stops.toTypedArray(),
        startX = start,
        endX = start + foilWidth,
    )
    val y = size.height / 2f
    drawLine(brush, Offset(start, y), Offset(start + foilWidth, y), size.height)
    if (alphaBoost > 0.01f) {
        // Breathing: the same foil overdrawn at partial alpha reads as brightness.
        drawLine(
            brush,
            Offset(start, y),
            Offset(start + foilWidth, y),
            size.height,
            alpha = alphaBoost,
        )
    }
}

/**
 * White-hot gleam under a nearby attractor; proximity window is generous.
 * Also feeds the proximity hum — on hover, or while a finger deliberately
 * holds the seam strip: the hum sustains under the held finger, paired
 * with the haptic rumble. (A side effect from draw is unusual, but this is
 * exactly where proximity is already computed each invalidation.)
 */
private fun DrawScope.drawGleam(
    effects: com.darkrockstudios.cairn.effects.GridEffectsState,
    coords: LayoutCoordinates?,
    sound: SoundEngine?,
    held: Boolean,
) {
    effects.frameTick.longValue
    val humming = effects.attractorSource == AttractorSource.Hover || held
    val attractor = effects.attractor
    val content = effects.contentCoords
    val self = coords

    val center: Offset
    val near: Float
    if (attractor != null && content != null && self != null &&
        self.isAttached && content.isAttached
    ) {
        // Pointer physics: proximity to the seam.
        val local = self.localPositionOf(content, attractor)
        near = (1f - kotlin.math.abs(local.y - size.height / 2f) / (80 * density))
            .coerceIn(0f, 1f)
        center = Offset(local.x, size.height / 2f)
        sound?.humLevel(if (humming) near else 0f)
    } else {
        // Tilt physics: the holographic foil — the gleam slides with roll.
        sound?.humLevel(0f)
        val tilt = effects.tilt
        near = (kotlin.math.abs(tilt.x) * 1.6f).coerceIn(0f, 1f)
        center = Offset(size.width * (0.5f + tilt.x * 0.55f), size.height / 2f)
    }
    if (near <= 0.01f) return

    val gleamWidth = 130 * density
    scale(scaleX = 1f, scaleY = (9f * density) / gleamWidth, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFFFFF4E6).copy(alpha = 0.6f * near),
                0.7f to Color.Transparent,
                center = center,
                radius = gleamWidth,
            ),
            radius = gleamWidth,
            center = center,
        )
    }
}
