package com.darkrockstudios.cairn.catalog

import androidx.compose.ui.graphics.Color
import com.darkrockstudios.cairn.CairnAppId
import com.darkrockstudios.cairn.CairnLink
import com.darkrockstudios.cairn.generated.resources.Res
import com.darkrockstudios.cairn.generated.resources.cairn_icon_c2paverify
import com.darkrockstudios.cairn.generated.resources.cairn_icon_cleancopy
import com.darkrockstudios.cairn.generated.resources.cairn_icon_fasttrack
import com.darkrockstudios.cairn.generated.resources.cairn_icon_fugitive
import com.darkrockstudios.cairn.generated.resources.cairn_icon_hammer
import com.darkrockstudios.cairn.generated.resources.cairn_icon_snapsafe

/**
 * The baked family catalog, v1: hand-written from the website frontmatter
 * (DarkrockStudiosWebsite content/projects). Keep entries flat and literal —
 * a generator will replace this file eventually.
 */
internal val cairnCatalog: List<CairnApp> = listOf(
    CairnApp(
        id = CairnAppId.FastTrack,
        name = "Fast Track",
        tagline = "Track what your body is doing while fasting",
        accent = Color(0xFF7E14DC),
        license = "MIT",
        canonicalUrl = "https://darkrockstudios.com/projects/fasttrack",
        canonicalLabel = "darkrock.studio/fasttrack",
        icon = Res.drawable.cairn_icon_fasttrack,
        githubUrl = "https://github.com/Wavesonics/FastTrack",
        playUrl = "https://play.google.com/store/apps/details?id=com.darkrockstudios.apps.fasttrack",
        fdroidUrl = "https://f-droid.org/en/packages/com.darkrockstudios.apps.fasttrack/",
        androidPackage = "com.darkrockstudios.apps.fasttrack",
    ),
    CairnApp(
        id = CairnAppId.Hammer,
        name = "Hammer",
        tagline = "A simple tool for building stories.",
        accent = Color(0xFFB7410E),
        license = "MIT",
        canonicalUrl = "https://hammer.ink",
        canonicalLabel = "hammer.ink",
        icon = Res.drawable.cairn_icon_hammer,
        githubUrl = "https://github.com/Wavesonics/hammer-editor",
        playUrl = "https://play.google.com/store/apps/details?id=com.darkrockstudios.apps.hammer.android",
        fdroidUrl = "https://f-droid.org/en/packages/com.darkrockstudios.apps.hammer.android/",
        androidPackage = "com.darkrockstudios.apps.hammer.android",
    ),
    CairnApp(
        id = CairnAppId.SnapSafe,
        name = "SnapSafe",
        tagline = "Snap pics of anything, safely.",
        accent = Color(0xFF3DDC84),
        license = "MIT",
        canonicalUrl = "https://snapsafe.org",
        canonicalLabel = "snapsafe.org",
        icon = Res.drawable.cairn_icon_snapsafe,
        githubUrl = "https://github.com/SecureCamera/SecureCameraAndroid",
        playUrl = "https://play.google.com/store/apps/details?id=com.darkrockstudios.app.securecamera",
        fdroidUrl = "https://f-droid.org/en/packages/com.darkrockstudios.app.securecamera/",
        androidPackage = "com.darkrockstudios.app.securecamera",
    ),
    CairnApp(
        id = CairnAppId.Fugitive,
        name = "Fugitive",
        tagline = "Multiplayer hide and seek.",
        accent = Color(0xFFE3A72F),
        license = "MIT",
        canonicalUrl = "https://darkrockstudios.com/projects/fugitive",
        canonicalLabel = "darkrock.studio/fugitive",
        icon = Res.drawable.cairn_icon_fugitive,
        githubUrl = "https://github.com/FugitiveTheGame/Fugitive",
    ),
    CairnApp(
        id = CairnAppId.C2paVerify,
        name = "C2PA Verify",
        tagline = "See who made a photo, and whether you can trust it.",
        accent = Color(0xFF2CB5AA),
        license = "MIT",
        canonicalUrl = "https://darkrockstudios.com/projects/c2paverify",
        canonicalLabel = "darkrock.studio/c2paverify",
        icon = Res.drawable.cairn_icon_c2paverify,
        githubUrl = "https://github.com/Darkrock-Studios/C2PAVerify",
        playUrl = "https://play.google.com/store/apps/details?id=com.darkrockstudios.apps.c2paverify",
        androidPackage = "com.darkrockstudios.apps.c2paverify",
    ),
    CairnApp(
        id = CairnAppId.CleanCopy,
        name = "CleanCopy",
        tagline = "Copy URLs without the junk.",
        accent = Color(0xFF8FA5B5),
        license = "MIT",
        canonicalUrl = "https://darkrockstudios.com/projects/cleancopy",
        canonicalLabel = "darkrock.studio/cleancopy",
        icon = Res.drawable.cairn_icon_cleancopy,
        githubUrl = "https://github.com/Darkrock-Studios/CleanCopy",
    ),
)

internal val cairnStudio = StudioInfo(
    links = listOf(
        CairnLink("Website", "https://darkrockstudios.com"),
        CairnLink("GitHub", "https://github.com/Wavesonics"),
        CairnLink("Discord", "https://discord.gg/ju2RQa5x8W"),
        CairnLink("Mastodon", "https://mastodon.social/@DarkRockStudios"),
        CairnLink("Bluesky", "https://bsky.app/profile/darkrockstudios.bsky.social"),
    ),
    patreonUrl = "https://patreon.com/DarkRockStudios",
    sponsorsUrl = "https://github.com/sponsors/Wavesonics",
    discordUrl = "https://discord.gg/ju2RQa5x8W",
)

internal fun findApp(id: CairnAppId): CairnApp? = cairnCatalog.firstOrNull { it.id == id }
