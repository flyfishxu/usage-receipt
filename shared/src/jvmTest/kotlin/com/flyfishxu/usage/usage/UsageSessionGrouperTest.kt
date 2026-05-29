package com.flyfishxu.usage.usage

import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.TokenUsage
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageSessionGrouperTest {
    @Test
    fun groupsTurnsByProviderAndSessionAndSortsSessionsByLatestTurn() {
        val aggregator = UsageAggregator()
        val firstTurn = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-a",
            turnId = "turn-1",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T01:00:00Z"),
            cwd = "/tmp/a",
            transcriptPath = null,
            rawItems = listOf("gpt-5.5" to TokenUsage(inputTokens = 100, outputTokens = 10)),
        )
        val secondTurn = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-a",
            turnId = "turn-2",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T03:00:00Z"),
            cwd = "/tmp/a",
            transcriptPath = null,
            rawItems = listOf("gpt-5.5" to TokenUsage(inputTokens = 200, outputTokens = 20)),
        )
        val otherSession = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-b",
            turnId = "turn-1",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T02:00:00Z"),
            cwd = "/tmp/b",
            transcriptPath = null,
            rawItems = listOf("gpt-5.5" to TokenUsage(inputTokens = 300, outputTokens = 30)),
        )

        val grouped = UsageSessionGrouper().group(listOf(otherSession, secondTurn, firstTurn))

        assertEquals(listOf("session-a", "session-b"), grouped.map { it.sessionId })
        assertEquals(listOf("turn-1", "turn-2"), grouped.first().turns.map { it.turnId })
        assertEquals(300, grouped.first().totals.inputTokens)
        assertEquals(30, grouped.first().totals.outputTokens)
    }

    @Test
    fun unknownTurnCostMakesSessionCostUnknown() {
        val aggregator = UsageAggregator()
        val knownTurn = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-a",
            turnId = "turn-1",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T01:00:00Z"),
            cwd = null,
            transcriptPath = null,
            rawItems = listOf("gpt-5.5" to TokenUsage(inputTokens = 100, outputTokens = 10)),
        )
        val unknownTurn = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-a",
            turnId = "turn-2",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T02:00:00Z"),
            cwd = null,
            transcriptPath = null,
            rawItems = listOf("unknown-model" to TokenUsage(inputTokens = 100, outputTokens = 10)),
        )

        val grouped = UsageSessionGrouper().group(listOf(knownTurn, unknownTurn))

        assertNull(grouped.single().totals.usdCost)
    }
}
