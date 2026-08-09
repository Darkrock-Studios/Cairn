package com.darkrockstudios.cairn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.darkrockstudios.cairn.effects.CeremonyState
import com.darkrockstudios.cairn.platform.isReducedMotionEnabled
import com.darkrockstudios.cairn.ui.CairnCeremonyHost
import kotlinx.coroutines.launch

/**
 * The primary entry point: place as the last child of a full-screen Box,
 * above the host app's own content. When [visible] becomes true the entrance
 * ceremony plays over the host's pixels — the survey grid etches over them,
 * basalt floods in beneath the lines, the interface ignites. Closing runs the
 * retreat, then [onDismissed] fires so the host can drop [visible].
 */
@Composable
public fun CairnAboutOverlay(
    visible: Boolean,
    config: CairnConfig,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = isReducedMotionEnabled()
    val ceremony = remember(config.entrance, reducedMotion) {
        CeremonyState(config.entrance, reducedMotion)
    }
    val scope = rememberCoroutineScope()
    var present by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    // Survives configuration changes: if the overlay was already open when
    // the host recreated us, restore straight to Open — never replay the
    // ceremony (or its sound) because the phone rotated.
    val wasOpen = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible && !present) {
            present = true
            closing = false
            if (wasOpen.value) {
                ceremony.snapOpen()
            } else {
                ceremony.runEnter()
                wasOpen.value = true
            }
        } else if (!visible && present && !closing) {
            // Host withdrew visibility directly: retreat, then settle.
            closing = true
            ceremony.runExit()
            present = false
            closing = false
            wasOpen.value = false
        }
    }

    val requestClose: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                ceremony.runExit()
                present = false
                closing = false
                wasOpen.value = false
                onDismissed()
            }
        }
    }

    if (present) {
        // System back runs the retreat instead of popping the host.
        @OptIn(ExperimentalComposeUiApi::class)
        BackHandler(enabled = true) { requestClose() }

        CairnCeremonyHost(
            config = config,
            ceremony = ceremony,
            onClose = requestClose,
            modifier = modifier,
        )
    }
}
