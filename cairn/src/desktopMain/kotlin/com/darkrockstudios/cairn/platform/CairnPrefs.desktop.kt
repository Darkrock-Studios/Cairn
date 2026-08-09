package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

private class DesktopPrefs : CairnPrefs {
    private val node = Preferences.userRoot().node("com/darkrockstudios/cairn")

    override fun getBoolean(key: String, default: Boolean): Boolean =
        node.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        node.putBoolean(key, value)
    }
}

@Composable
internal actual fun rememberCairnPrefs(): CairnPrefs = remember { DesktopPrefs() }
