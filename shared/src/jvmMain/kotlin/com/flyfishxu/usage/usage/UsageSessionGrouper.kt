package com.flyfishxu.usage.usage

import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.UsageTotals
import java.math.BigDecimal
import java.time.Instant

data class TurnSummary(
    val usage: ConversationUsage,
) {
    val provider: Provider = usage.provider
    val sessionId: String = usage.sessionId
    val turnId: String? = usage.turnId
    val startedAt: Instant? = usage.startedAt
    val endedAt: Instant = usage.endedAt
    val totals: UsageTotals = usage.totals
}

data class SessionSummary(
    val provider: Provider,
    val sessionId: String,
    val cwd: String?,
    val startedAt: Instant?,
    val endedAt: Instant,
    val turns: List<TurnSummary>,
    val totals: UsageTotals,
    val warnings: List<String>,
)

class UsageSessionGrouper {
    fun group(entries: List<ConversationUsage>): List<SessionSummary> =
        entries
            .groupBy { SessionKey(it.provider, it.sessionId) }
            .map { (key, usages) -> key.toSummary(usages) }
            .sortedByDescending { it.endedAt }

    private fun SessionKey.toSummary(usages: List<ConversationUsage>): SessionSummary {
        val turns = usages
            .sortedWith(compareBy<ConversationUsage> { it.endedAt }.thenBy { it.turnId.orEmpty() })
            .map(::TurnSummary)
        val first = turns.first().usage
        val latest = turns.maxBy { it.endedAt }.usage

        return SessionSummary(
            provider = provider,
            sessionId = sessionId,
            cwd = latest.cwd ?: first.cwd,
            startedAt = turns.mapNotNull { it.startedAt ?: it.endedAt }.minOrNull(),
            endedAt = turns.maxOfOrNull { it.endedAt } ?: Instant.now(),
            turns = turns,
            totals = turns.map { it.totals }.combineTotals(),
            warnings = turns.flatMap { it.usage.warnings }.distinct(),
        )
    }
}

private data class SessionKey(
    val provider: Provider,
    val sessionId: String,
)

fun Iterable<UsageTotals>.combineTotals(): UsageTotals {
    val totals = toList()
    if (totals.isEmpty()) return UsageTotals.Empty
    val knownCosts = totals.mapNotNull { it.usdCost }
    val totalCost = if (knownCosts.size == totals.size) {
        knownCosts.fold(BigDecimal.ZERO, BigDecimal::add)
    } else {
        null
    }
    return UsageTotals(
        inputTokens = totals.sumOf { it.inputTokens },
        cachedInputTokens = totals.sumOf { it.cachedInputTokens },
        cacheCreationTokens = totals.sumOf { it.cacheCreationTokens },
        outputTokens = totals.sumOf { it.outputTokens },
        reasoningOutputTokens = totals.sumOf { it.reasoningOutputTokens },
        usdCost = totalCost,
    )
}

fun UsageTotals.totalTokens(): Long =
    inputTokens + cachedInputTokens + cacheCreationTokens + outputTokens + reasoningOutputTokens
