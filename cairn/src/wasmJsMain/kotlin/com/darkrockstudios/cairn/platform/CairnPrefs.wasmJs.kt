package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.localStorage

private class WasmPrefs : CairnPrefs {
    override fun getBoolean(key: String, default: Boolean): Boolean =
        when (runCatching { localStorage.getItem("cairn-$key") }.getOrNull()) {
            "1" -> true
            "0" -> false
            else -> default
        }

    override fun putBoolean(key: String, value: Boolean) {
        runCatching { localStorage.setItem("cairn-$key", if (value) "1" else "0") }
    }
}

@Composable
internal actual fun rememberCairnPrefs(): CairnPrefs = remember { WasmPrefs() }
