package com.flyfishxu.usage.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyfishxu.usage.hooks.HookInstaller
import com.flyfishxu.usage.model.AppConfig
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.ui.components.Panel
import com.flyfishxu.usage.ui.components.StatusDot
import com.flyfishxu.usage.ui.theme.GeekTheme

@Composable
fun HooksPanel(
    config: AppConfig,
    hookInstaller: HookInstaller,
    onToggle: (Provider, Boolean) -> Unit,
) {
    Panel(title = "Hooks") {
        ProviderHookRow(
            provider = Provider.OPENAI_CODEX,
            enabled = config.hooks.codexEnabled,
            installed = hookInstaller.status(Provider.OPENAI_CODEX).installed,
            onToggle = onToggle,
        )
        ProviderHookRow(
            provider = Provider.ANTHROPIC_CLAUDE_CODE,
            enabled = config.hooks.claudeEnabled,
            installed = hookInstaller.status(Provider.ANTHROPIC_CLAUDE_CODE).installed,
            onToggle = onToggle,
        )
    }
}

@Composable
private fun ProviderHookRow(
    provider: Provider,
    enabled: Boolean,
    installed: Boolean,
    onToggle: (Provider, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDot(if (enabled && installed) GeekTheme.accent else if (enabled) GeekTheme.warning else GeekTheme.textDim)
            Column {
                Text(provider.displayName, color = GeekTheme.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    if (installed) "global hook installed" else "hook not installed",
                    color = GeekTheme.textMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
        Checkbox(
            checked = enabled,
            onCheckedChange = { onToggle(provider, it) },
            colors = CheckboxDefaults.colors(
                checkedColor = GeekTheme.accent,
                uncheckedColor = GeekTheme.textMuted,
                checkmarkColor = GeekTheme.background,
            ),
        )
    }
}
