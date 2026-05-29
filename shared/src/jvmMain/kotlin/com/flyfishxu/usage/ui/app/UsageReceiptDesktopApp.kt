package com.flyfishxu.usage.ui.app

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyfishxu.usage.config.AppConfigRepository
import com.flyfishxu.usage.hooks.HookInstaller
import com.flyfishxu.usage.model.AppConfig
import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.TokenUsage
import com.flyfishxu.usage.printer.PrinterClient
import com.flyfishxu.usage.receipt.ReceiptRenderer
import com.flyfishxu.usage.receipt.toSessionReceipt
import com.flyfishxu.usage.storage.UsageHistoryRepository
import com.flyfishxu.usage.ui.components.CommandButton
import com.flyfishxu.usage.ui.panels.HeaderPanel
import com.flyfishxu.usage.ui.panels.HooksPanel
import com.flyfishxu.usage.ui.panels.PrinterPanel
import com.flyfishxu.usage.ui.panels.ReceiptPreviewModal
import com.flyfishxu.usage.ui.panels.ReceiptPreviewTarget
import com.flyfishxu.usage.ui.panels.SessionExplorerPanel
import com.flyfishxu.usage.ui.panels.SessionReceiptPreviewTarget
import com.flyfishxu.usage.ui.panels.TurnReceiptPreviewTarget
import com.flyfishxu.usage.ui.theme.GeekTheme
import com.flyfishxu.usage.usage.HookInput
import com.flyfishxu.usage.usage.SessionSummary
import com.flyfishxu.usage.usage.UsageAggregator
import com.flyfishxu.usage.usage.UsageSessionGrouper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UsageReceiptDesktopApp() {
    MaterialTheme(colorScheme = GeekTheme.materialColors) {
        val scope = rememberCoroutineScope()
        val configRepository = remember { AppConfigRepository() }
        val historyRepository = remember { UsageHistoryRepository() }
        val hookInstaller = remember { HookInstaller() }
        val printerClient = remember { PrinterClient() }
        val receiptRenderer = remember { ReceiptRenderer() }
        val sessionGrouper = remember { UsageSessionGrouper() }

        var config by remember { mutableStateOf(configRepository.load()) }
        var sessions by remember { mutableStateOf(emptyList<SessionSummary>()) }
        var selectedSession by remember { mutableStateOf<SessionSummary?>(null) }
        var previewTarget by remember { mutableStateOf<ReceiptPreviewTarget?>(null) }
        var status by remember { mutableStateOf("Ready") }

        fun reloadSessions() {
            val next = sessionGrouper.group(historyRepository.recent())
            sessions = next
            selectedSession = selectedSession
                ?.let { current -> next.firstOrNull { it.provider == current.provider && it.sessionId == current.sessionId } }
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
            Box(modifier = Modifier.fillMaxSize()) {
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
                        HeaderPanel(status = status)
                        PrinterPanel(config) { nextPrinter ->
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

                    SessionExplorerPanel(
                        sessions = sessions,
                        selected = selectedSession,
                        onSelectSession = { selectedSession = it },
                        onRefresh = {
                            reloadSessions()
                            status = "Sessions reloaded"
                        },
                        onPrintSession = { session ->
                            scope.launch {
                                status = withContext(Dispatchers.IO) {
                                    printerClient.print(config.printer, receiptRenderer.renderSession(session.toSessionReceipt(), config.printer.width))
                                        .fold({ "Session receipt sent" }, { "Print failed: ${it.message}" })
                                }
                            }
                        },
                        onPreviewSession = { session ->
                            previewTarget = SessionReceiptPreviewTarget(session)
                        },
                        onPrintTurn = { usage ->
                            scope.launch {
                                status = withContext(Dispatchers.IO) {
                                    printerClient.print(config.printer, receiptRenderer.render(usage, config.printer.width))
                                        .fold({ "Turn receipt sent" }, { "Print failed: ${it.message}" })
                                }
                            }
                        },
                        onPreviewTurn = { usage ->
                            previewTarget = TurnReceiptPreviewTarget(usage)
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }

                ReceiptPreviewModal(
                    target = previewTarget,
                    width = config.printer.width,
                    renderer = receiptRenderer,
                    onClose = { previewTarget = null },
                )
            }
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
                outputTokens = 2345,
            ),
            "gpt-5.5" to TokenUsage(
                inputTokens = 12345,
                cachedInputTokens = 8000,
                outputTokens = 2345,
            ),
        ),
    )
}
