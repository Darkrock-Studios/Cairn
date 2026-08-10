package com.darkrockstudios.cairn

/**
 * The Dark Rock Studios apps Cairn knows about. A host names itself with one
 * of these in [CairnConfig.currentAppId]: it picks the accent for the "This
 * App" block and drops that app from the family list below it.
 *
 * [slug] is the app's key on the website (content/projects/<slug>/index.md),
 * the same key the catalog entries mirror, so a website→Kotlin generator can
 * emit this enum alongside the catalog.
 */
public enum class CairnAppId(public val slug: String) {
    FastTrack("fasttrack"),
    Hammer("hammer"),
    SnapSafe("snapsafe"),
    Fugitive("fugitive"),
    C2paVerify("c2paverify"),
    CleanCopy("cleancopy"),
}
