package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.model.Provider
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder

class SvgLogoRasterizer(
    private val openAiSvg: File = File("/Users/flyfishxu/Downloads/openai.svg"),
    private val anthropicSvg: File = File("/Users/flyfishxu/Downloads/anthropic.svg"),
) {
    fun rasterize(provider: Provider, targetWidth: Int, targetHeight: Int): BufferedImage? {
        val file = when (provider) {
            Provider.OPENAI_CODEX -> openAiSvg
            Provider.ANTHROPIC_CLAUDE_CODE -> anthropicSvg
        }
        if (!file.exists()) return null

        val png = ByteArrayOutputStream()
        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(PNGTranscoder.KEY_WIDTH, targetWidth.toFloat())
            addTranscodingHint(PNGTranscoder.KEY_HEIGHT, targetHeight.toFloat())
            addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE)
        }
        file.inputStream().use { input ->
            transcoder.transcode(TranscoderInput(input), TranscoderOutput(png))
        }
        val image = ImageIO.read(ByteArrayInputStream(png.toByteArray())) ?: return null
        return image.cropWhitespace(padding = 2)
    }
}

private fun BufferedImage.cropWhitespace(padding: Int): BufferedImage {
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (isInk(x, y)) {
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
    }
    if (maxX < minX || maxY < minY) return this
    minX = (minX - padding).coerceAtLeast(0)
    minY = (minY - padding).coerceAtLeast(0)
    maxX = (maxX + padding).coerceAtMost(width - 1)
    maxY = (maxY + padding).coerceAtMost(height - 1)
    return getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1)
}

private fun BufferedImage.isInk(x: Int, y: Int): Boolean {
    val rgb = getRGB(x, y)
    val alpha = rgb ushr 24 and 0xff
    val r = rgb ushr 16 and 0xff
    val g = rgb ushr 8 and 0xff
    val b = rgb and 0xff
    return alpha > 32 && (r + g + b) / 3 < 245
}
