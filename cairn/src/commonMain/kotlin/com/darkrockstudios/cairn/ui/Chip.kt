package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.theme.cairnType

/** The mono-labeled chip button used for links and actions throughout. */
@Composable
internal fun Chip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(5.dp)
    BasicText(
        text = label,
        style = cairnType().chip,
        modifier = modifier
            .clip(shape)
            .background(CairnColors.Slate.copy(alpha = 0.5f))
            .border(1.dp, CairnColors.Scree, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}
