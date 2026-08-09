package com.darkrockstudios.cairn.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalCairnTypography = staticCompositionLocalOf<CairnTypography> {
    error("CairnTheme not applied")
}

/** The active section's accent color; sections override it for their children. */
internal val LocalCairnAccent = staticCompositionLocalOf { CairnColors.Steel }

/** Whether content should be wide-layout (centered ~620dp column, 2-col family grid). */
internal val LocalCairnWideLayout = staticCompositionLocalOf { false }

@Composable
internal fun CairnTheme(content: @Composable () -> Unit) {
    val typography = rememberCairnTypography()
    CompositionLocalProvider(
        LocalCairnTypography provides typography,
        content = content,
    )
}

@Composable
internal fun cairnAccent(): Color = LocalCairnAccent.current

@Composable
internal fun cairnType(): CairnTypography = LocalCairnTypography.current
