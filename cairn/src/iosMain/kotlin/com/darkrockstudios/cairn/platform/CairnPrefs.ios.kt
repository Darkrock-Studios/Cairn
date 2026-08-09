package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private class IosPrefs : CairnPrefs {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, default: Boolean): Boolean {
        val fullKey = "cairn-$key"
        return if (defaults.objectForKey(fullKey) != null) {
            defaults.boolForKey(fullKey)
        } else {
            default
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, "cairn-$key")
    }
}

@Composable
internal actual fun rememberCairnPrefs(): CairnPrefs = remember { IosPrefs() }
