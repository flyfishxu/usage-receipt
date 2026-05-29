package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.ReceiptWidth
import com.flyfishxu.usage.model.UsageLineItem
import com.flyfishxu.usage.usage.totalTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReceiptRenderer(
    private val logoRasterizer: SvgLogoRasterizer = SvgLogoRasterizer(),
) {
    fun render(usage: ConversationUsage, width: ReceiptWidth): ByteArray {
        val builder = EscPosBuilder(width).initialize()
        val logo = logoRasterizer.rasterize(
            provider = usage.provider,
            targetWidth = if (width == ReceiptWidth.MM_58) 160 else 220,
            targetHeight = if (width == ReceiptWidth.MM_58) 54 else 70,
        )
        builder.align(EscPosBuilder.Alignment.CENTER)
        if (logo != null) {
            builder.image(logo)
        } else {
            builder.bold(true).line(usage.provider.receiptTitle()).bold(false)
        }
        builder.line("USAGE RECEIPT")
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.separator('=')
        builder.keyValue("TOOL", usage.provider.displayName)
        builder.keyValue("SESSION", usage.sessionId.shorten(width.columns - 10))
        usage.turnId?.let { builder.keyValue("TURN", it.shorten(width.columns - 7)) }
        builder.keyValue("TIME", TimeFormatter.format(usage.endedAt))
        builder.separator()

        if (usage.items.isEmpty()) {
            builder.wrappedLine("No billable usage was found.")
        } else {
            usage.items.forEachIndexed { index, item ->
                if (index > 0) builder.separator('.')
                builder.wrappedLine(item.model.uppercase())
                builder.keyValue("INPUT", item.usage.inputTokens.tokens())
                if (item.usage.cachedInputTokens > 0) builder.keyValue("CACHED", item.usage.cachedInputTokens.tokens())
                if (item.usage.cacheCreationTokens > 0) builder.keyValue("CACHE WRITE", item.usage.cacheCreationTokens.tokens())
                builder.keyValue("OUTPUT", item.usage.outputTokens.tokens())
                if (item.usage.reasoningOutputTokens > 0) builder.keyValue("REASONING", item.usage.reasoningOutputTokens.tokens())
                builder.keyValue("USD", item.usdCost.usdOrNa())
            }
        }

        builder.separator('=')
        builder.bold(true)
        builder.keyValue("TOTAL TOKENS", (usage.totals.inputTokens + usage.totals.cachedInputTokens + usage.totals.cacheCreationTokens + usage.totals.outputTokens).tokens())
        builder.keyValue("TOTAL USD", usage.totals.usdCost.usdOrNa())
        builder.bold(false)

        if (usage.warnings.isNotEmpty()) {
            builder.separator()
            usage.warnings.forEach { builder.wrappedLine("WARN $it") }
        }

        builder.separator('=')
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.line("THANK YOU FOR CODING")
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.feed(2).cut()
        return builder.bytes()
    }

    fun renderSession(receipt: SessionReceipt, width: ReceiptWidth): ByteArray {
        val builder = EscPosBuilder(width).initialize()
        appendLogo(builder, receipt.provider, width)
        builder.line("SESSION RECEIPT")
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.separator('=')
        builder.keyValue("TOOL", receipt.provider.displayName)
        builder.keyValue("SESSION", receipt.sessionId.shorten(width.columns - 10))
        builder.keyValue("START", receipt.startedAt?.let(TimeFormatter::format) ?: "N/A")
        builder.keyValue("END", TimeFormatter.format(receipt.endedAt))
        builder.separator()

        if (receipt.turns.isEmpty()) {
            builder.wrappedLine("No turns were found.")
        } else {
            receipt.turns.forEachIndexed { index, turn ->
                if (index > 0) builder.separator('-')
                builder.bold(true).keyValue("TURN ${index + 1}", TimeFormatter.format(turn.endedAt)).bold(false)
                turn.turnId?.let { builder.keyValue("TURN ID", it.shorten(width.columns - 10)) }
                appendItems(builder, turn.items)
                builder.keyValue("TOTAL TOKENS", turn.totals.totalTokens().tokens())
            }
        }

        builder.separator('=')
        builder.bold(true)
        builder.keyValue("TOTAL TURNS", receipt.turns.size.toString())
        builder.keyValue("TOTAL TOKENS", receipt.totals.totalTokens().tokens())
        builder.keyValue("TOTAL USD", receipt.totals.usdCost.usdOrNa())
        builder.bold(false)

        if (receipt.warnings.isNotEmpty()) {
            builder.separator()
            receipt.warnings.forEach { builder.wrappedLine("WARN $it") }
        }

        builder.separator('=')
        builder.align(EscPosBuilder.Alignment.CENTER)
        builder.line("THANK YOU FOR CODING")
        builder.align(EscPosBuilder.Alignment.LEFT)
        builder.feed(2).cut()
        return builder.bytes()
    }

    fun previewText(usage: ConversationUsage, width: ReceiptWidth): String {
        val lines = mutableListOf<String>()
        fun line(value: String = "") {
            lines += value
        }
        fun kv(key: String, value: String) {
            val available = width.columns - key.length
            lines += key + " ".repeat((available - value.length).coerceAtLeast(1)) + value.takeLast(available)
        }
        line(usage.provider.receiptTitle())
        line("USAGE RECEIPT")
        line("=".repeat(width.columns))
        kv("TOOL", usage.provider.displayName)
        kv("SESSION", usage.sessionId.shorten(width.columns - 10))
        usage.turnId?.let { kv("TURN", it.shorten(width.columns - 7)) }
        kv("TIME", TimeFormatter.format(usage.endedAt))
        line("-".repeat(width.columns))
        usage.items.forEach {
            line(it.model.uppercase())
            kv("INPUT", it.usage.inputTokens.tokens())
            kv("OUTPUT", it.usage.outputTokens.tokens())
            kv("USD", it.usdCost.usdOrNa())
        }
        line("=".repeat(width.columns))
        kv("TOTAL USD", usage.totals.usdCost.usdOrNa())
        return lines.joinToString("\n")
    }

    fun previewSessionText(receipt: SessionReceipt, width: ReceiptWidth): String {
        val lines = mutableListOf<String>()
        fun line(value: String = "") {
            lines += value
        }
        fun kv(key: String, value: String) {
            val available = width.columns - key.length
            lines += key + " ".repeat((available - value.length).coerceAtLeast(1)) + value.takeLast(available)
        }
        line(receipt.provider.receiptTitle())
        line("SESSION RECEIPT")
        line("=".repeat(width.columns))
        kv("TOOL", receipt.provider.displayName)
        kv("SESSION", receipt.sessionId.shorten(width.columns - 10))
        kv("START", receipt.startedAt?.let(TimeFormatter::format) ?: "N/A")
        kv("END", TimeFormatter.format(receipt.endedAt))
        line("-".repeat(width.columns))
        receipt.turns.forEachIndexed { index, turn ->
            if (index > 0) line("-".repeat(width.columns))
            line("TURN ${index + 1}  ${TimeFormatter.format(turn.endedAt)}")
            turn.turnId?.let { kv("TURN ID", it.shorten(width.columns - 10)) }
            turn.items.forEach { item ->
                line(item.model.uppercase())
                kv("INPUT", item.usage.inputTokens.tokens())
                if (item.usage.cachedInputTokens > 0) kv("CACHED", item.usage.cachedInputTokens.tokens())
                if (item.usage.cacheCreationTokens > 0) kv("CACHE WRITE", item.usage.cacheCreationTokens.tokens())
                kv("OUTPUT", item.usage.outputTokens.tokens())
                if (item.usage.reasoningOutputTokens > 0) kv("REASONING", item.usage.reasoningOutputTokens.tokens())
                kv("USD", item.usdCost.usdOrNa())
            }
            kv("TURN USD", turn.totals.usdCost.usdOrNa())
        }
        line("=".repeat(width.columns))
        kv("TOTAL TURNS", receipt.turns.size.toString())
        kv("TOTAL TOKENS", receipt.totals.totalTokens().tokens())
        kv("TOTAL USD", receipt.totals.usdCost.usdOrNa())
        return lines.joinToString("\n")
    }

    private fun appendLogo(builder: EscPosBuilder, provider: Provider, width: ReceiptWidth) {
        val logo = logoRasterizer.rasterize(
            provider = provider,
            targetWidth = if (width == ReceiptWidth.MM_58) 160 else 220,
            targetHeight = if (width == ReceiptWidth.MM_58) 54 else 70,
        )
        builder.align(EscPosBuilder.Alignment.CENTER)
        if (logo != null) {
            builder.image(logo)
        } else {
            builder.bold(true).line(provider.receiptTitle()).bold(false)
        }
    }

    private fun appendItems(builder: EscPosBuilder, items: List<UsageLineItem>) {
        if (items.isEmpty()) {
            builder.wrappedLine("No billable usage was found.")
            return
        }
        items.forEachIndexed { index, item ->
            if (index > 0) builder.separator('.')
            builder.keyValue("MODEL", item.model.uppercase())
            builder.keyValue("INPUT", item.usage.inputTokens.tokens())
            if (item.usage.cachedInputTokens > 0) builder.keyValue("CACHED", item.usage.cachedInputTokens.tokens())
            if (item.usage.cacheCreationTokens > 0) builder.keyValue("CACHE WRITE", item.usage.cacheCreationTokens.tokens())
            builder.keyValue("OUTPUT", item.usage.outputTokens.tokens())
            if (item.usage.reasoningOutputTokens > 0) builder.keyValue("REASONING", item.usage.reasoningOutputTokens.tokens())
            builder.keyValue("USD", item.usdCost.usdOrNa())
        }
    }
}

private object TimeFormatter {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun format(instant: java.time.Instant): String = formatter.format(instant)
}

private fun Provider.receiptTitle(): String =
    when (this) {
        Provider.OPENAI_CODEX -> "OPENAI"
        Provider.ANTHROPIC_CLAUDE_CODE -> "ANTHROPIC"
    }

private fun String.shorten(max: Int): String =
    if (length <= max) this else "..." + takeLast((max - 3).coerceAtLeast(0))

private fun Long.tokens(): String = "%,d".format(this)

private fun BigDecimal?.usdOrNa(): String =
    this?.setScale(6, RoundingMode.HALF_UP)?.stripTrailingZeros()?.toPlainString()?.let { "$$it" } ?: "N/A"
