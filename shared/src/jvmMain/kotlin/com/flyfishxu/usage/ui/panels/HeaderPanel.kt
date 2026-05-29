package com.flyfishxu.usage.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyfishxu.usage.ui.components.StatusDot
import com.flyfishxu.usage.ui.components.TerminalPanel
import com.flyfishxu.usage.ui.theme.GeekTheme

@Composable
fun HeaderPanel(status: String) {
    TerminalPanel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusDot(GeekTheme.accent)
                Text(
                    "Usage Receipt",
                    color = GeekTheme.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                )
            }
            Text(
                "Print AI usage telemetry",
                color = GeekTheme.textMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GeekTheme.surfaceInset)
                    .border(1.dp, GeekTheme.border),
            ) {
                Text(
                    text = "> ${status.take(72)}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = GeekTheme.accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
