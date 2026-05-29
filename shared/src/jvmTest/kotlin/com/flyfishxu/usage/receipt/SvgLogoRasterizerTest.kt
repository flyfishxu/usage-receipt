package com.flyfishxu.usage.receipt

import com.flyfishxu.usage.model.Provider
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvgLogoRasterizerTest {
    @Test
    fun rasterizesCompleteProviderLogosWithTightBounds() {
        val rasterizer = SvgLogoRasterizer()

        Provider.entries.forEach { provider ->
            val image = rasterizer.rasterize(provider, targetWidth = 220, targetHeight = 70)
            assertNotNull(image, "Expected logo image for $provider")
            assertTrue(image.width <= 220, "Logo should fit requested width")
            assertTrue(image.height <= 70, "Logo should fit requested height")
            assertTrue(image.hasInk(), "Logo should contain printable pixels")
        }
    }
}

private fun java.awt.image.BufferedImage.hasInk(): Boolean {
    for (y in 0 until height) {
        for (x in 0 until width) {
            val rgb = getRGB(x, y)
            val alpha = rgb ushr 24 and 0xff
            val r = rgb ushr 16 and 0xff
            val g = rgb ushr 8 and 0xff
            val b = rgb and 0xff
            if (alpha > 32 && (r + g + b) / 3 < 245) return true
        }
    }
    return false
}
