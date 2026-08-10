package com.darkrockstudios.cairn.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.cairn.catalog.StudioInfo
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.cairnType

/** Support FOSS development: the heart tile, the pitch, the links. */
@Composable
internal fun SupportSection(
    studio: StudioInfo,
    onOpenUrl: (String) -> Unit,
    onHeartTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = cairnType()

    SeamSection(accent = CairnColors.SupportOrange, modifier = modifier) {
        SectionLabel("Support FOSS Development")
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeartTile(onTap = onHeartTap)
            Column(Modifier.padding(start = 16.dp)) {
                BasicText(
                    text = "ALIVE AND SHIPPING",
                    style = type.fossLine,
                )
                BasicText(
                    text = "Free software, alive and shipping — if it's useful, help it continue.",
                    style = type.supportCopy,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Chip("Patreon", onClick = { onOpenUrl(studio.patreonUrl) })
            Chip("GitHub Sponsors", onClick = { onOpenUrl(studio.sponsorsUrl) })
            // The third way to support: show up.
            Chip("Discord", onClick = { onOpenUrl(studio.discordUrl) })
        }
    }
}

/** The orange heart tile: it beats when tapped (double-thump, 750ms). */
@Composable
internal fun HeartTile(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun beat() {
        scope.launch {
            scale.snapTo(1f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 750
                    1f at 0
                    1.22f at 165 using FastOutSlowInEasing
                    1f at 337 using FastOutSlowInEasing
                    1.16f at 502 using FastOutSlowInEasing
                    1f at 750 using FastOutSlowInEasing
                },
            )
        }
        onTap()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF7A1A), Color(0xFFE04A00))
                )
            )
            .tapNoRipple(::beat),
    ) {
        BasicText(
            text = "❤️",
            style = TextStyle(fontSize = 26.sp),
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}
