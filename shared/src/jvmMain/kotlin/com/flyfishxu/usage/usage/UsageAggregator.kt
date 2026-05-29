package com.flyfishxu.usage.usage

import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.TokenUsage
import com.flyfishxu.usage.model.UsageLineItem
import com.flyfishxu.usage.model.UsageTotals
import com.flyfishxu.usage.pricing.PricingCatalog
import java.math.BigDecimal
import java.time.Instant

class UsageAggregator(
    private val pricingCatalog: PricingCatalog = PricingCatalog(),
) {
    fun lineItem(provider: Provider, model: String, usage: TokenUsage): UsageLineItem {
        val pricing = pricingCatalog.find(provider, model)
        return UsageLineItem(
            model = model,
            usage = usage,
            usdCost = pricing?.let { pricingCatalog.calculateUsd(it, usage) },
            pricingSourceUrl = pricing?.sourceUrl,
        )
    }

    fun conversation(
        provider: Provider,
        sessionId: String,
        turnId: String?,
        startedAt: Instant?,
        endedAt: Instant,
        cwd: String?,
        transcriptPath: String?,
        rawItems: List<Pair<String, TokenUsage>>,
        warnings: List<String> = emptyList(),
    ): ConversationUsage {
        val merged = rawItems
            .filterNot { it.second.isEmpty() }
            .groupBy({ it.first.ifBlank { "unknown" } }, { it.second })
            .map { (model, usageList) -> lineItem(provider, model, usageList.reduce(::mergeUsage)) }

        return ConversationUsage(
            provider = provider,
            sessionId = sessionId.ifBlank { "unknown" },
            turnId = turnId,
            startedAt = startedAt,
            endedAt = endedAt,
            cwd = cwd,
            transcriptPath = transcriptPath,
            items = merged,
            totals = totals(merged),
            warnings = warnings,
        )
    }

    private fun totals(items: List<UsageLineItem>): UsageTotals {
        if (items.isEmpty()) return UsageTotals.Empty
        val knownCosts = items.mapNotNull { it.usdCost }
        val totalCost = if (knownCosts.size == items.size) {
            knownCosts.fold(BigDecimal.ZERO, BigDecimal::add)
        } else {
            null
        }

        return UsageTotals(
            inputTokens = items.sumOf { it.usage.inputTokens },
            cachedInputTokens = items.sumOf { it.usage.cachedInputTokens },
            cacheCreationTokens = items.sumOf { it.usage.cacheCreationTokens },
            outputTokens = items.sumOf { it.usage.outputTokens },
            reasoningOutputTokens = items.sumOf { it.usage.reasoningOutputTokens },
            usdCost = totalCost,
        )
    }

    private fun mergeUsage(left: TokenUsage, right: TokenUsage): TokenUsage =
        TokenUsage(
            inputTokens = left.inputTokens + right.inputTokens,
            cachedInputTokens = left.cachedInputTokens + right.cachedInputTokens,
            cacheCreationTokens = left.cacheCreationTokens + right.cacheCreationTokens,
            outputTokens = left.outputTokens + right.outputTokens,
            reasoningOutputTokens = left.reasoningOutputTokens + right.reasoningOutputTokens,
        )
}
