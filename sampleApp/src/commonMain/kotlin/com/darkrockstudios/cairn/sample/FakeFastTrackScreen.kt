package com.darkrockstudios.cairn.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A mock of the Fast Track fasting screen — the "host app" the Cairn overlay
 * opens over, so the etch-over-host entrance reads properly in the sample.
 */
@Composable
fun FakeFastTrackScreen(onAboutClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1C0F33), Color(0xFF140B24), Color(0xFF100A1C))
                )
            )
            // Background stays edge-to-edge; content drops below the bars.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Fasting",
                color = Color(0xFFCBB8E8),
                fontSize = 24.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAboutClick) {
                Text(
                    text = "ⓘ",
                    color = Color(0xFFCBB8E8),
                    fontSize = 20.sp,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .border(5.dp, Color(0x8C7E14DC), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "16:42:05",
                    color = Color(0xFF3DDC84),
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = "AUTOPHAGY · STAGE 4",
                color = Color(0xFF8A7AA8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text("◷ Fasting", color = Color(0xFFCBB8E8), fontSize = 12.sp)
            Text("▤ Log", color = Color(0xFF8A7AA8), fontSize = 12.sp)
            Text("◉ Profile", color = Color(0xFF8A7AA8), fontSize = 12.sp)
        }
    }
}
