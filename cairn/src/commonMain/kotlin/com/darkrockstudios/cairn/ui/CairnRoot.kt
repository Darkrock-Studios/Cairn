package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.cairn.CairnConfig
import com.darkrockstudios.cairn.catalog.cairnCatalog
import com.darkrockstudios.cairn.catalog.cairnStudio
import com.darkrockstudios.cairn.catalog.findApp
import com.darkrockstudios.cairn.effects.AttractorSource
import com.darkrockstudios.cairn.effects.CeremonyState
import com.darkrockstudios.cairn.effects.IgniteCues
import com.darkrockstudios.cairn.effects.LocalCeremony
import com.darkrockstudios.cairn.effects.LocalGridEffects
import com.darkrockstudios.cairn.effects.LocalSeamRegistry
import com.darkrockstudios.cairn.effects.SeamRegistry
import com.darkrockstudios.cairn.effects.drawGridEffects
import com.darkrockstudios.cairn.effects.drawSurveyGrid
import com.darkrockstudios.cairn.effects.ignite
import com.darkrockstudios.cairn.effects.rememberGridEffects
import com.darkrockstudios.cairn.platform.CairnServices
import com.darkrockstudios.cairn.platform.LocalCairnServices
import com.darkrockstudios.cairn.platform.TiltFilter
import com.darkrockstudios.cairn.platform.TiltSource
import com.darkrockstudios.cairn.platform.rememberAppAvailability
import com.darkrockstudios.cairn.platform.rememberInstallSource
import com.darkrockstudios.cairn.platform.rememberSharePresenter
import com.darkrockstudios.cairn.platform.rememberUrlOpener
import com.darkrockstudios.cairn.sound.LocalSoundEngine
import com.darkrockstudios.cairn.sound.SoundEngine
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.CairnTheme
import com.darkrockstudios.cairn.theme.LocalCairnEndGutter
import com.darkrockstudios.cairn.theme.LocalCairnWideLayout
import com.darkrockstudios.cairn.theme.cairnType

/** Widest the content column ever gets: the 664dp column plus its side padding. */
private val CairnContentMaxWidth = 708.dp

/** Internal orchestrator: layer stack, scroll, input, effects, sections. */
@Composable
internal fun CairnRoot(
    config: CairnConfig,
    onClose: () -> Unit,
    ceremony: CeremonyState,
    sound: SoundEngine,
    modifier: Modifier = Modifier,
) {
    CairnTheme {
        // Background basalt comes from the ceremony host's scrim layer.
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val wide = maxWidth >= 620.dp
            val currentApp = remember(config.currentAppId) { findApp(config.currentAppId) }
            val familyApps = remember(config.currentAppId) {
                cairnCatalog.filter { it.id != config.currentAppId }
            }
            val services = CairnServices(
                openUrl = rememberUrlOpener(),
                availability = rememberAppAvailability(),
                installSource = rememberInstallSource(),
                share = rememberSharePresenter(),
                storeOverride = config.storeOverride,
            )
            val onOpenUrl: (String) -> Unit = services.openUrl::open

            val scrollState = rememberScrollState()
            val effects = rememberGridEffects()
            val seams = remember(effects) { SeamRegistry(effects) }
            var viewportSize by remember { mutableStateOf(Size.Zero) }
            var mouseSeen by remember { mutableStateOf(false) }
            var summitCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

            effects.visibleRect = {
                Rect(
                    left = 0f,
                    top = scrollState.value.toFloat(),
                    right = viewportSize.width,
                    bottom = scrollState.value + viewportSize.height,
                )
            }
            val feedback = rememberCairnFeedback(enabled = config.hapticsDefault)

            // Device tilt (or CairnDebug simulation) feeds parallax, the
            // horizon's holo gleam, the global card sheen, and ambient light.
            val tiltFilter = remember { TiltFilter() }
            TiltSource { rollDeg, pitchDeg ->
                val (nx, ny) = tiltFilter.update(rollDeg, pitchDeg)
                effects.tiltChanged(nx, ny)
            }

            // Seams arm only once the entrance completes, so the first-visible
            // seams ripple as the ceremony's finale; ambient effects likewise.
            effects.ambientEnabled = { ceremony.phase == CeremonyState.Phase.Open }
            seams.onCross = {
                sound.thock()
                feedback.seamTick()
            }
            LaunchedEffect(seams, viewportSize, ceremony.phase) {
                if (ceremony.phase != CeremonyState.Phase.Open) return@LaunchedEffect
                snapshotFlow { scrollState.value }.collect {
                    seams.checkIgnitions()
                }
            }

            effects.summitCenter = provider@{
                val summit = summitCoords ?: return@provider null
                val content = contentCoords ?: return@provider null
                if (!summit.isAttached || !content.isAttached) return@provider null
                content.localPositionOf(
                    summit,
                    Offset(summit.size.width / 2f, summit.size.height / 2f),
                )
            }

            // The rail claims the right gutter. Wide windows already leave one
            // beside the centered column; narrow ones have to give it up.
            val railShowing = mouseSeen && scrollState.maxValue > 0
            val freeGutter = ((maxWidth - CairnContentMaxWidth) / 2).coerceAtLeast(0.dp)
            val endGutter = if (railShowing) {
                (RailFootprint - freeGutter).coerceAtLeast(0.dp)
            } else {
                0.dp
            }

            CompositionLocalProvider(
                LocalCairnEndGutter provides endGutter,
                LocalCairnWideLayout provides wide,
                LocalGridEffects provides effects,
                LocalSeamRegistry provides seams,
                LocalCeremony provides ceremony,
                LocalSoundEngine provides sound,
                LocalCairnFeedback provides feedback,
                LocalCairnServices provides services,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { viewportSize = it.size.toSize() }
                        .verticalScroll(scrollState)
                        .drawBehind {
                            effects.frameTick.longValue
                            drawSurveyGrid(effects.gridSpacingPx, effects.parallaxOffset())
                        }
                        .onGloballyPositioned {
                            contentCoords = it
                            effects.contentCoords = it
                        }
                        .cairnPointerEffects(effects, seams, sound, feedback) { mouseSeen = true },
                ) {
                    // The grid/scrim/seams stay truly edge-to-edge (they draw
                    // behind); only the content column steps clear of the
                    // status/nav bars. Vertical sides only so seams still
                    // bleed through display cutout margins in landscape.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                WindowInsets.safeDrawing
                                    .only(WindowInsetsSides.Vertical)
                                    .asPaddingValues()
                            ),
                    ) {
                        HeroSection(
                            links = cairnStudio.links + config.extraLinks,
                            socials = cairnStudio.socials,
                            onOpenUrl = onOpenUrl,
                            onSummitTap = {
                                effects.summitCenter()?.let { effects.strike(it) }
                                sound.strike()
                                feedback.strike()
                            },
                            onSummitPositioned = { summitCoords = it },
                        )
                        // The tap strip supplies its own vertical breathing room.
                        HorizonSeam()
                        if (currentApp != null) {
                            CurrentAppSection(
                                app = currentApp,
                                config = config,
                                onOpenUrl = onOpenUrl,
                                modifier = Modifier.ignite(IgniteCues.SECTIONS),
                            )
                        }
                        FamilySection(
                            apps = familyApps,
                            onOpenUrl = onOpenUrl,
                            modifier = Modifier.ignite(IgniteCues.SECTIONS),
                        )
                        SupportSection(
                            studio = cairnStudio,
                            onOpenUrl = onOpenUrl,
                            onHeartTap = { feedback.heartbeat() },
                            modifier = Modifier.ignite(IgniteCues.SECTIONS),
                        )
                        CairnFooter(
                            soundEnabled = !sound.muted,
                            onToggleSound = { sound.toggleMuted() },
                            onOpenUrl = onOpenUrl,
                            modifier = Modifier.ignite(IgniteCues.FOOTER),
                        )
                    }

                    // Effects paint above content, in content space, so they
                    // stay grid-aligned while scrolling.
                    Box(
                        Modifier
                            .matchParentSize()
                            .drawBehind {
                                effects.frameTick.longValue
                                drawGridEffects(effects, effects.frameTick.longValue)
                            },
                    )
                }

                // The rail shares the close button's axis: the ✕ caps the rod.
                SurveyRail(
                    scrollState = scrollState,
                    viewportHeightPx = viewportSize.height,
                    visible = mouseSeen && ceremony.phase == CeremonyState.Phase.Open,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 54.dp, bottom = 20.dp, end = RailEdgeInset),
                )

                CloseButton(
                    onClose = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(10.dp),
                )
            }
        }
    }
}

/**
 * Observes all pointer traffic for the attractor (Initial pass, never
 * consuming) and fires click pulses for taps on dead space (Final pass, only
 * when nothing consumed the tap).
 */
private fun Modifier.cairnPointerEffects(
    effects: com.darkrockstudios.cairn.effects.GridEffectsState,
    seams: SeamRegistry,
    sound: SoundEngine,
    feedback: CairnFeedback,
    onMouseSeen: () -> Unit,
): Modifier = this
    .pointerInput(effects) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                val isMouse = change.type == PointerType.Mouse
                if (isMouse) onMouseSeen()
                when (event.type) {
                    PointerEventType.Press,
                    PointerEventType.Move,
                    -> {
                        if (event.type == PointerEventType.Press) {
                            sound.onUserGesture()
                        }
                        val source = if (change.pressed) {
                            AttractorSource.Touch
                        } else {
                            AttractorSource.Hover
                        }
                        effects.attractorMoved(change.position, source)
                        // Crossing ripples are hover-only: on touch, drag-
                        // scrolling sweeps every seam past the held finger,
                        // which pulsed on every divider — felt terrible.
                        if (!change.pressed) seams.onPointer(change.position)
                    }

                    PointerEventType.Release -> {
                        if (!isMouse) {
                            effects.attractorCleared(AttractorSource.Touch)
                            seams.clearSides()
                        } else {
                            effects.attractorMoved(change.position, AttractorSource.Hover)
                        }
                    }

                    PointerEventType.Exit -> {
                        effects.attractorCleared(AttractorSource.Hover)
                        effects.attractorCleared(AttractorSource.Touch)
                        seams.clearSides()
                    }

                    else -> Unit
                }
            }
        }
    }
    .pointerInput(effects) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Final, requireUnconsumed = false)
            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
            if (up != null && !up.isConsumed) {
                effects.clickPulse(up.position)
                sound.tap()
                feedback.tap()
            }
        }
    }

@Composable
private fun CloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CairnColors.Slate.copy(alpha = 0.7f))
            .border(1.dp, CairnColors.Scree, CircleShape)
            .tapNoRipple(onClose),
    ) {
        BasicText(
            text = "✕",
            style = cairnType().chip.copy(fontSize = 13.sp, color = CairnColors.BoneDim),
        )
    }
}

private fun androidx.compose.ui.unit.IntSize.toSize(): Size =
    Size(width.toFloat(), height.toFloat())
