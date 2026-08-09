package com.darkrockstudios.cairn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.darkrockstudios.cairn.CairnConfig
import com.darkrockstudios.cairn.catalog.CairnApp
import com.darkrockstudios.cairn.catalog.InstallSource
import com.darkrockstudios.cairn.catalog.rateUrlFor
import com.darkrockstudios.cairn.catalog.shareUrlFor
import com.darkrockstudios.cairn.platform.LocalCairnServices
import com.darkrockstudios.cairn.platform.platformDebugString
import com.darkrockstudios.cairn.theme.cairnType
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** "This App": icon, name, version stamp (tap to copy debug info), actions. */
@Composable
internal fun CurrentAppSection(
    app: CairnApp,
    config: CairnConfig,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = cairnType()
    val services = LocalCairnServices.current
    val source = services?.installSource ?: InstallSource.None
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }

    SeamSection(accent = app.accent, showSeam = false, modifier = modifier) {
        SectionLabel("This App")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(app.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(13.dp)),
            )
            Column(Modifier.padding(start = 14.dp)) {
                BasicText(text = app.name, style = type.appName)
                BasicText(
                    text = buildString {
                        append(app.name.uppercase())
                        append(" v")
                        append(config.versionName)
                        append(" · ")
                        append(app.license)
                        append(" · FOSS")
                        if (copied) append("  — copied")
                    },
                    style = if (copied) {
                        type.versionStamp.copy(color = app.accent)
                    } else {
                        type.versionStamp
                    },
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .tapNoRipple {
                            clipboard.setText(AnnotatedString(debugInfo(app, config)))
                            copied = true
                        },
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 18.dp),
        ) {
            // Rate: only ratings-capable installs (Play / App Store).
            rateUrlFor(app, source)?.let { url ->
                Chip("★ Rate", onClick = { onOpenUrl(url) })
            }
            // Share: mobile only, store-aware link.
            services?.share?.let { presenter ->
                Chip("Share", onClick = {
                    presenter.share("${app.name} · ${app.tagline}\n${shareUrlFor(app, source)}")
                })
            }
            app.githubUrl?.let { url ->
                Chip("Source", onClick = { onOpenUrl(url) })
            }
        }
    }
}

private fun debugInfo(app: CairnApp, config: CairnConfig): String = buildString {
    append(app.name)
    append(" v")
    append(config.versionName)
    append('\n')
    append(platformDebugString())
    config.extraDebugInfo.forEach { (key, value) ->
        append('\n')
        append(key)
        append(": ")
        append(value)
    }
}
