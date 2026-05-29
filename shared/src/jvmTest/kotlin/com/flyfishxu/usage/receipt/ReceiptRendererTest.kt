package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.ReceiptWidth
import com.flyfishxu.usage.model.TokenUsage
import com.flyfishxu.usage.usage.UsageAggregator
import com.flyfishxu.usage.usage.UsageSessionGrouper
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ReceiptRendererTest {
    @Test
    fun rendersReceiptTextForBothWidths() {
        val usage = UsageAggregator().conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-1234567890",
            turnId = "turn-1",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T01:00:00Z"),
            cwd = "/tmp",
            transcriptPath = null,
            rawItems = listOf("gpt-5.4" to TokenUsage(inputTokens = 100, outputTokens = 50)),
        )
        val renderer = ReceiptRenderer()

        val preview58 = renderer.previewText(usage, ReceiptWidth.MM_58)
        val preview80 = renderer.previewText(usage, ReceiptWidth.MM_80)
        val bytes = renderer.render(usage, ReceiptWidth.MM_58)

        assertContains(preview58, "USAGE RECEIPT")
        assertContains(preview80, "TOTAL USD")
        assertTrue(bytes.size > 100)
        assertTrue(bytes.take(2) == listOf(0x1B.toByte(), 0x40.toByte()))
    }

    @Test
    fun rendersStructuredSessionReceiptWithTurnSections() {
        val aggregator = UsageAggregator()
        val turn1 = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-1234567890",
            turnId = "turn-1",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T01:00:00Z"),
            cwd = "/tmp",
            transcriptPath = null,
            rawItems = listOf("gpt-5.5" to TokenUsage(inputTokens = 100, outputTokens = 50)),
        )
        val turn2 = aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = "session-1234567890",
            turnId = "turn-2",
            startedAt = null,
            endedAt = Instant.parse("2026-05-29T02:00:00Z"),
            cwd = "/tmp",
            transcriptPath = null,
            rawItems = listOf("gpt-5.3-codex" to TokenUsage(inputTokens = 200, outputTokens = 75)),
        )
        val session = UsageSessionGrouper().group(listOf(turn2, turn1)).single().toSessionReceipt()
        val renderer = ReceiptRenderer()

        val preview = renderer.previewSessionText(session, ReceiptWidth.MM_80)
        val bytes = renderer.renderSession(session, ReceiptWidth.MM_58)

        assertContains(preview, "SESSION RECEIPT")
        assertContains(preview, "TURN 1")
        assertContains(preview, "TURN 2")
        assertContains(preview, "GPT-5.5")
        assertContains(preview, "GPT-5.3-CODEX")
        assertContains(preview, "TOTAL TURNS")
        assertContains(preview, "TOTAL USD")
        assertTrue(bytes.size > 100)
        assertTrue(bytes.take(2) == listOf(0x1B.toByte(), 0x40.toByte()))
    }
}
