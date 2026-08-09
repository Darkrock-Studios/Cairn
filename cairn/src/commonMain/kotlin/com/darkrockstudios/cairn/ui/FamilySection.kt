package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.GetAction
import com.darkrockstudios.cairn.catalog.InstallSource
import com.darkrockstudios.cairn.catalog.resolveGetAction
import com.darkrockstudios.cairn.effects.GridEffectsState
import com.darkrockstudios.cairn.effects.LocalGridEffects
import com.darkrockstudios.cairn.platform.LocalCairnServices
import com.darkrockstudios.cairn.sound.LocalSoundEngine
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.LocalCairnWideLayout
import com.darkrockstudios.cairn.theme.cairnType
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max

/**
 * "More from Dark Rock": the family cards. Card body → canonical page;
 * GET/OPEN → store routing (wired in the platform-services pass; for now GET
 * also opens the canonical page).
 */
@Composable
internal fun FamilySection(
    apps: List<CairnApp>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val wide = LocalCairnWideLayout.current

    SeamSection(accent = CairnColors.NeutralSeam, modifier = modifier) {
        SectionLabel("More from Dark Rock")
        if (wide) {
            apps.chunked(2).forEach { rowApps ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(bottom = 11.dp),
                ) {
                    rowApps.forEach { app ->
                        FamilyCard(app, onOpenUrl, Modifier.weight(1f).fillMaxHeight())
                    }
                    if (rowApps.size == 1) {
                        // ragged last row: hold the column width
                        Box(Modifier.weight(1f))
                    }
                }
            }
        } else {
            apps.forEach { app ->
                FamilyCard(
                    app,
                    onOpenUrl,
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun FamilyCard(
    app: CairnApp,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = cairnType()
    val shape = RoundedCornerShape(10.dp)
    val effects = LocalGridEffects.current
    val sound = LocalSoundEngine.current
    val services = LocalCairnServices.current
    var cardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val feedback = LocalCairnFeedback.current
    val installed = remember(app, services) {
        services?.availability?.isInstalled(app) == true
    }

    fun pulseAndFeedback() {
        sound?.thock()
        feedback?.tap()
        cardCoords?.let { coords ->
            effects.contentCoords?.let { content ->
                val center = content.localPositionOf(
                    coords,
                    Offset(coords.size.width / 2f, coords.size.height / 2f),
                )
                effects.clickPulse(center, app.accent)
            }
        }
    }

    // Card body → canonical page; GET/OPEN → store routing (DESIGN.md §4).
    fun cardTapped() {
        pulseAndFeedback()
        onOpenUrl(app.canonicalUrl)
    }

    fun getTapped() {
        pulseAndFeedback()
        val action = resolveGetAction(
            app = app,
            source = services?.installSource ?: InstallSource.None,
            installed = installed,
            override = services?.storeOverride,
        )
        when (action) {
            GetAction.Open -> {
                if (services?.availability?.launch(app) != true) {
                    onOpenUrl(app.canonicalUrl)
                }
            }
            is GetAction.OpenUrl -> onOpenUrl(action.url)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .onGloballyPositioned { cardCoords = it }
            .clip(shape)
            .background(CairnColors.Slate.copy(alpha = 0.75f))
            .drawWithContent {
                drawContent()
                drawFoilSheen(app.accent, effects, cardCoords)
            }
            .border(1.dp, lerp(CairnColors.Scree, app.accent, 0.22f), shape)
            .tapNoRipple(::cardTapped)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Image(
            painter = painterResource(app.icon),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            BasicText(
                text = app.name,
                style = type.cardName.copy(
                    color = lerp(CairnColors.Bone, app.accent, 0.45f),
                ),
            )
            BasicText(
                text = app.tagline,
                style = type.cardTagline,
                modifier = Modifier.padding(top = 2.dp),
            )
            BasicText(
                text = if (installed) "installed · ${app.canonicalLabel}" else app.canonicalLabel,
                style = type.cardMeta,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        val getShape = RoundedCornerShape(5.dp)
        BasicText(
            text = if (installed) "OPEN" else "GET",
            style = type.getButton.copy(color = app.accent),
            modifier = Modifier
                .clip(getShape)
                .border(1.dp, lerp(CairnColors.Scree, app.accent, 0.45f), getShape)
                .tapNoRipple(::getTapped)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

/**
 * The pointer-mode foil sheen: an accent-tinted specular that only glints
 * when the attractor is within ~150dp of the card, positioned exactly under
 * it — a local spotlight. (Device tilt uses global-light physics instead;
 * that feed arrives with the tilt pass.)
 */
private fun ContentDrawScope.drawFoilSheen(
    accent: Color,
    effects: GridEffectsState,
    cardCoords: LayoutCoordinates?,
) {
    effects.frameTick.longValue
    val attractor = effects.attractor
    val content = effects.contentCoords
    val self = cardCoords

    if (content == null || self == null || !self.isAttached || !content.isAttached) return

    val local: Offset
    val near: Float
    if (attractor != null) {
        // Pointer physics: a local spotlight — only nearby cards glint.
        local = self.localPositionOf(content, attractor)
        val dx = max(max(-local.x, 0f), local.x - size.width)
        val dy = max(max(-local.y, 0f), local.y - size.height)
        near = (1f - kotlin.math.sqrt(dx * dx + dy * dy) / (150 * density))
            .coerceIn(0f, 1f)
    } else {
        // Tilt physics: ONE lamp hanging over the whole visible surface,
        // steered by the tilt. Each card only glints when the lamp is
        // actually near it — never six identical blobs in lockstep.
        val tilt = effects.tilt
        val strength = (kotlin.math.sqrt(tilt.x * tilt.x + tilt.y * tilt.y) * 1.1f)
            .coerceIn(0f, 1f)
        if (strength <= 0.01f) return
        val view = effects.visibleRect()
        if (view.width <= 0f) return
        val lamp = Offset(
            view.left + view.width * (0.5f + tilt.x * 0.55f),
            view.top + view.height * (0.4f + tilt.y * 0.5f),
        )
        local = self.localPositionOf(content, lamp)
        val dx = max(max(-local.x, 0f), local.x - size.width)
        val dy = max(max(-local.y, 0f), local.y - size.height)
        val falloff = (1f - kotlin.math.sqrt(dx * dx + dy * dy) / (240 * density))
            .coerceIn(0f, 1f)
        near = strength * falloff
    }
    if (near <= 0.01f) return

    val radiusX = 170 * density
    scale(scaleX = 1f, scaleY = (90f / 170f), pivot = local) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to accent.copy(alpha = 0.17f * near),
                0.7f to Color.Transparent,
                center = local,
                radius = radiusX,
            ),
            radius = radiusX,
            center = local,
        )
    }
}
