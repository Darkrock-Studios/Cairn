package com.darkrockstudios.cairn.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.darkrockstudios.cairn.CairnAboutOverlay
import com.darkrockstudios.cairn.CairnConfig

@Composable
fun App() {
    var aboutVisible by remember { mutableStateOf(false) }
    val config = remember { CairnConfig(currentAppId = "fasttrack", versionName = "5.0.1") }

    Box(Modifier.fillMaxSize()) {
        FakeFastTrackScreen(onAboutClick = { aboutVisible = true })
        CairnAboutOverlay(
            visible = aboutVisible,
            config = config,
            onDismissed = { aboutVisible = false },
        )
    }
}
