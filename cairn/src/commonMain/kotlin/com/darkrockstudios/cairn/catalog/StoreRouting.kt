package com.darkrockstudios.cairn.catalog

import com.darkrockstudios.cairn.CairnStore

/** Where this installation of the host app came from. */
internal enum class InstallSource {
    Play,
    FDroid,

    /** Aurora Store: Play catalog, but a deliberately de-Googled user. */
    Aurora,
    Sideload,
    AppStore,

    /** Desktop, web, or anything unknowable. */
    None,
}

/** What tapping a family card's GET/OPEN button should do. */
internal sealed interface GetAction {
    /** App is installed: launch it. */
    data object Open : GetAction

    data class OpenUrl(val url: String) : GetAction
}

/**
 * The store-routing rules from DESIGN.md §4:
 *  1. Installed → open the app.
 *  2. Otherwise → this install's store page for that app, if the catalog has one.
 *  3. Otherwise → the app's canonical page. Never wrong.
 *
 * Aurora deliberately routes to canonical rather than Play: don't bounce
 * de-Googled users to Google.
 */
/** The current app's ratings page, if this install came from a rating-capable store. */
internal fun rateUrlFor(app: CairnApp, source: InstallSource): String? = when (source) {
    InstallSource.Play -> app.playUrl
    InstallSource.AppStore -> app.appStoreUrl
    else -> null
}

/** Share the link matching this install's source; canonical otherwise. */
internal fun shareUrlFor(app: CairnApp, source: InstallSource): String = when (source) {
    InstallSource.Play -> app.playUrl ?: app.canonicalUrl
    InstallSource.FDroid -> app.fdroidUrl ?: app.canonicalUrl
    InstallSource.AppStore -> app.appStoreUrl ?: app.canonicalUrl
    else -> app.canonicalUrl
}

internal fun resolveGetAction(
    app: CairnApp,
    source: InstallSource,
    installed: Boolean,
    override: CairnStore? = null,
): GetAction {
    if (installed) return GetAction.Open

    val storeUrl = when (override) {
        CairnStore.Play -> app.playUrl
        CairnStore.FDroid -> app.fdroidUrl
        CairnStore.AppStore -> app.appStoreUrl
        null -> when (source) {
            InstallSource.Play -> app.playUrl
            InstallSource.FDroid -> app.fdroidUrl
            InstallSource.AppStore -> app.appStoreUrl
            InstallSource.Aurora,
            InstallSource.Sideload,
            InstallSource.None -> null
        }
    }

    return GetAction.OpenUrl(storeUrl ?: app.canonicalUrl)
}
