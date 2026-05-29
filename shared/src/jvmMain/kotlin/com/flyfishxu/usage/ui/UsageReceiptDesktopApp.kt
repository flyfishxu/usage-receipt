package com.flyfishxu.usage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyfishxu.usage.config.AppConfigRepository
import com.flyfishxu.usage.hooks.HookInstaller
import com.flyfishxu.usage.model.AppConfig
import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.PrinterConfig
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.ReceiptWidth
import com.flyfishxu.usage.model.TokenUsage
import com.flyfishxu.usage.printer.PrinterClient
import com.flyfishxu.usage.receipt.ReceiptRenderer
import com.flyfishxu.usage.storage.UsageHistoryRepository
import com.flyfishxu.usage.usage.HookInput
import com.flyfishxu.usage.usage.UsageAggregator
import com.flyfishxu.usage.usage.UsageSessionCombiner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun UsageReceiptDesktopApp() {
    MaterialTheme(colorScheme = GeekTheme.materialColors) {
        val scope = rememberCoroutineScope()
        val configRepository = remember { AppConfigRepository() }
        val historyRepository = remember { UsageHistoryRepository() }
        val hookInstaller = remember { HookInstaller() }
        val printerClient = remember { PrinterClient() }
        val receiptRenderer = remember { ReceiptRenderer() }
        val sessionCombiner = remember { UsageSessionCombiner() }

        var config by remember { mutableStateOf(configRepository.load()) }
        var sessions by remember { mutableStateOf(emptyList<ConversationUsage>()) }
        var selected by remember { mutableStateOf<ConversationUsage?>(null) }
        var status by remember { mutableStateOf("Ready") }

        fun reloadSessions() {
            val next = historyRepository.recent()
            sessions = next
            selected = selected
                ?.let { current -> next.firstOrNull { it.id == current.id && it.endedAt == current.endedAt } }
                ?: next.firstOrNull()
        }

        fun saveConfig(next: AppConfig) {
            config = next
            configRepository.save(next)
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                reloadSessions()
                delay(2_000)
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = GeekTheme.background) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Header(status = status)
                    PrinterPanel(config, status) { nextPrinter ->
                        saveConfig(config.copy(printer = nextPrinter))
                    }
                    CommandButton(
                        text = "TEST CONNECTION",
                        primary = true,
                        onClick = {
                            scope.launch {
                                status = "Testing printer..."
                                status = withContext(Dispatchers.IO) {
                                    printerClient.testConnection(config.printer)
                                        .fold({ "Printer connection OK" }, { "Printer connection failed: ${it.message}" })
                                }
                            }
                        },
                    )
                    CommandButton(
                        text = "PRINT TEST RECEIPT",
                        onClick = {
                            scope.launch {
                                val sample = sampleUsage()
                                status = withContext(Dispatchers.IO) {
                                    printerClient.print(config.printer, receiptRenderer.render(sample, config.printer.width))
                                        .fold({ "Test receipt sent" }, { "Print failed: ${it.message}" })
                                }
                            }
                        },
                    )
                    HooksPanel(config, hookInstaller) { provider, enabled ->
                        scope.launch {
                            status = if (enabled) "Installing ${provider.displayName} hook..." else "Removing ${provider.displayName} hook..."
                            val result = withContext(Dispatchers.IO) {
                                if (enabled) hookInstaller.install(provider) else hookInstaller.uninstall(provider)
                            }
                            if (result.isSuccess) {
                                val nextHooks = when (provider) {
                                    Provider.OPENAI_CODEX -> config.hooks.copy(codexEnabled = enabled)
                                    Provider.ANTHROPIC_CLAUDE_CODE -> config.hooks.copy(claudeEnabled = enabled)
                                }
                                saveConfig(config.copy(hooks = nextHooks))
                                status = "${provider.displayName} hook ${if (enabled) "installed" else "removed"}"
                            } else {
                                status = "Hook update failed: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    }
                }

                SessionsPanel(
                    sessions = sessions,
                    selected = selected,
                    width = config.printer.width,
                    renderer = receiptRenderer,
                    onSelect = { selected = it },
                    onRefresh = {
                        reloadSessions()
                        status = "Sessions reloaded"
                    },
                    onPrintTurn = { usage ->
                        scope.launch {
                            status = withContext(Dispatchers.IO) {
                                printerClient.print(config.printer, receiptRenderer.render(usage, config.printer.width))
                                    .fold({ "Turn receipt sent" }, { "Print failed: ${it.message}" })
                            }
                        }
                    },
                    onPrintSession = { usage ->
                        scope.launch {
                            val combined = sessionCombiner.combine(historyRepository.findSession(usage.provider, usage.sessionId))
                            if (combined == null) {
                                status = "No session entries found"
                            } else {
                                status = withContext(Dispatchers.IO) {
                                    printerClient.print(config.printer, receiptRenderer.render(combined, config.printer.width))
                                        .fold({ "Session receipt sent" }, { "Print failed: ${it.message}" })
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Header(status = "Ready")
}

@Composable
private fun Header(status: String) {
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

@Composable
private fun PrinterPanel(
    config: AppConfig,
    status: String,
    onPrinterChange: (PrinterConfig) -> Unit,
) {
    Panel(title = "Printer") {
        GeekTextField(
            value = config.printer.host,
            onValueChange = { onPrinterChange(config.printer.copy(host = it.trim())) },
            label = { Text("IP address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        GeekTextField(
            value = config.printer.port.toString(),
            onValueChange = { value -> onPrinterChange(config.printer.copy(port = value.toIntOrNull() ?: config.printer.port)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceiptWidth.entries.forEach { width ->
                Row(
                    modifier = Modifier
                        .border(1.dp, if (config.printer.width == width) GeekTheme.accent else GeekTheme.border)
                        .clickable { onPrinterChange(config.printer.copy(width = width)) }
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = config.printer.width == width,
                        onClick = { onPrinterChange(config.printer.copy(width = width)) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = GeekTheme.accent,
                            unselectedColor = GeekTheme.textMuted,
                        ),
                    )
                    Text(width.label, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace)
                }
            }
        }
        MetricLine("WIDTH", "${config.printer.width.label} / ${config.printer.width.columns} cols")
    }
}

@Composable
private fun HooksPanel(
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

@Composable
private fun SessionsPanel(
    sessions: List<ConversationUsage>,
    selected: ConversationUsage?,
    width: ReceiptWidth,
    renderer: ReceiptRenderer,
    onSelect: (ConversationUsage) -> Unit,
    onRefresh: () -> Unit,
    onPrintTurn: (ConversationUsage) -> Unit,
    onPrintSession: (ConversationUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(title = "Sessions", modifier = Modifier.fillMaxWidth().height(210.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandButton(text = "REFRESH", width = 118.dp, onClick = onRefresh)
                Text(
                    "${sessions.size} captured",
                    color = GeekTheme.textMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id + it.endedAt.toString() }) { usage ->
                    val active = usage == selected
                    SessionRow(
                        usage = usage,
                        active = active,
                        onClick = { onSelect(usage) },
                    )
                }
            }
        }
        Panel(title = "Receipt Preview", modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selected == null) {
                EmptyState()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(selected.provider.displayName.uppercase(), color = GeekTheme.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${selected.items.size} model line(s) / ${selected.totals.totalTokens()} tokens",
                            color = GeekTheme.textMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommandButton(text = "PRINT TURN", width = 128.dp, primary = true, onClick = { onPrintTurn(selected) })
                        CommandButton(text = "PRINT SESSION", width = 148.dp, onClick = { onPrintSession(selected) })
                    }
                }
                HorizontalDivider(color = GeekTheme.border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(GeekTheme.surfaceInset)
                        .padding(18.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = if (width == ReceiptWidth.MM_80) 560.dp else 390.dp)
                            .fillMaxWidth()
                            .background(GeekTheme.receiptPaper)
                            .border(1.dp, Color(0xFF2A2A2A))
                            .padding(18.dp),
                    ) {
                        Text(
                            text = renderer.previewText(selected, width),
                            color = GeekTheme.receiptInk,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    TerminalPanel(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("//", color = GeekTheme.accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(title.uppercase(), color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun TerminalPanel(
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
private fun GeekTextField(
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
        textStyle = androidx.compose.ui.text.TextStyle(
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
private fun CommandButton(
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
private fun SessionRow(
    usage: ConversationUsage,
    active: Boolean,
    onClick: () -> Unit,
) {
    val cost = usage.totals.usdCost?.toPlainString()?.let { "$$it" } ?: "N/A"
    val providerColor = when (usage.provider) {
        Provider.OPENAI_CODEX -> GeekTheme.accent
        Provider.ANTHROPIC_CLAUDE_CODE -> GeekTheme.warning
    }
    Box(
        modifier = Modifier
            .width(320.dp)
            .background(if (active) GeekTheme.activeSurface else GeekTheme.surfaceInset)
            .border(1.dp, if (active) providerColor else GeekTheme.border)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDot(providerColor)
                    Column {
                        Text(usage.provider.displayName, color = GeekTheme.textPrimary, fontWeight = FontWeight.Bold)
                        Text(shortTime(usage), color = GeekTheme.textMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
                Text(cost, color = providerColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricPill("TOK", usage.totals.totalTokens().toString())
                MetricPill("MODELS", usage.items.size.toString())
                MetricPill("SID", usage.sessionId.takeLast(8))
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .background(GeekTheme.background)
            .border(1.dp, GeekTheme.border)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = GeekTheme.textDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Text(value, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = GeekTheme.textDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Text(value, color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GeekTheme.surfaceInset)
            .border(1.dp, GeekTheme.border),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("WAITING FOR HOOK DATA", color = GeekTheme.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("~/.usageReceipt/sessions.jsonl", color = GeekTheme.textMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(color),
    )
}

private fun shortTime(usage: ConversationUsage): String {
    return DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(usage.endedAt)
}

private fun com.flyfishxu.usage.model.UsageTotals.totalTokens(): Long =
    inputTokens + cachedInputTokens + cacheCreationTokens + outputTokens + reasoningOutputTokens

private object GeekTheme {
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

private fun sampleUsage(): ConversationUsage {
    val input = """
        {"session_id":"sample-session","turn_id":"sample-turn","transcript_path":null,"model":"gpt-5.4"}
    """.trimIndent()
    val parserInput = HookInput.from(Provider.OPENAI_CODEX, input)
    val aggregator = UsageAggregator()
    return aggregator.conversation(
        provider = Provider.OPENAI_CODEX,
        sessionId = parserInput.sessionId,
        turnId = parserInput.turnId,
        startedAt = null,
        endedAt = java.time.Instant.now(),
        cwd = null,
        transcriptPath = null,
        rawItems = listOf(
            "gpt-5.3-codex" to TokenUsage(
                inputTokens = 12345,
                cachedInputTokens = 8000,
                outputTokens = 2345
            ),
            "gpt-5.5" to TokenUsage(
                inputTokens = 12345,
                cachedInputTokens = 8000,
                outputTokens = 2345
            )
        ),
    )
}
