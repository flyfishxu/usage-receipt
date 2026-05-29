package com.flyfishxu.usage.storage

import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.TokenUsage
import com.flyfishxu.usage.model.UsageLineItem
import com.flyfishxu.usage.model.UsageTotals
import com.flyfishxu.usage.usage.array
import com.flyfishxu.usage.usage.long
import com.flyfishxu.usage.usage.obj
import com.flyfishxu.usage.usage.parseJsonObjectOrNull
import com.flyfishxu.usage.usage.string
import com.flyfishxu.usage.usage.stringOrNull
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

class UsageHistoryRepository(
    private val file: File = AppPaths.historyFile,
) {
    fun append(usage: ConversationUsage) {
        AppPaths.ensure()
        file.appendText(usage.toJson().toString() + "\n")
    }

    fun recent(limit: Int = 100): List<ConversationUsage> {
        if (!file.exists()) return emptyList()
        return file.useLines { lines ->
            lines.mapNotNull { parseJsonObjectOrNull(it)?.toConversationUsage() }
                .toList()
                .asReversed()
                .take(limit)
        }
    }

    fun findSession(provider: Provider, sessionId: String): List<ConversationUsage> =
        recent(Int.MAX_VALUE)
            .filter { it.provider == provider && it.sessionId == sessionId }
            .asReversed()
}

private fun ConversationUsage.toJson() = buildJsonObject {
    put("provider", provider.name)
    put("sessionId", sessionId)
    turnId?.let { put("turnId", it) }
    startedAt?.let { put("startedAt", it.toString()) }
    put("endedAt", endedAt.toString())
    cwd?.let { put("cwd", it) }
    transcriptPath?.let { put("transcriptPath", it) }
    put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
    put("items", buildJsonArray {
        items.forEach { item ->
            add(buildJsonObject {
                put("model", item.model)
                put("inputTokens", item.usage.inputTokens)
                put("cachedInputTokens", item.usage.cachedInputTokens)
                put("cacheCreationTokens", item.usage.cacheCreationTokens)
                put("outputTokens", item.usage.outputTokens)
                put("reasoningOutputTokens", item.usage.reasoningOutputTokens)
                item.usdCost?.let { put("usdCost", it.toPlainString()) }
                item.pricingSourceUrl?.let { put("pricingSourceUrl", it) }
            })
        }
    })
}

private fun kotlinx.serialization.json.JsonObject.toConversationUsage(): ConversationUsage? {
    val provider = string("provider")?.let { runCatching { Provider.valueOf(it) }.getOrNull() } ?: return null
    val items = array("items").orEmpty().mapNotNull { element ->
        val item = element.obj() ?: return@mapNotNull null
        val usage = TokenUsage(
            inputTokens = item.long("inputTokens"),
            cachedInputTokens = item.long("cachedInputTokens"),
            cacheCreationTokens = item.long("cacheCreationTokens"),
            outputTokens = item.long("outputTokens"),
            reasoningOutputTokens = item.long("reasoningOutputTokens"),
        )
        UsageLineItem(
            model = item.string("model") ?: "unknown",
            usage = usage,
            usdCost = item.string("usdCost")?.let { runCatching { BigDecimal(it) }.getOrNull() },
            pricingSourceUrl = item.string("pricingSourceUrl"),
        )
    }
    val totalCost = items.mapNotNull { it.usdCost }.takeIf { it.size == items.size }
        ?.fold(BigDecimal.ZERO, BigDecimal::add)
    return ConversationUsage(
        provider = provider,
        sessionId = string("sessionId") ?: "unknown",
        turnId = string("turnId"),
        startedAt = string("startedAt")?.toInstantOrNull(),
        endedAt = string("endedAt")?.toInstantOrNull() ?: Instant.now(),
        cwd = string("cwd"),
        transcriptPath = string("transcriptPath"),
        items = items,
        totals = UsageTotals(
            inputTokens = items.sumOf { it.usage.inputTokens },
            cachedInputTokens = items.sumOf { it.usage.cachedInputTokens },
            cacheCreationTokens = items.sumOf { it.usage.cacheCreationTokens },
            outputTokens = items.sumOf { it.usage.outputTokens },
            reasoningOutputTokens = items.sumOf { it.usage.reasoningOutputTokens },
            usdCost = totalCost,
        ),
        warnings = array("warnings").orEmpty().mapNotNull { it.stringOrNull() },
    )
}

private fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }.getOrNull()
