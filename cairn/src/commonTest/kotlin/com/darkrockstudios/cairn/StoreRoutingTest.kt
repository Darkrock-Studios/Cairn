package com.darkrockstudios.cairn

import com.darkrockstudios.cairn.catalog.GetAction
import com.darkrockstudios.cairn.catalog.InstallSource
import com.darkrockstudios.cairn.catalog.cairnCatalog
import com.darkrockstudios.cairn.catalog.findApp
import com.darkrockstudios.cairn.catalog.resolveGetAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StoreRoutingTest {

    private val hammer = findApp(CairnAppId.Hammer)!! // Play + F-Droid + hammer.ink
    private val c2pa = findApp(CairnAppId.C2paVerify)!! // Play only
    private val fugitive = findApp(CairnAppId.Fugitive)!! // no stores at all

    @Test
    fun installedAlwaysWins() {
        InstallSource.entries.forEach { source ->
            assertEquals(
                GetAction.Open,
                resolveGetAction(hammer, source, installed = true),
                "installed should win for $source",
            )
        }
    }

    @Test
    fun playInstallRoutesToPlay() {
        val action = resolveGetAction(hammer, InstallSource.Play, installed = false)
        assertEquals(GetAction.OpenUrl(hammer.playUrl!!), action)
    }

    @Test
    fun fdroidInstallRoutesToFdroid() {
        val action = resolveGetAction(hammer, InstallSource.FDroid, installed = false)
        assertEquals(GetAction.OpenUrl(hammer.fdroidUrl!!), action)
    }

    @Test
    fun fdroidInstallFallsBackToCanonicalForPlayOnlyApp() {
        val action = resolveGetAction(c2pa, InstallSource.FDroid, installed = false)
        assertEquals(GetAction.OpenUrl(c2pa.canonicalUrl), action)
    }

    @Test
    fun auroraNeverRoutesToPlay() {
        val action = resolveGetAction(hammer, InstallSource.Aurora, installed = false)
        assertEquals(GetAction.OpenUrl(hammer.canonicalUrl), action)
    }

    @Test
    fun sideloadRoutesToCanonical() {
        val action = resolveGetAction(hammer, InstallSource.Sideload, installed = false)
        assertEquals(GetAction.OpenUrl(hammer.canonicalUrl), action)
    }

    @Test
    fun desktopAndWebRouteToCanonical() {
        val action = resolveGetAction(hammer, InstallSource.None, installed = false)
        assertEquals(GetAction.OpenUrl(hammer.canonicalUrl), action)
    }

    @Test
    fun storelessAppAlwaysRoutesToCanonical() {
        InstallSource.entries.forEach { source ->
            val action = resolveGetAction(fugitive, source, installed = false)
            assertEquals(
                GetAction.OpenUrl(fugitive.canonicalUrl),
                action,
                "fugitive should route to canonical for $source",
            )
        }
    }

    @Test
    fun overrideForcesStore() {
        val action = resolveGetAction(
            hammer,
            InstallSource.Play,
            installed = false,
            override = CairnStore.FDroid,
        )
        assertEquals(GetAction.OpenUrl(hammer.fdroidUrl!!), action)
    }

    @Test
    fun overrideStillFallsBackToCanonicalWhenStoreMissing() {
        val action = resolveGetAction(
            c2pa,
            InstallSource.Play,
            installed = false,
            override = CairnStore.FDroid,
        )
        assertEquals(GetAction.OpenUrl(c2pa.canonicalUrl), action)
    }

    @Test
    fun rateOnlyOnRatingCapableStores() {
        assertEquals(hammer.playUrl, com.darkrockstudios.cairn.catalog.rateUrlFor(hammer, InstallSource.Play))
        assertEquals(null, com.darkrockstudios.cairn.catalog.rateUrlFor(hammer, InstallSource.FDroid))
        assertEquals(null, com.darkrockstudios.cairn.catalog.rateUrlFor(hammer, InstallSource.Sideload))
        assertEquals(null, com.darkrockstudios.cairn.catalog.rateUrlFor(hammer, InstallSource.Aurora))
        assertEquals(null, com.darkrockstudios.cairn.catalog.rateUrlFor(hammer, InstallSource.None))
    }

    @Test
    fun shareLinkMatchesInstallSource() {
        assertEquals(hammer.playUrl, com.darkrockstudios.cairn.catalog.shareUrlFor(hammer, InstallSource.Play))
        assertEquals(hammer.fdroidUrl, com.darkrockstudios.cairn.catalog.shareUrlFor(hammer, InstallSource.FDroid))
        assertEquals(hammer.canonicalUrl, com.darkrockstudios.cairn.catalog.shareUrlFor(hammer, InstallSource.Sideload))
        assertEquals(c2pa.canonicalUrl, com.darkrockstudios.cairn.catalog.shareUrlFor(c2pa, InstallSource.FDroid))
    }

    @Test
    fun catalogIsWellFormed() {
        assertEquals(6, cairnCatalog.size)
        assertTrue(cairnCatalog.all { it.canonicalUrl.startsWith("https://") })
        assertTrue(cairnCatalog.all { it.canonicalLabel.isNotBlank() })
        // one entry per app, and every app in the enum has one
        assertEquals(cairnCatalog.size, cairnCatalog.map { it.id }.toSet().size)
        assertEquals(CairnAppId.entries.toSet(), cairnCatalog.map { it.id }.toSet())
    }
}
