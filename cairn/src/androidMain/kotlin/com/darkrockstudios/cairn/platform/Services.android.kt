package com.darkrockstudios.cairn.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.InstallSource
import java.util.Locale

@Composable
internal actual fun rememberUrlOpener(): UrlOpener {
    val context = LocalContext.current
    return remember(context) {
        UrlOpener { url ->
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

@Composable
internal actual fun rememberAppAvailability(): AppAvailability {
    val context = LocalContext.current
    return remember(context) {
        object : AppAvailability {
            override fun isInstalled(app: CairnApp): Boolean {
                val pkg = app.androidPackage ?: return false
                return context.packageManager.getLaunchIntentForPackage(pkg) != null
            }

            override fun launch(app: CairnApp): Boolean {
                val pkg = app.androidPackage ?: return false
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    ?: return false
                return runCatching {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.isSuccess
            }
        }
    }
}

@Composable
internal actual fun rememberInstallSource(): InstallSource {
    val context = LocalContext.current
    return remember(context) { detectInstallSource(context) }
}

private fun detectInstallSource(context: Context): InstallSource {
    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager
                .getInstallSourceInfo(context.packageName)
                .installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()

    return when (installer) {
        "com.android.vending" -> InstallSource.Play
        "org.fdroid.fdroid", "org.fdroid.basic" -> InstallSource.FDroid
        "com.aurora.store" -> InstallSource.Aurora
        else -> InstallSource.Sideload
    }
}

@Composable
internal actual fun rememberSharePresenter(): SharePresenter? {
    val context = LocalContext.current
    return remember(context) {
        SharePresenter { text ->
            runCatching {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(
                    Intent.createChooser(send, null)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

internal actual fun platformDebugString(): String =
    "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
        "${Build.MANUFACTURER} ${Build.MODEL} · ${Locale.getDefault().toLanguageTag()}"
