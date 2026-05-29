package com.flyfishxu.usage

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.flyfishxu.usage.hooks.HookCommand
import com.flyfishxu.usage.ui.UsageReceiptDesktopApp

fun main(args: Array<String>) {
    if (args.firstOrNull() == "hook") {
        val stdin = generateSequence(::readLine).joinToString("\n")
        kotlin.system.exitProcess(HookCommand().run(args.drop(1).toTypedArray(), stdin))
    }
    launchDesktop()
}

private fun launchDesktop() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "UsageReceipt",
    ) {
        UsageReceiptDesktopApp()
    }
}
