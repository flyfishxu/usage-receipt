package com.flyfishxu.usage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyfishxu.usage.ui.theme.GeekTheme

@Composable
fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    TerminalPanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("//", color = GeekTheme.accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(title.uppercase(), color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(GeekTheme.surface)
            .border(1.dp, GeekTheme.border)
            .padding(16.dp),
    ) {
        Column(content = content)
    }
}

@Composable
fun GeekTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit),
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        modifier = modifier,
        textStyle = TextStyle(
            color = GeekTheme.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GeekTheme.textPrimary,
            unfocusedTextColor = GeekTheme.textPrimary,
            focusedBorderColor = GeekTheme.accent,
            unfocusedBorderColor = GeekTheme.borderBright,
            focusedLabelColor = GeekTheme.accent,
            unfocusedLabelColor = GeekTheme.textMuted,
            cursorColor = GeekTheme.accent,
            focusedContainerColor = GeekTheme.surfaceInset,
            unfocusedContainerColor = GeekTheme.surfaceInset,
        ),
    )
}

@Composable
fun CommandButton(
    text: String,
    width: Dp? = null,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (primary) GeekTheme.accent else GeekTheme.surfaceInset
    val foreground = if (primary) GeekTheme.background else GeekTheme.textPrimary
    val border = if (primary) GeekTheme.accent else GeekTheme.borderBright
    Box(
        modifier = Modifier
            .then(if (width == null) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(42.dp)
            .background(background)
            .border(1.dp, border)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
fun SmallCommandButton(
    text: String,
    width: Dp,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (primary) GeekTheme.accent else GeekTheme.surfaceInset
    val foreground = if (primary) GeekTheme.background else GeekTheme.textPrimary
    val border = if (primary) GeekTheme.accent else GeekTheme.borderBright
    Box(
        modifier = Modifier
            .width(width)
            .height(30.dp)
            .background(background)
            .border(1.dp, border)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(34.dp)
            .background(GeekTheme.background)
            .border(1.dp, GeekTheme.border),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = GeekTheme.textDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text(value, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = GeekTheme.textDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Text(value, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GeekTheme.surfaceInset)
            .border(1.dp, GeekTheme.border),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(subtitle, color = GeekTheme.textMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(9.dp)
            .background(color),
    )
}
