package com.darkrockstudios.cairn.catalog

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.darkrockstudios.cairn.CairnLink
import org.jetbrains.compose.resources.DrawableResource

/**
 * One app in the Dark Rock Studios family.
 *
 * Field names deliberately mirror the website's project frontmatter
 * (content/projects/<id>/index.md) so a website→Kotlin generator can emit
 * [cairnCatalog] mechanically later.
 */
@Immutable
internal data class CairnApp(
    val id: String,
    val name: String,
    val tagline: String,
    val accent: Color,
    val license: String,
    /** Where a card-body tap goes; also the universal GET fallback. */
    val canonicalUrl: String,
    /** The mono meta line, e.g. "hammer.ink" or "darkrock.studio/fugitive". */
    val canonicalLabel: String,
    val icon: DrawableResource,
    val githubUrl: String? = null,
    val playUrl: String? = null,
    val fdroidUrl: String? = null,
    val appStoreUrl: String? = null,
    val androidPackage: String? = null,
    val iosScheme: String? = null,
)

@Immutable
internal data class StudioInfo(
    val links: List<CairnLink>,
    val patreonUrl: String,
    val sponsorsUrl: String,
)
