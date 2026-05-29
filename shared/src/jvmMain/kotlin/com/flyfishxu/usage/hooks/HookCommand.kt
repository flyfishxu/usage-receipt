package com.flyfishxu.usage.hooks

import com.flyfishxu.usage.config.AppConfigRepository
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.printer.PrinterClient
import com.flyfishxu.usage.receipt.ReceiptRenderer
import com.flyfishxu.usage.storage.UsageHistoryRepository
import com.flyfishxu.usage.usage.HookInput
import com.flyfishxu.usage.usage.ParseMode
import com.flyfishxu.usage.usage.parserFor

class HookCommand(
    private val configRepository: AppConfigRepository = AppConfigRepository(),
    private val historyRepository: UsageHistoryRepository = UsageHistoryRepository(),
    private val renderer: ReceiptRenderer = ReceiptRenderer(),
    private val printerClient: PrinterClient = PrinterClient(),
) {
    fun run(args: Array<String>, stdin: String): Int {
        val provider = args.providerArg() ?: return fail("Missing --provider codex|claude")
        val mode = if ("--session" in args) ParseMode.SESSION else ParseMode.TURN
        val config = configRepository.load()
        val hookEnabled = when (provider) {
            Provider.OPENAI_CODEX -> config.hooks.codexEnabled
            Provider.ANTHROPIC_CLAUDE_CODE -> config.hooks.claudeEnabled
        }
        if (!hookEnabled) return 0

        val usage = runCatching {
            val input = HookInput.from(provider, stdin)
            parserFor(provider).parse(input, mode)
        }.getOrElse { error ->
            System.err.println("UsageReceipt hook parse failed: ${error.message}")
            return 0
        }

        historyRepository.append(usage)
        val bytes = renderer.render(usage, config.printer.width)
        printerClient.print(config.printer, bytes).onFailure { error ->
            System.err.println("UsageReceipt print failed: ${error.message}")
        }
        return 0
    }

    private fun fail(message: String): Int {
        System.err.println("UsageReceipt hook error: $message")
        return 2
    }
}

private fun Array<String>.providerArg(): Provider? {
    val providerValue = indexOf("--provider")
        .takeIf { it >= 0 && it + 1 < size }
        ?.let { this[it + 1] }
    return when (providerValue?.lowercase()) {
        "codex", "openai", "openai_codex" -> Provider.OPENAI_CODEX
        "claude", "anthropic", "claude_code" -> Provider.ANTHROPIC_CLAUDE_CODE
        else -> null
    }
}
