package com.flyfishxu.usage.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object GeekTheme {
    val background = Color(0xFF05070A)
    val surface = Color(0xFF0D1218)
    val surfaceInset = Color(0xFF080C10)
    val activeSurface = Color(0xFF10211F)
    val border = Color(0xFF24313C)
    val borderBright = Color(0xFF3C4B58)
    val textPrimary = Color(0xFFE7EDF2)
    val textMuted = Color(0xFF8D9AA5)
    val textDim = Color(0xFF596572)
    val accent = Color(0xFF22F0B0)
    val warning = Color(0xFFFFB86B)
    val receiptPaper = Color(0xFFF5F1E8)
    val receiptInk = Color(0xFF222222)

    val materialColors = darkColorScheme(
        background = background,
        surface = surface,
        primary = accent,
        secondary = warning,
        onBackground = textPrimary,
        onSurface = textPrimary,
        onPrimary = background,
    )
}
