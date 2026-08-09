package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.cairnType

@Composable
internal fun CairnFooter(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = cairnType()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 26.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = "© 2026 Dark Rock Studios · open source — for good",
            style = type.fine.copy(textAlign = TextAlign.Center),
        )
        BasicText(
            text = "⛰ built by Wavesonics",
            style = type.fine.copy(color = CairnColors.BoneDim, textAlign = TextAlign.Center),
            modifier = Modifier
                .padding(top = 2.dp)
                .tapNoRipple { onOpenUrl("https://adamwbrown.me") },
        )
        Chip(
            label = if (soundEnabled) "SOUND ON" else "SOUND OFF",
            onClick = onToggleSound,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}
