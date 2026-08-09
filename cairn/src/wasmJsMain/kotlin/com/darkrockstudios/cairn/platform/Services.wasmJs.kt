package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.InstallSource
import kotlinx.browser.window

@Composable
internal actual fun rememberUrlOpener(): UrlOpener = remember {
    UrlOpener { url ->
        runCatching { window.open(url, "_blank") }
    }
}

@Composable
internal actual fun rememberAppAvailability(): AppAvailability = remember {
    object : AppAvailability {
        override fun isInstalled(app: CairnApp): Boolean = false
        override fun launch(app: CairnApp): Boolean = false
    }
}

@Composable
internal actual fun rememberInstallSource(): InstallSource = InstallSource.None

@Composable
internal actual fun rememberSharePresenter(): SharePresenter? = null

internal actual fun platformDebugString(): String =
    "${window.navigator.userAgent} · ${window.navigator.language}"
