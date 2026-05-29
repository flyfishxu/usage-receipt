package com.flyfishxu.usage.hooks

import com.flyfishxu.usage.model.Provider
import com.flyfishxu.usage.storage.AppPaths
import com.flyfishxu.usage.usage.UsageJson
import com.flyfishxu.usage.usage.array
import com.flyfishxu.usage.usage.obj
import com.flyfishxu.usage.usage.parseJsonObjectOrNull
import com.flyfishxu.usage.usage.string
import java.io.File
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HookInstaller(
    private val codexHooksFile: File = File(AppPaths.home, ".codex/hooks.json"),
    private val claudeSettingsFile: File = File(AppPaths.home, ".claude/settings.json"),
    private val hookScriptFile: File = File(AppPaths.binDir, "usagereceipt-hook"),
) {
    fun install(provider: Provider): Result<Unit> =
        runCatching {
            val script = ensureHookScript()
            when (provider) {
                Provider.OPENAI_CODEX -> mergeProviderHook(codexHooksFile, provider, script)
                Provider.ANTHROPIC_CLAUDE_CODE -> mergeProviderHook(claudeSettingsFile, provider, script)
            }
        }

    fun uninstall(provider: Provider): Result<Unit> =
        runCatching {
            when (provider) {
                Provider.OPENAI_CODEX -> removeProviderHook(codexHooksFile, provider)
                Provider.ANTHROPIC_CLAUDE_CODE -> removeProviderHook(claudeSettingsFile, provider)
            }
        }

    fun status(provider: Provider): HookStatus {
        val file = when (provider) {
            Provider.OPENAI_CODEX -> codexHooksFile
            Provider.ANTHROPIC_CLAUDE_CODE -> claudeSettingsFile
        }
        if (!file.exists()) return HookStatus(provider, installed = false, configPath = file.path)
        val root = parseJsonObjectOrNull(file.readText()) ?: return HookStatus(provider, installed = false, configPath = file.path, warning = "Config is not valid JSON.")
        return HookStatus(
            provider = provider,
            installed = root.hasManagedCommand(provider),
            configPath = file.path,
        )
    }

    private fun ensureHookScript(): File {
        val script = hookScriptFile
        script.parentFile.mkdirs()
        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        val classPath = System.getProperty("java.class.path")
        script.writeText(
            """
            |#!/bin/sh
            |exec ${javaBin.shellQuote()} -cp ${classPath.shellQuote()} com.flyfishxu.usage.MainKt hook "$@"
            |
            """.trimMargin(),
        )
        script.setExecutable(true, false)
        return script
    }

    private fun mergeProviderHook(file: File, provider: Provider, script: File) {
        file.parentFile.mkdirs()
        val root = if (file.exists()) parseJsonObjectOrNull(file.readText()) ?: JsonObject(emptyMap()) else JsonObject(emptyMap())
        val cleaned = root.withoutManagedCommand(provider)
        val merged = cleaned.withStopHook(provider, script)
        writeWithBackup(file, merged)
    }

    private fun removeProviderHook(file: File, provider: Provider) {
        if (!file.exists()) return
        val root = parseJsonObjectOrNull(file.readText()) ?: return
        writeWithBackup(file, root.withoutManagedCommand(provider))
    }

    private fun writeWithBackup(file: File, root: JsonObject) {
        if (file.exists()) {
            val backup = File(file.parentFile, "${file.name}.usageReceipt.${Instant.now().epochSecond}.bak")
            file.copyTo(backup, overwrite = false)
        }
        file.writeText(UsageJson.encodeToString(JsonObject.serializer(), root))
    }
}

data class HookStatus(
    val provider: Provider,
    val installed: Boolean,
    val configPath: String,
    val warning: String? = null,
)

private const val MANAGED_MARKER = "UsageReceipt"

private fun JsonObject.withStopHook(provider: Provider, script: File): JsonObject {
    val hooks = obj("hooks") ?: JsonObject(emptyMap())
    val stopGroups = hooks.array("Stop").orEmpty()
    val command = "${script.absolutePath.shellQuote()} --provider ${provider.hookId}"
    val receiptGroup = buildJsonObject {
        put("hooks", buildJsonArray {
            add(buildJsonObject {
                put("type", "command")
                put("command", command)
                put("timeout", 30)
                put("statusMessage", "$MANAGED_MARKER printing ${provider.displayName} receipt")
            })
        })
    }
    val newStopGroups = buildJsonArray {
        stopGroups.forEach { add(it) }
        add(receiptGroup)
    }
    val newHooks = JsonObject(hooks + ("Stop" to newStopGroups))
    return JsonObject(this + ("hooks" to newHooks))
}

private fun JsonObject.withoutManagedCommand(provider: Provider): JsonObject {
    val hooks = obj("hooks") ?: return this
    val stopGroups = hooks.array("Stop") ?: return this
    val filteredGroups = buildJsonArray {
        stopGroups.forEach { groupElement ->
            val group = groupElement.obj()
            if (group == null) {
                add(groupElement)
                return@forEach
            }
            val commands = group.array("hooks").orEmpty()
            val filteredCommands = commands.filterNot { it.isManagedCommand(provider) }
            if (filteredCommands.isNotEmpty()) {
                add(JsonObject(group + ("hooks" to JsonArray(filteredCommands))))
            }
        }
    }
    val newHooks = if (filteredGroups.isEmpty()) {
        JsonObject(hooks - "Stop")
    } else {
        JsonObject(hooks + ("Stop" to filteredGroups))
    }
    return JsonObject(this + ("hooks" to newHooks))
}

private fun JsonObject.hasManagedCommand(provider: Provider): Boolean =
    obj("hooks")
        ?.array("Stop")
        .orEmpty()
        .any { group ->
            group.obj()?.array("hooks").orEmpty().any { it.isManagedCommand(provider) }
        }

private fun JsonElement.isManagedCommand(provider: Provider): Boolean {
    val obj = obj() ?: return false
    val command = obj.string("command").orEmpty()
    val status = obj.string("statusMessage").orEmpty()
    return provider.hookId in command &&
        ("usagereceipt-hook" in command || MANAGED_MARKER in status)
}

private fun String.shellQuote(): String =
    "'" + replace("'", "'\"'\"'") + "'"
