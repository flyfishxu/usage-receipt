package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.model.ReceiptWidth
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosBuilder(
    private val width: ReceiptWidth,
) {
    private val out = ByteArrayOutputStream()
    private val charset: Charset = Charsets.UTF_8

    fun initialize(): EscPosBuilder = apply {
        bytes(0x1B, 0x40)
    }

    fun align(alignment: Alignment): EscPosBuilder = apply {
        bytes(0x1B, 0x61, alignment.value)
    }

    fun bold(enabled: Boolean): EscPosBuilder = apply {
        bytes(0x1B, 0x45, if (enabled) 1 else 0)
    }

    fun text(value: String): EscPosBuilder = apply {
        out.write(value.toByteArray(charset))
    }

    fun line(value: String = ""): EscPosBuilder = apply {
        text(value)
        newline()
    }

    fun separator(char: Char = '-'): EscPosBuilder = line(char.toString().repeat(width.columns))

    fun keyValue(key: String, value: String): EscPosBuilder = apply {
        val safeKey = key.take(width.columns - 1)
        val available = (width.columns - safeKey.length).coerceAtLeast(1)
        val safeValue = value.takeLast(available)
        line(safeKey + " ".repeat((width.columns - safeKey.length - safeValue.length).coerceAtLeast(1)) + safeValue)
    }

    fun wrappedLine(text: String): EscPosBuilder = apply {
        wrap(text, width.columns).forEach(::line)
    }

    fun image(image: BufferedImage): EscPosBuilder = apply {
        val mono = image.toMonochrome()
        val widthBytes = (mono.width + 7) / 8
        bytes(0x1D, 0x76, 0x30, 0x00, widthBytes and 0xff, (widthBytes shr 8) and 0xff, mono.height and 0xff, (mono.height shr 8) and 0xff)
        for (y in 0 until mono.height) {
            for (xByte in 0 until widthBytes) {
                var b = 0
                for (bit in 0 until 8) {
                    val x = xByte * 8 + bit
                    if (x < mono.width && mono.getRGB(x, y) and 0x00ffffff == 0) {
                        b = b or (0x80 shr bit)
                    }
                }
                out.write(b)
            }
        }
        newline()
    }

    fun feed(lines: Int): EscPosBuilder = apply {
        bytes(0x1B, 0x64, lines.coerceIn(0, 10))
    }

    fun cut(): EscPosBuilder = apply {
        bytes(0x1D, 0x56, 0x42, 0x00)
    }

    fun bytes(): ByteArray = out.toByteArray()

    private fun newline() {
        out.write('\n'.code)
    }

    private fun bytes(vararg values: Int) {
        values.forEach { out.write(it) }
    }

    enum class Alignment(val value: Int) {
        LEFT(0),
        CENTER(1),
        RIGHT(2),
    }
}

fun wrap(value: String, columns: Int): List<String> {
    if (value.length <= columns) return listOf(value)
    val result = mutableListOf<String>()
    var remaining = value
    while (remaining.length > columns) {
        val splitAt = remaining.take(columns + 1).lastIndexOf(' ').takeIf { it > columns / 2 } ?: columns
        result += remaining.take(splitAt).trimEnd()
        remaining = remaining.drop(splitAt).trimStart()
    }
    if (remaining.isNotEmpty()) result += remaining
    return result
}

private fun BufferedImage.toMonochrome(): BufferedImage {
    val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val rgb = getRGB(x, y)
            val alpha = rgb ushr 24 and 0xff
            val r = rgb ushr 16 and 0xff
            val g = rgb ushr 8 and 0xff
            val b = rgb and 0xff
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            output.setRGB(x, y, if (alpha > 32 && luminance < 180) 0x000000 else 0xffffff)
        }
    }
    return output
}
