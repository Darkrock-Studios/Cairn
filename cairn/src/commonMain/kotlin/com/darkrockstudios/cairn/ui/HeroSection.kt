package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.CairnLink
import com.darkrockstudios.cairn.effects.IgniteCues
import com.darkrockstudios.cairn.effects.ignite
import com.darkrockstudios.cairn.generated.resources.Res
import com.darkrockstudios.cairn.generated.resources.cairn_summit
import com.darkrockstudios.cairn.theme.LocalCairnWideLayout
import com.darkrockstudios.cairn.theme.cairnType
import org.jetbrains.compose.resources.painterResource

/** Identity block: the summit, studio name, tagline, ethos, link chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HeroSection(
    links: List<CairnLink>,
    socials: List<CairnLink>,
    onOpenUrl: (String) -> Unit,
    onSummitTap: () -> Unit,
    onSummitPositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = cairnType()
    val wide = LocalCairnWideLayout.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 42.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Summit(
            onTap = onSummitTap,
            modifier = Modifier
                .ignite(IgniteCues.SUMMIT)
                .size(if (wide) 124.dp else 92.dp)
                .onGloballyPositioned(onSummitPositioned),
        )
        BasicText(
            text = "Dark Rock Studios",
            style = type.heroTitle,
            modifier = Modifier
                .ignite(IgniteCues.TITLE)
                .padding(top = 6.dp),
        )
        BasicText(
            text = "OPEN SOURCE, FOR GOOD.",
            style = type.mission,
            modifier = Modifier
                .ignite(IgniteCues.MISSION)
                .padding(top = 11.dp),
        )
        BasicText(
            text = "We make privacy-respecting\nOpen Source Software.",
            style = type.ethos.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .ignite(IgniteCues.ETHOS)
                .padding(top = 18.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .ignite(IgniteCues.CHIPS)
                .padding(top = 19.dp),
        ) {
            links.forEach { link ->
                Chip(label = link.label, onClick = { onOpenUrl(link.url) })
            }
        }
        // Broadcast socials: always their own line under the primary links.
        if (socials.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .ignite(IgniteCues.CHIPS)
                    .padding(top = 8.dp),
            ) {
                socials.forEach { link ->
                    Chip(label = link.label, onClick = { onOpenUrl(link.url) })
                }
            }
        }
    }
}

/** The mountain logo. Tap behavior (strike + shockwave) arrives with effects. */
@Composable
internal fun Summit(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(Res.drawable.cairn_summit),
        contentDescription = "Dark Rock Studios",
        modifier = modifier.tapNoRipple(onTap),
    )
}
