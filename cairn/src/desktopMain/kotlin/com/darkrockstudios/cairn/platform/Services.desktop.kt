package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.InstallSource
import java.awt.Desktop
import java.net.URI
import java.util.Locale

@Composable
internal actual fun rememberUrlOpener(): UrlOpener = remember {
    UrlOpener { url ->
        runCatching {
            if (Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
            ) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                // Linux without AWT browse support.
                ProcessBuilder("xdg-open", url).start()
            }
        }
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

internal actual fun platformDebugString(): String = buildString {
    append(System.getProperty("os.name"))
    append(' ')
    append(System.getProperty("os.version"))
    append(" · Java ")
    append(System.getProperty("java.version"))
    append(" · ")
    append(Locale.getDefault().toLanguageTag())
}
