package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.darkrockstudios.cairn.CairnStore
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.InstallSource

internal fun interface UrlOpener {
    fun open(url: String)
}

internal interface AppAvailability {
    fun isInstalled(app: CairnApp): Boolean

    /** Launch the installed app; false if it couldn't be launched. */
    fun launch(app: CairnApp): Boolean
}

internal fun interface SharePresenter {
    fun share(text: String)
}

@Composable
internal expect fun rememberUrlOpener(): UrlOpener

@Composable
internal expect fun rememberAppAvailability(): AppAvailability

@Composable
internal expect fun rememberInstallSource(): InstallSource

/** Null on platforms with no share sheet — the Share chip hides. */
@Composable
internal expect fun rememberSharePresenter(): SharePresenter?

internal expect fun platformDebugString(): String

/** Everything the sections need to act on the outside world. */
internal class CairnServices(
    val openUrl: UrlOpener,
    val availability: AppAvailability,
    val installSource: InstallSource,
    val share: SharePresenter?,
    val storeOverride: CairnStore?,
)

internal val LocalCairnServices = staticCompositionLocalOf<CairnServices?> { null }
