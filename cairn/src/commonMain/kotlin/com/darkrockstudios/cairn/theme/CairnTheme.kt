package com.darkrockstudios.cairn.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val LocalCairnTypography = staticCompositionLocalOf<CairnTypography> {
    error("CairnTheme not applied")
}

/** The active section's accent color; sections override it for their children. */
internal val LocalCairnAccent = staticCompositionLocalOf { CairnColors.Steel }

/** Whether content should be wide-layout (centered ~620dp column, 2-col family grid). */
internal val LocalCairnWideLayout = staticCompositionLocalOf { false }

/**
 * Right-edge space content must keep clear for the survey rail. Zero unless
 * the rail is showing and the window is too narrow to give it its own gutter.
 * Seams and the grid ignore it: they stay full-bleed and run under the rail.
 */
internal val LocalCairnEndGutter = compositionLocalOf { 0.dp }

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
