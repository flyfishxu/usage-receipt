package com.flyfishxu.usage.printer

import com.flyfishxu.usage.model.PrinterConfig
import com.flyfishxu.usage.model.ReceiptWidth
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class PrinterClientTest {
    @Test
    fun sendsReceiptBytesToTcpPrinter() {
        val expected = byteArrayOf(0x1B, 0x40, 0x54, 0x45, 0x53, 0x54)
        ServerSocket(0).use { server ->
            val received = mutableListOf<Byte>()
            val reader = thread {
                server.accept().use { socket ->
                    socket.getInputStream().readBytes().forEach(received::add)
                }
            }

            val config = PrinterConfig(host = "127.0.0.1", port = server.localPort, width = ReceiptWidth.MM_58)
            val result = PrinterClient().print(config, expected)
            reader.join(3_000)

            assertTrue(result.isSuccess)
            assertContentEquals(expected.toList(), received)
        }
    }

    @Test
    fun testConnectionSucceedsAgainstOpenSocket() {
        ServerSocket(0).use { server ->
            val reader = thread {
                server.accept().close()
            }
            val config = PrinterConfig(host = "127.0.0.1", port = server.localPort)
            val result = PrinterClient().testConnection(config)
            reader.join(3_000)
            assertTrue(result.isSuccess)
        }
    }
}
