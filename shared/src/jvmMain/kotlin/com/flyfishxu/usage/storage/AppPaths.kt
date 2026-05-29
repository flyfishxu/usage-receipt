package com.flyfishxu.usage.storage

import java.io.File

object AppPaths {
    val home: File = File(System.getProperty("user.home"))
    val appDir: File = File(home, ".usageReceipt")
    val binDir: File = File(appDir, "bin")
    val configFile: File = File(appDir, "config.json")
    val historyFile: File = File(appDir, "sessions.jsonl")

    fun ensure() {
        appDir.mkdirs()
        binDir.mkdirs()
    }
}
