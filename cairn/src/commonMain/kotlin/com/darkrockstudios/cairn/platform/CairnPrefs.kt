package com.darkrockstudios.cairn.platform

import androidx.compose.runtime.Composable

/** Tiny persisted key-value store; Cairn stores exactly one thing: mute. */
internal interface CairnPrefs {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

internal const val PREF_MUTED = "muted"

@Composable
internal expect fun rememberCairnPrefs(): CairnPrefs
