package com.darkrockstudios.cairn.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.darkrockstudios.cairn.generated.resources.Res
import com.darkrockstudios.cairn.generated.resources.cairn_jetbrains_mono_medium
import com.darkrockstudios.cairn.generated.resources.cairn_jetbrains_mono_regular
import com.darkrockstudios.cairn.generated.resources.cairn_zilla_slab_semibold
import org.jetbrains.compose.resources.Font

/**
 * Cairn's type system: Zilla Slab for display, JetBrains Mono for the
 * letter-spaced micro-labels that carry half the brand.
 */
@Immutable
internal class CairnTypography(
    val display: FontFamily,
    val mono: FontFamily,
) {
    val heroTitle = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.01).em,
        color = CairnColors.Bone,
    )
    val mission = TextStyle(
        fontFamily = mono,
        fontSize = 11.sp,
        letterSpacing = 0.32.em,
        color = CairnColors.BoneDim,
    )
    val ethos = TextStyle(
        fontSize = 14.sp,
        color = CairnColors.Prose,
        lineHeight = 21.sp,
    )
    val chip = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        color = CairnColors.Bone,
    )
    val sectionLabel = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.28.em,
    )
    val appName = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = CairnColors.Bone,
    )
    val versionStamp = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        letterSpacing = 0.06.em,
        color = CairnColors.BoneDim,
    )
    val cardName = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )
    val cardTagline = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = CairnColors.Prose,
    )
    val cardMeta = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        letterSpacing = 0.08.em,
        color = CairnColors.BoneDim,
    )
    val getButton = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.1.em,
    )
    val supportCopy = TextStyle(
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = CairnColors.Prose,
    )
    val fossLine = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        letterSpacing = 0.22.em,
        color = CairnColors.BoneDim,
    )
    val fine = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        letterSpacing = 0.14.em,
        lineHeight = 20.sp,
        color = CairnColors.Steel,
    )
}

@Composable
internal fun rememberCairnTypography(): CairnTypography {
    val zilla = Font(Res.font.cairn_zilla_slab_semibold, weight = FontWeight.SemiBold)
    val monoRegular = Font(Res.font.cairn_jetbrains_mono_regular, weight = FontWeight.Normal)
    val monoMedium = Font(Res.font.cairn_jetbrains_mono_medium, weight = FontWeight.Medium)
    return remember(zilla, monoRegular, monoMedium) {
        CairnTypography(
            display = FontFamily(zilla),
            mono = FontFamily(monoRegular, monoMedium),
        )
    }
}
