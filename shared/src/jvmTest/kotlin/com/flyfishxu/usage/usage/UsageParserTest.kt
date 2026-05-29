package com.flyfishxu.usage.usage

import com.flyfishxu.usage.model.Provider
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UsageParserTest {
    @Test
    fun codexParserReadsLastTurnUsage() {
        val transcript = tempJsonl(
            """
            {"timestamp":"2026-05-29T01:00:00Z","type":"event_msg","payload":{"type":"token_count","info":{"last_token_usage":{"input_tokens":1000,"cached_input_tokens":400,"output_tokens":200,"reasoning_output_tokens":50}}},"rate_limits":{"limit_name":"GPT-5.4"}}
            """.trimIndent(),
        )
        val input = HookInput.from(
            Provider.OPENAI_CODEX,
            """{"session_id":"s1","turn_id":"t1","transcript_path":"${transcript.path}","model":"gpt-5.4","cwd":"/tmp"}""",
        )

        val usage = CodexUsageParser().parse(input, ParseMode.TURN)

        assertEquals("s1", usage.sessionId)
        assertEquals("t1", usage.turnId)
        assertEquals(1000, usage.totals.inputTokens)
        assertEquals(400, usage.totals.cachedInputTokens)
        assertEquals(200, usage.totals.outputTokens)
        assertNotNull(usage.totals.usdCost)
    }

    @Test
    fun claudeParserReadsMessageUsage() {
        val transcript = tempJsonl(
            """
            {"timestamp":"2026-05-29T01:00:00Z","type":"assistant","message":{"model":"claude-sonnet-4.5-20250929","usage":{"input_tokens":1200,"cache_read_input_tokens":300,"cache_creation_input_tokens":100,"output_tokens":250}}}
            """.trimIndent(),
        )
        val input = HookInput.from(
            Provider.ANTHROPIC_CLAUDE_CODE,
            """{"session_id":"c1","transcript_path":"${transcript.path}","cwd":"/tmp"}""",
        )

        val usage = ClaudeUsageParser().parse(input, ParseMode.TURN)

        assertEquals("c1", usage.sessionId)
        assertEquals(1200, usage.totals.inputTokens)
        assertEquals(300, usage.totals.cachedInputTokens)
        assertEquals(100, usage.totals.cacheCreationTokens)
        assertEquals(250, usage.totals.outputTokens)
        assertNotNull(usage.totals.usdCost)
    }

    private fun tempJsonl(content: String): File =
        kotlin.io.path.createTempFile("usage-receipt", ".jsonl").toFile().also {
            it.writeText(content + "\n")
            it.deleteOnExit()
        }
}
