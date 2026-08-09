package com.darkrockstudios.cairn.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.darkrockstudios.cairn.theme.CairnColors
import kotlin.math.max
import kotlin.math.min

/** The resting survey grid, offset by the tilt parallax. */
internal fun DrawScope.drawSurveyGrid(spacingPx: Float, parallax: Offset = Offset.Zero) {
    var y = spacingPx + parallax.y.mod(spacingPx)
    while (y < size.height) {
        drawLine(CairnColors.GridLine, Offset(0f, y), Offset(size.width, y), 1f)
        y += spacingPx
    }
    var x = spacingPx + parallax.x.mod(spacingPx)
    while (x < size.width) {
        drawLine(CairnColors.GridLine, Offset(x, 0f), Offset(x, size.height), 1f)
        x += spacingPx
    }
}

/** Ambient tilt light: a very soft warm glow drifting opposite the tilt. */
internal fun DrawScope.drawTiltLight(state: GridEffectsState) {
    val tilt = state.tilt
    if (tilt == Offset.Zero) return
    val view = state.visibleRect()
    if (view.width <= 0f) return
    val center = Offset(
        view.left + view.width * (0.5f - tilt.x * 0.45f),
        view.top + view.height * (0.3f - tilt.y * 0.35f),
    )
    val radius = 260 * density
    drawCircle(
        brush = Brush.radialGradient(
            0f to CairnColors.SparkWarm.copy(alpha = 0.07f),
            0.7f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * All live effects. The CSS prototype used mask-image over brightened grid
 * copies; here each grid line is stroked with a radial-gradient brush that is
 * only opaque where the effect lives — same look, no layers.
 */
internal fun DrawScope.drawGridEffects(state: GridEffectsState, now: Long) {
    drawTiltLight(state)
    state.attractor?.let { drawFlashlight(it, state.gridSpacingPx) }
    state.rings.forEach { drawRing(it, now, state.gridSpacingPx) }
    state.sparks.forEach { drawSpark(it, now) }
    state.seamRipples.forEach { drawSeamRipple(it, now) }
}

private fun DrawScope.drawSeamRipple(ripple: SeamRippleEffect, now: Long) {
    if (ripple.startNanos == RingEffect.UNSET) return
    val pulseW = 110 * density
    val hot = lerp(ripple.accent, Color.White, 0.45f)

    fun pulse(direction: Float, dist: Float, duration: Int) {
        val progress = ripple.progress(now, duration)
        if (progress >= 1f) return
        val alpha = 1f - 0.9f * progress
        val centerX = ripple.originX + direction * dist * progress
        val start = (centerX - pulseW / 2f).coerceAtLeast(ripple.leftX)
        val end = (centerX + pulseW / 2f).coerceAtMost(ripple.rightX)
        if (end <= start) return
        val brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to hot.copy(alpha = alpha),
            1f to Color.Transparent,
            start = Offset(centerX - pulseW / 2f, ripple.yContent),
            end = Offset(centerX + pulseW / 2f, ripple.yContent),
        )
        drawLine(
            brush,
            Offset(start, ripple.yContent),
            Offset(end, ripple.yContent),
            3 * density,
        )
    }

    pulse(-1f, ripple.distLeftPx, ripple.durLeftMillis)
    pulse(1f, ripple.distRightPx, ripple.durRightMillis)
}

/** Grid brightening around the attractor: radius 120dp, fading out at 72%. */
private fun DrawScope.drawFlashlight(center: Offset, spacing: Float) {
    val radius = 120 * density
    val brush = Brush.radialGradient(
        0f to CairnColors.GridBright,
        0.72f to Color.Transparent,
        center = center,
        radius = radius,
    )
    forEachGridLineNear(center, radius, spacing) { start, end ->
        drawLine(brush, start, end, 1f)
    }
}

private fun DrawScope.drawRing(ring: RingEffect, now: Long, spacing: Float) {
    if (ring.startNanos == RingEffect.UNSET) return
    val progress = ring.easing.transform(ring.progress(now))
    val radius = ring.startRadiusPx + (ring.endRadiusPx - ring.startRadiusPx) * progress
    val alpha = ring.startAlpha * (1f - progress)
    if (alpha <= 0.005f) return

    val outer = radius + ring.featherPx
    val inner = max(0f, radius - ring.featherPx)
    val brush = Brush.radialGradient(
        (inner / outer) to Color.Transparent,
        (radius / outer) to ring.color.copy(alpha = ring.color.alpha * alpha),
        1f to Color.Transparent,
        center = ring.origin,
        radius = outer,
    )
    forEachGridLineNear(ring.origin, outer, spacing) { start, end ->
        drawLine(brush, start, end, 1f)
    }
}

private fun DrawScope.drawSpark(spark: SparkEffect, now: Long) {
    if (spark.startNanos == RingEffect.UNSET) return
    val progress = spark.progress(now)

    // Opacity envelope: ramp in to 15%, hold, ramp out from 80%. Peak 0.65.
    val envelope = when {
        progress < 0.15f -> progress / 0.15f
        progress > 0.8f -> (1f - progress) / 0.2f
        else -> 1f
    } * 0.65f
    if (envelope <= 0.01f) return

    val along = spark.startAlong + spark.direction * spark.distancePx * progress
    val len = 38 * density
    val (start, end) = if (spark.horizontal) {
        Offset(along, spark.lineCoord) to Offset(along + len, spark.lineCoord)
    } else {
        Offset(spark.lineCoord, along) to Offset(spark.lineCoord, along + len)
    }

    val core = CairnColors.SparkWarm.copy(alpha = 0.7f * envelope)
    val glow = CairnColors.SparkWarm.copy(alpha = 0.24f * envelope)
    val brush = Brush.linearGradient(
        0f to Color.Transparent,
        0.5f to core,
        1f to Color.Transparent,
        start = start,
        end = end,
    )
    val glowBrush = Brush.linearGradient(
        0f to Color.Transparent,
        0.5f to glow,
        1f to Color.Transparent,
        start = start,
        end = end,
    )
    drawLine(glowBrush, start, end, 6 * density)
    drawLine(brush, start, end, 2 * density)
}

/** Visits grid lines whose stroke could intersect a circle at [center]/[radius]. */
private inline fun DrawScope.forEachGridLineNear(
    center: Offset,
    radius: Float,
    spacing: Float,
    draw: (start: Offset, end: Offset) -> Unit,
) {
    val minY = max(spacing, (((center.y - radius) / spacing).toInt()) * spacing)
    val maxY = min(size.height, center.y + radius)
    var y = minY
    while (y <= maxY) {
        draw(Offset(max(0f, center.x - radius), y), Offset(min(size.width, center.x + radius), y))
        y += spacing
    }
    val minX = max(spacing, (((center.x - radius) / spacing).toInt()) * spacing)
    val maxX = min(size.width, center.x + radius)
    var x = minX
    while (x <= maxX) {
        draw(Offset(x, max(0f, center.y - radius)), Offset(x, min(size.height, center.y + radius)))
        x += spacing
    }
}
