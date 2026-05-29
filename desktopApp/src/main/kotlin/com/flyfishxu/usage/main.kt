package com.flyfishxu.usage

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.flyfishxu.usage.hooks.HookCommand
import com.flyfishxu.usage.ui.app.UsageReceiptDesktopApp
import java.awt.Dimension

fun main(args: Array<String>) {
    if (args.firstOrNull() == "hook") {
        val stdin = generateSequence(::readLine).joinToString("\n")
        kotlin.system.exitProcess(HookCommand().run(args.drop(1).toTypedArray(), stdin))
    }
    launchDesktop()
}

private fun launchDesktop() = application {
    val windowState = rememberWindowState(width = 1080.dp, height = 720.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "UsageReceipt",
        state = windowState,
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(720, 500)
        }
        UsageReceiptDesktopApp()
    }
}
