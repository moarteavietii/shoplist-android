package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoplistPalettesTest {

    private fun hex(color: Color): String {
        val rgb = (color.value.toLong() shr 32) and 0xFFFFFF
        return "#%06X".format(rgb)
    }

    private fun assertPinned(
        themeId: ThemeId,
        background: String,
        surface: String,
        content: String,
        accent: String,
        danger: String,
    ) {
        val palette = ShoplistThemePalettes[themeId]
            ?: throw AssertionError("Missing palette for $themeId")
        assertEquals("$themeId background", background, hex(palette.background))
        assertEquals("$themeId surface", surface, hex(palette.surface))
        assertEquals("$themeId content", content, hex(palette.content))
        assertEquals("$themeId accent", accent, hex(palette.accent))
        assertEquals("$themeId danger", danger, hex(palette.danger))
    }

    @Test
    fun `every theme has a palette`() {
        ThemeId.entries.forEach { themeId ->
            assertEquals(
                "Missing palette for $themeId",
                themeId,
                ShoplistThemePalettes[themeId]?.let { themeId },
            )
        }
    }

    @Test
    fun `every theme derives inverse tokens`() {
        ThemeId.entries.forEach { themeId ->
            val palette = ShoplistThemePalettes[themeId]
                ?: throw AssertionError("Missing palette for $themeId")
            assertEquals(
                "$themeId surfaceInverse should invert content",
                palette.content,
                palette.surfaceInverse,
            )
            assertEquals(
                "$themeId contentInverse should invert background",
                palette.background,
                palette.contentInverse,
            )
        }
    }

    @Test
    fun `default palette matches web root tokens`() {
        assertPinned(
            ThemeId.DEFAULT,
            background = "#FFFFFF",
            surface = "#FFFFFF",
            content = "#333333",
            accent = "#007BFF",
            danger = "#DC3545",
        )
    }

    @Test
    fun `pastel palette matches web data-theme block`() {
        assertPinned(
            ThemeId.PASTEL,
            background = "#FDF3E7",
            surface = "#FFFAF2",
            content = "#4A3B2E",
            accent = "#9C5634",
            danger = "#AA4B40",
        )
    }

    @Test
    fun `mint palette matches web data-theme block`() {
        assertPinned(
            ThemeId.MINT,
            background = "#EEF7F1",
            surface = "#F7FCF8",
            content = "#24382C",
            accent = "#187751",
            danger = "#BE3636",
        )
    }

    @Test
    fun `lavender palette matches web data-theme block`() {
        assertPinned(
            ThemeId.LAVENDER,
            background = "#F3F0FA",
            surface = "#FAF8FE",
            content = "#372D4E",
            accent = "#6C48D1",
            danger = "#AA3D53",
        )
    }

    @Test
    fun `dark slate palette matches web data-theme block`() {
        assertPinned(
            ThemeId.DARK_SLATE,
            background = "#1A1D23",
            surface = "#23272F",
            content = "#E8EAED",
            accent = "#5B9CF5",
            danger = "#E87882",
        )
    }

    @Test
    fun `ocean palette matches web data-theme block`() {
        assertPinned(
            ThemeId.OCEAN,
            background = "#0F2027",
            surface = "#16303A",
            content = "#E0F2F1",
            accent = "#2EC4B6",
            danger = "#EB878F",
        )
    }

    @Test
    fun `sunset palette matches web data-theme block`() {
        assertPinned(
            ThemeId.SUNSET,
            background = "#241A12",
            surface = "#2F2318",
            content = "#F5EAD9",
            accent = "#F59E42",
            danger = "#E8747D",
        )
    }

    @Test
    fun `high contrast palette matches web data-theme block`() {
        assertPinned(
            ThemeId.HIGH_CONTRAST,
            background = "#000000",
            surface = "#111111",
            content = "#FFFFFF",
            accent = "#FFD60A",
            danger = "#FF6B6B",
        )
    }

    @Test
    fun `christmas palette is light with red accent and red led`() {
        assertPinned(
            ThemeId.CHRISTMAS,
            background = "#FFFBFB",
            surface = "#FFFFFF",
            content = "#2F2F2D",
            accent = "#E02020",
            danger = "#922B21",
        )
        val palette = ShoplistThemePalettes.getValue(ThemeId.CHRISTMAS)
        assertEquals("#E02020", hex(palette.ledOn))
    }
}
