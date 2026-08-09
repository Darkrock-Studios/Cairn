package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.InstallSource
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

@Composable
internal actual fun rememberUrlOpener(): UrlOpener = remember {
    UrlOpener { url ->
        runCatching {
            NSURL.URLWithString(url)?.let { nsUrl ->
                UIApplication.sharedApplication.openURL(
                    nsUrl,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null,
                )
            }
        }
    }
}

@Composable
internal actual fun rememberAppAvailability(): AppAvailability = remember {
    object : AppAvailability {
        override fun isInstalled(app: CairnApp): Boolean {
            val scheme = app.iosScheme ?: return false
            val url = NSURL.URLWithString("$scheme://") ?: return false
            // Requires the host to declare LSApplicationQueriesSchemes;
            // silently false otherwise.
            return UIApplication.sharedApplication.canOpenURL(url)
        }

        override fun launch(app: CairnApp): Boolean {
            val scheme = app.iosScheme ?: return false
            val url = NSURL.URLWithString("$scheme://") ?: return false
            if (!UIApplication.sharedApplication.canOpenURL(url)) return false
            UIApplication.sharedApplication.openURL(
                url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null,
            )
            return true
        }
    }
}

@Composable
internal actual fun rememberInstallSource(): InstallSource = InstallSource.AppStore

@Composable
internal actual fun rememberSharePresenter(): SharePresenter? = remember {
    SharePresenter { text ->
        runCatching {
            val controller = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(controller, animated = true, completion = null)
        }
    }
}

internal actual fun platformDebugString(): String {
    val device = UIDevice.currentDevice
    return "${device.systemName} ${device.systemVersion} · ${device.model} · " +
        NSLocale.currentLocale.localeIdentifier
}
