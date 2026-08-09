package com.darkrockstudios.cairn

import androidx.compose.runtime.Immutable

/**
 * Host-app configuration for the Cairn about screen.
 *
 * @param currentAppId Catalog id of the hosting app (e.g. "fasttrack"); selects the
 *   accent for the "This App" section and excludes it from the family list.
 * @param versionName Version string shown in the terminal stamp, e.g. "5.0.1".
 * @param entrance How much ceremony the entrance plays.
 * @param soundDefault Whether Cairn's sounds start enabled (user mute toggle wins once set).
 * @param hapticsDefault Whether haptic feedback is enabled.
 * @param storeOverride Force a specific store for GET buttons instead of runtime detection.
 * @param extraLinks Additional link chips appended to the identity block.
 * @param extraDebugInfo Extra key/value pairs included in the tap-to-copy debug info.
 */
@Immutable
public class CairnConfig(
    public val currentAppId: String,
    public val versionName: String,
    public val entrance: CairnEntrance = CairnEntrance.Full,
    public val soundDefault: Boolean = true,
    public val hapticsDefault: Boolean = true,
    public val storeOverride: CairnStore? = null,
    public val extraLinks: List<CairnLink> = emptyList(),
    public val extraDebugInfo: Map<String, String> = emptyMap(),
)

public enum class CairnEntrance { Full, Quick, None }

public enum class CairnStore { Play, FDroid, AppStore }

@Immutable
public class CairnLink(
    public val label: String,
    public val url: String,
)
