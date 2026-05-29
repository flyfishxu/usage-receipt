package com.flyfishxu.usage.usage

import com.flyfishxu.usage.model.ConversationUsage
import java.time.Instant

class UsageSessionCombiner(
    private val aggregator: UsageAggregator = UsageAggregator(),
) {
    fun combine(entries: List<ConversationUsage>): ConversationUsage? {
        if (entries.isEmpty()) return null
        val first = entries.first()
        val rawItems = entries.flatMap { usage ->
            usage.items.map { item -> item.model to item.usage }
        }
        return aggregator.conversation(
            provider = first.provider,
            sessionId = first.sessionId,
            turnId = null,
            startedAt = entries.mapNotNull { it.startedAt ?: it.endedAt }.minOrNull(),
            endedAt = entries.maxOfOrNull { it.endedAt } ?: Instant.now(),
            cwd = first.cwd,
            transcriptPath = first.transcriptPath,
            rawItems = rawItems,
            warnings = entries.flatMap { it.warnings }.distinct(),
        )
    }
}
