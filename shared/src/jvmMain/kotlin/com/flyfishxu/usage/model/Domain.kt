package com.flyfishxu.usage.model

import java.math.BigDecimal
import java.time.Instant

enum class Provider(
    val displayName: String,
    val hookId: String,
) {
    OPENAI_CODEX("OpenAI Codex", "codex"),
    ANTHROPIC_CLAUDE_CODE("Claude Code", "claude"),
}

enum class ReceiptWidth(
    val label: String,
    val columns: Int,
    val printWidthPixels: Int,
) {
    MM_58("58mm", 32, 282),
    MM_80("80mm", 48, 410),
}

data class TokenUsage(
    val inputTokens: Long = 0,
    val cachedInputTokens: Long = 0,
    val cacheCreationTokens: Long = 0,
    val outputTokens: Long = 0,
    val reasoningOutputTokens: Long = 0,
) {
    val billableOutputTokens: Long
        get() = outputTokens

    fun isEmpty(): Boolean =
        inputTokens == 0L &&
            cachedInputTokens == 0L &&
            cacheCreationTokens == 0L &&
            outputTokens == 0L &&
            reasoningOutputTokens == 0L
}

data class UsageLineItem(
    val model: String,
    val usage: TokenUsage,
    val usdCost: BigDecimal?,
    val pricingSourceUrl: String?,
)

data class UsageTotals(
    val inputTokens: Long,
    val cachedInputTokens: Long,
    val cacheCreationTokens: Long,
    val outputTokens: Long,
    val reasoningOutputTokens: Long,
    val usdCost: BigDecimal?,
) {
    companion object {
        val Empty = UsageTotals(0, 0, 0, 0, 0, BigDecimal.ZERO)
    }
}

data class ConversationUsage(
    val provider: Provider,
    val sessionId: String,
    val turnId: String?,
    val startedAt: Instant?,
    val endedAt: Instant,
    val cwd: String?,
    val transcriptPath: String?,
    val items: List<UsageLineItem>,
    val totals: UsageTotals,
    val warnings: List<String> = emptyList(),
) {
    val id: String
        get() = listOf(provider.hookId, sessionId, turnId ?: "session").joinToString(":")
}

data class ModelPricing(
    val provider: Provider,
    val modelPattern: Regex,
    val modelLabel: String,
    val inputPerMTok: BigDecimal,
    val cachedInputPerMTok: BigDecimal?,
    val cacheWritePerMTok: BigDecimal?,
    val outputPerMTok: BigDecimal,
    val sourceUrl: String,
    val effectiveDate: String,
) {
    fun matches(model: String): Boolean = modelPattern.matches(model)
}

data class PrinterConfig(
    val printerId: String = "default",
    val host: String = "127.0.0.1",
    val port: Int = 9100,
    val width: ReceiptWidth = ReceiptWidth.MM_58,
)

data class HookConfig(
    val codexEnabled: Boolean = false,
    val claudeEnabled: Boolean = false,
)

data class AppConfig(
    val printer: PrinterConfig = PrinterConfig(),
    val hooks: HookConfig = HookConfig(),
)
