package com.flyfishxu.usage.config

import com.flyfishxu.usage.model.AppConfig
import com.flyfishxu.usage.model.HookConfig
import com.flyfishxu.usage.model.PrinterConfig
import com.flyfishxu.usage.model.ReceiptWidth
import com.flyfishxu.usage.storage.AppPaths
import com.flyfishxu.usage.usage.UsageJson
import com.flyfishxu.usage.usage.boolean
import com.flyfishxu.usage.usage.obj
import com.flyfishxu.usage.usage.parseJsonObjectOrNull
import com.flyfishxu.usage.usage.string
import java.io.File
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppConfigRepository(
    private val file: File = AppPaths.configFile,
) {
    fun load(): AppConfig {
        AppPaths.ensure()
        if (!file.exists()) return AppConfig()
        val root = parseJsonObjectOrNull(file.readText()) ?: return AppConfig()
        val printer = root.obj("printer")
        val hooks = root.obj("hooks")
        return AppConfig(
            printer = PrinterConfig(
                printerId = printer?.string("printerId") ?: "default",
                host = printer?.string("host") ?: "127.0.0.1",
                port = printer?.string("port")?.toIntOrNull() ?: 9100,
                width = printer?.string("width")?.let { value ->
                    ReceiptWidth.entries.firstOrNull { it.name == value }
                } ?: ReceiptWidth.MM_58,
            ),
            hooks = HookConfig(
                codexEnabled = hooks?.boolean("codexEnabled") ?: false,
                claudeEnabled = hooks?.boolean("claudeEnabled") ?: false,
            ),
        )
    }

    fun save(config: AppConfig) {
        AppPaths.ensure()
        val root = buildJsonObject {
            put("printer", buildJsonObject {
                put("printerId", config.printer.printerId)
                put("host", config.printer.host)
                put("port", config.printer.port.toString())
                put("width", config.printer.width.name)
            })
            put("hooks", buildJsonObject {
                put("codexEnabled", config.hooks.codexEnabled)
                put("claudeEnabled", config.hooks.claudeEnabled)
            })
        }
        file.writeText(root.toString())
    }
}
