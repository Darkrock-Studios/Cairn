package com.darkrockstudios.cairn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.darkrockstudios.cairn.effects.CeremonyState
import com.darkrockstudios.cairn.platform.isReducedMotionEnabled
import com.darkrockstudios.cairn.theme.CairnColors
import com.darkrockstudios.cairn.ui.CairnCeremonyHost

/**
 * The Cairn about screen as a standalone full-screen destination. The
 * entrance still plays (over basalt rather than the host's pixels); the exit
 * transition is the host's navigation. For the etch-over-host entrance,
 * prefer [CairnAboutOverlay].
 */
@Composable
public fun CairnAboutScreen(
    config: CairnConfig,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = isReducedMotionEnabled()
    val ceremony = remember(config.entrance, reducedMotion) {
        CeremonyState(config.entrance, reducedMotion)
    }
    LaunchedEffect(ceremony) { ceremony.runEnter() }

    CairnCeremonyHost(
        config = config,
        ceremony = ceremony,
        onClose = onClose,
        modifier = modifier.background(CairnColors.Basalt),
    )
}
