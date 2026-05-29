package com.flyfishxu.usage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flyfishxu.usage.config.AppConfigRepository
import com.flyfishxu.usage.hooks.HookInstaller
import com.flyfishxu.usage.model.AppConfig
import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.HookConfig
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UsageReceiptDesktopApp() {
    MaterialTheme {
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
            sessions = historyRepository.recent()
            if (selected == null) selected = sessions.firstOrNull()
        }

        fun saveConfig(next: AppConfig) {
            config = next
            configRepository.save(next)
        }

        LaunchedEffect(Unit) {
            reloadSessions()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.width(380.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Header()
                    PrinterPanel(config, status) { nextPrinter ->
                        saveConfig(config.copy(printer = nextPrinter))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                status = "Testing printer..."
                                status = withContext(Dispatchers.IO) {
                                    printerClient.testConnection(config.printer)
                                        .fold({ "Printer connection OK" }, { "Printer connection failed: ${it.message}" })
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Test Connection")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val sample = sampleUsage()
                                status = withContext(Dispatchers.IO) {
                                    printerClient.print(config.printer, receiptRenderer.render(sample, config.printer.width))
                                        .fold({ "Test receipt sent" }, { "Print failed: ${it.message}" })
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Print Test Receipt")
                    }
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
    Column {
        Text("UsageReceipt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("AI coding usage receipts over ESC/POS", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrinterPanel(
    config: AppConfig,
    status: String,
    onPrinterChange: (PrinterConfig) -> Unit,
) {
    Panel(title = "Printer") {
        OutlinedTextField(
            value = config.printer.host,
            onValueChange = { onPrinterChange(config.printer.copy(host = it.trim())) },
            label = { Text("IP address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = config.printer.port.toString(),
            onValueChange = { value -> onPrinterChange(config.printer.copy(port = value.toIntOrNull() ?: config.printer.port)) },
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ReceiptWidth.entries.forEach { width ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.printer.width == width,
                        onClick = { onPrinterChange(config.printer.copy(width = width)) },
                    )
                    Text(width.label)
                }
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column {
            Text(provider.displayName, fontWeight = FontWeight.Medium)
            Text(if (installed) "Installed globally" else "Not installed", style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(checked = enabled, onCheckedChange = { onToggle(provider, it) })
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Panel(title = "Sessions", modifier = Modifier.width(330.dp).fillMaxHeight()) {
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id + it.endedAt.toString() }) { usage ->
                    val active = usage == selected
                    Card(
                        onClick = { onSelect(usage) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(usage.provider.displayName, fontWeight = FontWeight.SemiBold)
                            Text(usage.sessionId.takeLast(16), style = MaterialTheme.typography.bodySmall)
                            Text(usage.totals.usdCost?.toPlainString()?.let { "$$it" } ?: "N/A", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        Panel(title = "Receipt Preview", modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selected == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No usage captured yet")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onPrintTurn(selected) }) {
                        Text("Print Turn")
                    }
                    Button(onClick = { onPrintSession(selected) }) {
                        Text("Print Session")
                    }
                }
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFF8F6EF))
                        .padding(16.dp),
                ) {
                    Text(
                        text = renderer.previewText(selected, width),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
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
