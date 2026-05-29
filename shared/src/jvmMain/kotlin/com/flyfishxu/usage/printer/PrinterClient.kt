package com.flyfishxu.usage.printer

import com.flyfishxu.usage.model.PrinterConfig
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PrinterClient(
    private val timeout: Duration = 3.seconds,
) {
    fun testConnection(config: PrinterConfig): Result<Unit> =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(config.host, config.port), timeout.inWholeMilliseconds.toInt())
            }
        }

    fun print(config: PrinterConfig, bytes: ByteArray): Result<Unit> =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(config.host, config.port), timeout.inWholeMilliseconds.toInt())
                socket.getOutputStream().use { output ->
                    output.write(bytes)
                    output.flush()
                }
            }
        }
}
