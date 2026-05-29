package com.flyfishxu.usage.usage

import com.flyfishxu.usage.model.ConversationUsage
import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.model.TokenUsage
import java.io.File
import java.time.Instant
import kotlinx.serialization.json.JsonObject

enum class ParseMode {
    TURN,
    SESSION,
}

data class HookInput(
    val provider: Provider,
    val sessionId: String,
    val turnId: String?,
    val transcriptPath: String?,
    val cwd: String?,
    val model: String?,
    val eventName: String?,
) {
    companion object {
        fun from(provider: Provider, rawJson: String): HookInput {
            val root = parseJsonObjectOrNull(rawJson) ?: JsonObject(emptyMap())
            return HookInput(
                provider = provider,
                sessionId = root.string("session_id") ?: root.string("sessionId") ?: "unknown",
                turnId = root.string("turn_id") ?: root.string("turnId"),
                transcriptPath = root.string("transcript_path") ?: root.string("transcriptPath"),
                cwd = root.string("cwd"),
                model = root.string("model"),
                eventName = root.string("hook_event_name") ?: root.string("hookEventName"),
            )
        }
    }
}

interface HookUsageParser {
    fun parse(input: HookInput, mode: ParseMode): ConversationUsage
}

class CodexUsageParser(
    private val aggregator: UsageAggregator = UsageAggregator(),
) : HookUsageParser {
    override fun parse(input: HookInput, mode: ParseMode): ConversationUsage {
        val warnings = mutableListOf<String>()
        val transcriptFile = input.transcriptPath?.let(::File)
        val events = transcriptFile.readJsonLines(warnings)
        val items = when (mode) {
            ParseMode.TURN -> listOfNotNull(parseLastTurnUsage(input, events, warnings))
            ParseMode.SESSION -> parseSessionUsage(input, events, warnings)
        }
        return aggregator.conversation(
            provider = Provider.OPENAI_CODEX,
            sessionId = input.sessionId,
            turnId = if (mode == ParseMode.TURN) input.turnId else null,
            startedAt = events.firstOrNull()?.string("timestamp")?.toInstantOrNull(),
            endedAt = Instant.now(),
            cwd = input.cwd,
            transcriptPath = input.transcriptPath,
            rawItems = items,
            warnings = warnings,
        )
    }

    private fun parseLastTurnUsage(
        input: HookInput,
        events: List<JsonObject>,
        warnings: MutableList<String>,
    ): Pair<String, TokenUsage>? {
        val tokenEvents = events.filter { it.obj("payload")?.string("type") == "token_count" }
        val tokenEvent = tokenEvents.lastOrNull()
        if (tokenEvent == null) {
            warnings += "No Codex token_count event found in transcript."
            return null
        }
        val usage = tokenEvent.obj("payload")
            ?.obj("info")
            ?.obj("last_token_usage")
            ?.toCodexUsage()
        if (usage == null || usage.isEmpty()) {
            warnings += "Codex token_count event did not contain last_token_usage."
            return null
        }
        return (input.model ?: tokenEvent.codexModel() ?: "unknown") to usage
    }

    private fun parseSessionUsage(
        input: HookInput,
        events: List<JsonObject>,
        warnings: MutableList<String>,
    ): List<Pair<String, TokenUsage>> {
        val items = events
            .filter { it.obj("payload")?.string("type") == "token_count" }
            .mapNotNull { event ->
                val usage = event.obj("payload")?.obj("info")?.obj("last_token_usage")?.toCodexUsage()
                if (usage == null || usage.isEmpty()) null else (event.codexModel() ?: input.model ?: "unknown") to usage
            }
        if (items.isEmpty()) warnings += "No billable Codex usage was found in transcript."
        return items
    }

    private fun JsonObject.codexModel(): String? =
        obj("rate_limits")?.string("limit_name")
            ?: obj("payload")?.obj("info")?.string("model")

    private fun JsonObject.toCodexUsage(): TokenUsage =
        TokenUsage(
            inputTokens = long("input_tokens"),
            cachedInputTokens = long("cached_input_tokens"),
            outputTokens = long("output_tokens"),
            reasoningOutputTokens = long("reasoning_output_tokens"),
        )
}

class ClaudeUsageParser(
    private val aggregator: UsageAggregator = UsageAggregator(),
) : HookUsageParser {
    override fun parse(input: HookInput, mode: ParseMode): ConversationUsage {
        val warnings = mutableListOf<String>()
        val events = input.transcriptPath?.let(::File).readJsonLines(warnings)
        val allItems = events.flatMap { event -> event.extractClaudeUsageItems() }
        val items = when (mode) {
            ParseMode.TURN -> allItems.takeLast(1)
            ParseMode.SESSION -> allItems
        }
        if (items.isEmpty()) warnings += "No Claude usage fields were found in transcript."

        return aggregator.conversation(
            provider = Provider.ANTHROPIC_CLAUDE_CODE,
            sessionId = input.sessionId,
            turnId = if (mode == ParseMode.TURN) input.turnId else null,
            startedAt = events.firstOrNull()?.string("timestamp")?.toInstantOrNull(),
            endedAt = Instant.now(),
            cwd = input.cwd,
            transcriptPath = input.transcriptPath,
            rawItems = items,
            warnings = warnings,
        )
    }

    private fun JsonObject.extractClaudeUsageItems(): List<Pair<String, TokenUsage>> {
        val message = obj("message")
        val model = message?.string("model") ?: string("model") ?: obj("payload")?.string("model") ?: "unknown"
        val usage = message?.obj("usage") ?: obj("usage") ?: obj("payload")?.obj("usage")
        return usage?.toClaudeUsage()?.takeUnless { it.isEmpty() }?.let { listOf(model to it) }.orEmpty()
    }

    private fun JsonObject.toClaudeUsage(): TokenUsage =
        TokenUsage(
            inputTokens = long("input_tokens"),
            cachedInputTokens = long("cache_read_input_tokens") + long("cached_input_tokens"),
            cacheCreationTokens = long("cache_creation_input_tokens") + long("cache_write_input_tokens"),
            outputTokens = long("output_tokens"),
        )
}

fun parserFor(provider: Provider): HookUsageParser =
    when (provider) {
        Provider.OPENAI_CODEX -> CodexUsageParser()
        Provider.ANTHROPIC_CLAUDE_CODE -> ClaudeUsageParser()
    }

private fun File?.readJsonLines(warnings: MutableList<String>): List<JsonObject> {
    if (this == null) {
        warnings += "Hook input did not include transcript_path."
        return emptyList()
    }
    if (!exists()) {
        warnings += "Transcript does not exist: $path"
        return emptyList()
    }
    return useLines { lines ->
        lines.mapNotNull { line -> parseJsonObjectOrNull(line) }.toList()
    }
}

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }
