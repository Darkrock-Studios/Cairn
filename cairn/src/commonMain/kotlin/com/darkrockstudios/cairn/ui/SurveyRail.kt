package com.darkrockstudios.cairn.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.cairn.effects.LocalSeamRegistry
import com.darkrockstudios.cairn.effects.SeamRegistry
import com.darkrockstudios.cairn.sound.LocalSoundEngine
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.cairnType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val RailWidth = 34.dp
private val TickSpacing = 9.dp
private val ThumbMinHeight = 36.dp

/** Gap between the rail and the window edge. */
internal val RailEdgeInset = 10.dp

/** Everything the rail claims on the right edge; content keeps clear of it. */
internal val RailFootprint = RailWidth + RailEdgeInset

/**
 * The survey rail: Cairn's scrollbar as a surveyor's leveling rod. A graduated
 * scale runs the right gutter with each section notched onto it in that
 * section's accent, and a caliper marker rides the scale trailing warm
 * phosphor that brightens with scroll speed. Drag the marker, or grab the
 * scale anywhere to jump. Mouse only: it fades in the first time a mouse is
 * seen and never appears for touch.
 */
@Composable
internal fun SurveyRail(
    scrollState: ScrollState,
    viewportHeightPx: Float,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val seams = LocalSeamRegistry.current
    val sound = LocalSoundEngine.current
    val feedback = LocalCairnFeedback.current
    val scope = rememberCoroutineScope()
    val measurer = rememberTextMeasurer()
    val readoutStyle = cairnType().versionStamp.copy(fontSize = 9.sp)

    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var railHeightPx by remember { mutableFloatStateOf(0f) }
    var heatDir by remember { mutableFloatStateOf(1f) }

    val maxValue = scrollState.maxValue
    val scrollable = maxValue > 0 && viewportHeightPx > 0f

    val focus by animateFloatAsState(
        targetValue = if (dragging) 1f else if (hovered) 0.6f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "railFocus",
    )
    val presence by animateFloatAsState(
        targetValue = if (visible && scrollable) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "railPresence",
    )

    // Phosphor: scrolling charges the marker, then it bleeds off. Charge is
    // plain state driving a composition animation. Thar be dragons: an
    // Animatable launched from the scroll collector gets resumed inside frame
    // dispatch and deadlocks against the frame clock.
    var burst by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(scrollState) {
        var last = scrollState.value
        snapshotFlow { scrollState.value }.collect { value ->
            val delta = value - last
            last = value
            if (delta == 0) return@collect
            heatDir = if (delta > 0) 1f else -1f
            burst = min(1f, max(burst, abs(delta) / 60f))
        }
    }
    LaunchedEffect(burst) {
        if (burst > 0f) {
            delay(90)
            burst = 0f
        }
    }
    val heat by animateFloatAsState(
        targetValue = burst,
        animationSpec = tween(
            durationMillis = if (burst > 0f) 90 else 900,
            easing = LinearEasing,
        ),
        label = "railHeat",
    )

    if (presence <= 0.01f) return

    val contentHeightPx = viewportHeightPx + maxValue

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(RailWidth)
            .onSizeChanged { railHeightPx = it.height.toFloat() }
            // The wheel still works over the rail; the drag detectors sit
            // inside this, so they consume thumb drags before it sees them.
            .scrollable(
                state = scrollState,
                orientation = Orientation.Vertical,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    Orientation.Vertical,
                    false,
                ),
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                            else -> Unit
                        }
                    }
                }
            }
            .pointerInput(railHeightPx, maxValue, viewportHeightPx) {
                val thumbPx = thumbHeightPx(railHeightPx, viewportHeightPx, contentHeightPx, this)
                val track = (railHeightPx - thumbPx).coerceAtLeast(1f)
                detectTapGestures { offset ->
                    // Clicks land the marker where you clicked; clicking the
                    // marker itself is a no-op, as any scrollbar should be.
                    val thumbTop = scrollState.value / maxValue.toFloat() * track
                    if (offset.y in thumbTop..(thumbTop + thumbPx)) return@detectTapGestures
                    sound?.onUserGesture()
                    scope.launch {
                        scrollState.animateScrollTo(valueAt(offset.y, thumbPx, track, maxValue))
                    }
                }
            }
            .pointerInput(railHeightPx, maxValue, viewportHeightPx) {
                val thumbPx = thumbHeightPx(railHeightPx, viewportHeightPx, contentHeightPx, this)
                val track = (railHeightPx - thumbPx).coerceAtLeast(1f)
                var lastValue = 0
                detectDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        lastValue = scrollState.value
                        sound?.onUserGesture()
                        // Grabbing off the marker snaps it under the cursor first.
                        val thumbTop = scrollState.value / maxValue.toFloat() * track
                        if (offset.y < thumbTop || offset.y > thumbTop + thumbPx) {
                            val target = valueAt(offset.y, thumbPx, track, maxValue)
                            scrollState.dispatchRawDelta(target - scrollState.value.toFloat())
                        }
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, drag ->
                    change.consume()
                    scrollState.dispatchRawDelta(drag.y * maxValue / track)
                    val value = scrollState.value
                    if (crossedMark(seams, lastValue, value)) {
                        sound?.thock()
                        feedback?.seamTick()
                    }
                    lastValue = value
                }
            }
            .drawBehind {
                drawRail(
                    frac = (scrollState.value / maxValue.toFloat()).coerceIn(0f, 1f),
                    thumbPx = thumbHeightPx(size.height, viewportHeightPx, contentHeightPx, this),
                    marks = seams.marks(),
                    contentHeightPx = contentHeightPx,
                    heat = heat,
                    heatDir = heatDir,
                    focus = focus,
                    presence = presence,
                    readout = if (focus > 0.01f) {
                        (scrollState.value * 100f / maxValue).roundToInt().coerceIn(0, 100)
                    } else {
                        null
                    },
                    measurer = measurer,
                    readoutStyle = readoutStyle,
                )
            },
    )
}

private fun valueAt(y: Float, thumbPx: Float, track: Float, maxValue: Int): Int =
    ((y - thumbPx / 2f) / track * maxValue).coerceIn(0f, maxValue.toFloat()).roundToInt()

private fun thumbHeightPx(
    railHeightPx: Float,
    viewportHeightPx: Float,
    contentHeightPx: Float,
    density: Density,
): Float {
    val floor = with(density) { ThumbMinHeight.toPx() }
    if (contentHeightPx <= 0f || railHeightPx <= 0f) return floor
    val proportional = railHeightPx * (viewportHeightPx / contentHeightPx)
    return proportional.coerceIn(min(floor, railHeightPx), railHeightPx)
}

/** True when this scroll step swept the viewport's top edge past a section. */
private fun crossedMark(seams: SeamRegistry, from: Int, to: Int): Boolean {
    if (from == to) return false
    val low = min(from, to).toFloat()
    val high = max(from, to).toFloat()
    return seams.marks().any { it.contentTop in low..high }
}

private fun DrawScope.drawRail(
    frac: Float,
    thumbPx: Float,
    marks: List<SeamRegistry.Mark>,
    contentHeightPx: Float,
    heat: Float,
    heatDir: Float,
    focus: Float,
    presence: Float,
    readout: Int?,
    measurer: TextMeasurer,
    readoutStyle: TextStyle,
) {
    val height = size.height
    if (height <= 0f) return
    val axis = size.width / 2f
    val hairline = 1.dp.toPx()

    drawLine(
        color = lerp(CairnColors.GridLine, CairnColors.Scree, focus)
            .copy(alpha = (0.35f + 0.65f * focus) * presence),
        start = Offset(axis, 0f),
        end = Offset(axis, height),
        strokeWidth = hairline,
    )

    val track = (height - thumbPx).coerceAtLeast(1f)
    val top = frac * track
    val bottom = top + thumbPx
    val center = (top + bottom) / 2f
    val bodyWidth = 3.dp.toPx()
    val charge = max(heat, focus)

    // Rod graduations: minor every step, major every fifth. The marker
    // energizes the length of rod it spans (that span is the slice of the
    // document you can see), running those graduations out long and warm,
    // hottest at the jaws, guttering out past them.
    val step = TickSpacing.toPx()
    val inset = 2.dp.toPx()
    val minorLen = 3.dp.toPx()
    val majorLen = 7.dp.toPx()
    val spill = 60.dp.toPx()
    val jawReach = 50.dp.toPx()
    var index = 0
    var y = 0f
    while (y <= height) {
        val major = index % 5 == 0
        val toEdge = min(abs(y - top), abs(y - bottom))
        val outside = if (y in top..bottom) 0f else toEdge
        val inSpan = (1f - outside / spill).coerceIn(0f, 1f)
        val atJaw = (1f - toEdge / jawReach).coerceIn(0f, 1f)
        val lit = charge * inSpan * (0.45f + 0.55f * atJaw)
        val len = (if (major) majorLen else minorLen) + lit * 8.dp.toPx()
        val alpha = ((if (major) 0.22f else 0.11f) + lit * 0.7f) * presence
        drawLine(
            color = lerp(CairnColors.Bone, CairnColors.SparkWarm, lit).copy(alpha = alpha),
            start = Offset(axis - inset - len, y),
            end = Offset(axis - inset, y),
            strokeWidth = hairline + lit * hairline,
        )
        y += step
        index++
    }

    // The legend: one notch per section, in that section's accent. A lit
    // section also lays its color across the rod as a station line.
    marks.forEach { mark ->
        val markY = (mark.contentTop / contentHeightPx).coerceIn(0f, 1f) * height
        if (mark.lit) {
            drawLine(
                color = mark.accent.copy(alpha = 0.30f * presence),
                start = Offset(axis - 7.dp.toPx(), markY),
                end = Offset(axis + inset, markY),
                strokeWidth = hairline,
            )
        }
        drawLine(
            color = mark.accent.copy(alpha = (if (mark.lit) 0.9f else 0.28f) * presence),
            start = Offset(axis + inset, markY),
            end = Offset(axis + inset + (if (mark.lit) 7.dp else 4.dp).toPx(), markY),
            strokeWidth = 2.dp.toPx(),
        )
    }

    if (heat > 0.01f) {
        // Phosphor trail, streaming out behind the direction of travel.
        val trail = (70.dp.toPx() * heat).coerceAtMost(height)
        val from = if (heatDir > 0f) top else bottom
        val to = if (heatDir > 0f) top - trail else bottom + trail
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    CairnColors.SparkWarm.copy(alpha = 0.32f * heat * presence),
                    Color.Transparent,
                ),
                startY = from,
                endY = to,
            ),
            topLeft = Offset(axis - bodyWidth / 2f, min(from, to)),
            size = Size(bodyWidth, abs(to - from)),
        )
    }

    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to CairnColors.Steel,
            0.5f to CairnColors.Bone,
            1f to CairnColors.Steel,
            startY = top,
            endY = bottom,
        ),
        topLeft = Offset(axis - bodyWidth / 2f, top),
        size = Size(bodyWidth, thumbPx),
        cornerRadius = CornerRadius(bodyWidth / 2f),
        alpha = (0.7f + 0.3f * charge) * presence,
    )

    // The filament: a hard white-hot thread up the marker's spine.
    if (charge > 0.01f) {
        drawLine(
            color = CairnColors.SparkWarm.copy(alpha = (0.35f + 0.65f * charge) * presence),
            start = Offset(axis, top + hairline),
            end = Offset(axis, bottom - hairline),
            strokeWidth = hairline,
        )
    }

    // Caliper jaws: the marker reads a span, not a point. They open as the
    // marker charges, and throw a warm cross-tick when it is fully lit.
    val jaw = (8.dp.toPx()) + charge * 5.dp.toPx()
    val jawAlpha = (0.45f + 0.55f * charge) * presence
    listOf(top, bottom).forEach { edge ->
        drawLine(
            color = lerp(CairnColors.Bone, CairnColors.SparkWarm, charge)
                .copy(alpha = jawAlpha),
            start = Offset(axis - jaw, edge),
            end = Offset(axis + jaw, edge),
            strokeWidth = hairline,
        )
    }

    if (readout != null) {
        val text = if (readout < 10) "0$readout%" else "$readout%"
        val layout = measurer.measure(text, readoutStyle)
        drawText(
            textLayoutResult = layout,
            color = CairnColors.BoneDim.copy(alpha = focus * presence),
            topLeft = Offset(
                x = axis - jaw - 6.dp.toPx() - layout.size.width,
                y = center - layout.size.height / 2f,
            ),
        )
    }
}
