package com.darkrockstudios.cairn.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private class AndroidPrefs(context: Context) : CairnPrefs {
    private val prefs = context.getSharedPreferences("cairn", Context.MODE_PRIVATE)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

@Composable
internal actual fun rememberCairnPrefs(): CairnPrefs {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidPrefs(context) }
}
