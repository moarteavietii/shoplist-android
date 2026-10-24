package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {

    private fun luminance(color: Long): Double {
        fun channel(c: Long): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        val r = channel((color shr 16) and 0xFF)
        val g = channel((color shr 8) and 0xFF)
        val b = channel(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(foreground: Long, background: Long): Double {
        val l1 = luminance(foreground)
        val l2 = luminance(background)
        val (hi, lo) = if (l1 >= l2) l1 to l2 else l2 to l1
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun argbToRgb(color: Color): Long = (color.value.toLong() shr 32) and 0xFFFFFF

    private fun assertContrast(foreground: Color, background: Color, minimum: Double, role: String) {
        val ratio = contrastRatio(argbToRgb(foreground), argbToRgb(background))
        assertTrue(
            "$role contrast $ratio is below $minimum (fg=${foreground.value}, bg=${background.value})",
            ratio >= minimum,
        )
    }

    @Test
    fun `catalog contains exactly the nine curated themes`() {
        val expected = listOf(
            ThemeId.DEFAULT,
            ThemeId.PASTEL,
            ThemeId.MINT,
            ThemeId.LAVENDER,
            ThemeId.DARK_SLATE,
            ThemeId.OCEAN,
            ThemeId.SUNSET,
            ThemeId.HIGH_CONTRAST,
            ThemeId.CHRISTMAS,
        )
        org.junit.Assert.assertEquals(expected, ThemeId.entries)
    }

    @Test
    fun `all themes meet WCAG AA contrast`() {
        ThemeId.entries.forEach { themeId ->
            val palette = ShoplistThemePalettes[themeId]
                ?: throw AssertionError("Missing palette for theme $themeId")

            assertContrast(palette.content, palette.surface, 4.5, "${themeId.name} content/surface")
            assertContrast(palette.content, palette.background, 4.5, "${themeId.name} content/background")
            assertContrast(palette.accent, palette.surface, 3.0, "${themeId.name} accent/surface")
            assertContrast(palette.danger, palette.surface, 3.0, "${themeId.name} danger/surface")
            assertContrast(palette.ledOn, palette.background, 3.0, "${themeId.name} ledOn/background (checkmark)")
        }
    }
}
