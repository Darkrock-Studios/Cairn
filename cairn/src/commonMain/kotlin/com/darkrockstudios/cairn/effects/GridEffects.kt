package com.darkrockstudios.cairn.effects

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.theme.CairnColors
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

internal enum class AttractorSource { None, Touch, Hover, Tilt }

/** How a ring pulse brightens the grid. */
internal class RingEffect(
    val origin: Offset,
    val startRadiusPx: Float,
    val endRadiusPx: Float,
    val featherPx: Float,
    val durationMillis: Int,
    val startAlpha: Float,
    val color: Color,
    val easing: Easing,
) {
    var startNanos: Long = UNSET

    fun progress(now: Long): Float =
        ((now - startNanos) / 1_000_000f / durationMillis).coerceIn(0f, 1f)

    companion object {
        const val UNSET = Long.MIN_VALUE
    }
}

/**
 * A pulse of light riding a seam: two 110dp segments racing outward from the
 * crossing/ignition point at equal speed, sliding out of the section's span.
 */
internal class SeamRippleEffect(
    val yContent: Float,
    val leftX: Float,
    val rightX: Float,
    val originX: Float,
    val accent: Color,
    val distLeftPx: Float,
    val durLeftMillis: Int,
    val distRightPx: Float,
    val durRightMillis: Int,
) {
    var startNanos: Long = RingEffect.UNSET

    fun progress(now: Long, durationMillis: Int): Float =
        ((now - startNanos) / 1_000_000f / durationMillis).coerceIn(0f, 1f)

    fun finished(now: Long): Boolean =
        progress(now, maxOf(durLeftMillis, durRightMillis)) >= 1f
}

internal class SparkEffect(
    /** true = rides a horizontal grid line. */
    val horizontal: Boolean,
    /** The grid line's fixed coordinate (content space). */
    val lineCoord: Float,
    /** Travel start position along the line. */
    val startAlong: Float,
    val distancePx: Float,
    val direction: Float,
    val speedPxPerSec: Float,
) {
    var startNanos: Long = RingEffect.UNSET

    val durationMillis: Int get() = (distancePx / speedPxPerSec * 1000f).toInt()

    fun progress(now: Long): Float =
        ((now - startNanos) / 1_000_000f / durationMillis).coerceIn(0f, 1f)
}

/**
 * Owner of every live grid effect. Drawing subscribes to [frameTick]; the frame
 * loop only schedules frames while effects are alive, so the screen costs zero
 * frames at rest. Timer-driven spawners (sparks, summit idle pulses) run on
 * delay-coroutines and wake the loop by injecting work.
 */
internal class GridEffectsState(density: Density) {

    val gridSpacingPx: Float = with(density) { 32.dp.toPx() }
    private val dp = density.density

    /** Bumped every stepped frame and on attractor motion; draw reads it. */
    val frameTick = mutableLongStateOf(0L)

    var attractor: Offset? = null
        private set
    var attractorSource: AttractorSource = AttractorSource.None
        private set

    /** Smoothed, normalized device tilt (nx, ny) in -1..1; Zero when flat/absent. */
    var tilt: Offset = Offset.Zero
        private set

    internal val rings = ArrayList<RingEffect>()
    internal val sparks = ArrayList<SparkEffect>()
    internal val seamRipples = ArrayList<SeamRippleEffect>()

    /** The scrolled content's layout node; effects and seams share its space. */
    var contentCoords: androidx.compose.ui.layout.LayoutCoordinates? = null

    private val work = Channel<Unit>(Channel.CONFLATED)

    /** True once the user has fired the summit themselves. */
    var struck: Boolean = false
        private set

    /** Content-space rect currently visible; set by the host layout. */
    var visibleRect: () -> Rect = { Rect.Zero }

    /** Ambient spawners (sparks, idle pulses) run only when this is true —
     * suppressed during the entrance/exit ceremonies. */
    var ambientEnabled: () -> Boolean = { true }

    /** Content-space summit center, when known. */
    var summitCenter: () -> Offset? = { null }

    private val shockEasing = CubicBezierEasing(0.2f, 0.6f, 0.35f, 1f)

    // ---- attractor ----

    fun attractorMoved(position: Offset, source: AttractorSource) {
        // Touch beats hover beats tilt.
        if (source.ordinal < attractorSource.ordinal && attractor != null) return
        attractor = position
        attractorSource = source
        invalidate()
    }

    fun attractorCleared(source: AttractorSource) {
        if (source != attractorSource) return
        attractor = null
        attractorSource = AttractorSource.None
        invalidate()
    }

    fun tiltChanged(nx: Float, ny: Float) {
        tilt = Offset(nx, ny)
        invalidate()
    }

    /** Grid parallax: the grid slides gently opposite the tilt. */
    fun parallaxOffset(): Offset = Offset(-tilt.x * 5 * dp, -tilt.y * 4 * dp)

    // ---- spawns ----

    fun strike(origin: Offset) {
        struck = true
        rings += RingEffect(
            origin = origin,
            startRadiusPx = 10 * dp,
            endRadiusPx = 900 * dp,
            featherPx = 40 * dp,
            durationMillis = 1600,
            startAlpha = 1f,
            color = CairnColors.ShockWarm.copy(alpha = 0.45f),
            easing = shockEasing,
        )
        wake()
    }

    fun miniPulse(origin: Offset) {
        rings += RingEffect(
            origin = origin,
            startRadiusPx = 6 * dp,
            endRadiusPx = 190 * dp,
            featherPx = 18 * dp,
            durationMillis = 1800,
            startAlpha = 0.55f,
            color = CairnColors.ShockWarm.copy(alpha = 0.2f),
            easing = LinearOutSlowInEasing,
        )
        wake()
    }

    fun clickPulse(origin: Offset, accent: Color? = null) {
        val base = accent ?: CairnColors.ShockWarm
        rings += RingEffect(
            origin = origin,
            startRadiusPx = 8 * dp,
            endRadiusPx = 300 * dp,
            featherPx = 22 * dp,
            durationMillis = 850,
            startAlpha = 1f,
            color = base.copy(alpha = 0.5f),
            easing = LinearOutSlowInEasing,
        )
        wake()
    }

    fun spawnSeamRipple(
        yContent: Float,
        leftX: Float,
        rightX: Float,
        originX: Float,
        accent: Color,
    ) {
        val pulseW = 110 * dp
        val width = (rightX - leftX).coerceAtLeast(1f)
        val distLeft = (originX - leftX) + pulseW
        val distRight = (rightX - originX) + pulseW
        val fullWidthMs = 450f
        seamRipples += SeamRippleEffect(
            yContent = yContent,
            leftX = leftX,
            rightX = rightX,
            originX = originX,
            accent = accent,
            distLeftPx = distLeft,
            durLeftMillis = (distLeft / width * fullWidthMs).toInt().coerceAtLeast(80),
            distRightPx = distRight,
            durRightMillis = (distRight / width * fullWidthMs).toInt().coerceAtLeast(80),
        )
        wake()
    }

    fun spawnSpark() {
        val view = visibleRect()
        if (view.width < gridSpacingPx * 4 || view.height < gridSpacingPx * 4) return
        val horizontal = Random.nextBoolean()
        val margin = 20 * dp
        val sparkLen = 38 * dp

        fun snapToLine(min: Float, max: Float): Float {
            val first = (((min + margin) / gridSpacingPx).toInt() + 1) * gridSpacingPx
            val count = (((max - margin) - first) / gridSpacingPx).toInt().coerceAtLeast(0)
            return first + Random.nextInt(count + 1) * gridSpacingPx
        }

        val lineCoord: Float
        val startAlong: Float
        if (horizontal) {
            lineCoord = snapToLine(view.top, view.bottom)
            startAlong = view.left + margin + Random.nextFloat() * (view.width - 2 * margin - sparkLen)
        } else {
            lineCoord = snapToLine(view.left, view.right)
            startAlong = view.top + margin + Random.nextFloat() * (view.height - 2 * margin - sparkLen)
        }

        sparks += SparkEffect(
            horizontal = horizontal,
            lineCoord = lineCoord,
            startAlong = startAlong.coerceAtLeast(if (horizontal) view.left else view.top),
            distancePx = (150 + Random.nextFloat() * 300) * dp,
            direction = if (Random.nextBoolean()) 1f else -1f,
            speedPxPerSec = (210 + Random.nextFloat() * 120) * dp,
        )
        wake()
    }

    // ---- the loops ----

    /** Runs until the composition leaves; structured under a LaunchedEffect. */
    suspend fun runLoops(withFrame: suspend (onFrame: (Long) -> Unit) -> Unit) = coroutineScope {
        // Frame stepper: suspends entirely while there is no live effect.
        launch {
            while (isActive) {
                if (rings.isEmpty() && sparks.isEmpty() && seamRipples.isEmpty()) {
                    work.receive()
                }
                withFrame { now -> step(now) }
            }
        }
        // Stray sparks: frequent but faint.
        launch {
            while (isActive) {
                delay(1700 + Random.nextLong(2800))
                if (!ambientEnabled()) continue
                spawnSpark()
                if (Random.nextFloat() < 0.22f) {
                    delay(160)
                    spawnSpark()
                }
            }
        }
        // Summit idle pulses until the first strike.
        launch {
            while (isActive) {
                delay(2300)
                if (struck) break
                if (!ambientEnabled()) continue
                val center = summitCenter() ?: continue
                if (visibleRect().contains(center)) miniPulse(center)
            }
        }
    }

    private fun step(now: Long) {
        rings.forEach { if (it.startNanos == RingEffect.UNSET) it.startNanos = now }
        sparks.forEach { if (it.startNanos == RingEffect.UNSET) it.startNanos = now }
        seamRipples.forEach { if (it.startNanos == RingEffect.UNSET) it.startNanos = now }
        rings.removeAll { it.progress(now) >= 1f }
        sparks.removeAll { it.progress(now) >= 1f }
        seamRipples.removeAll { it.finished(now) }
        frameTick.longValue = now
    }

    private fun wake() {
        work.trySend(Unit)
        invalidate()
    }

    private fun invalidate() {
        frameTick.longValue = frameTick.longValue + 1
    }
}

@Composable
internal fun rememberGridEffects(): GridEffectsState {
    val density = LocalDensity.current
    val state = remember { GridEffectsState(density) }
    LaunchedEffect(state) {
        state.runLoops { onFrame ->
            androidx.compose.runtime.withFrameNanos(onFrame)
        }
    }
    return state
}
