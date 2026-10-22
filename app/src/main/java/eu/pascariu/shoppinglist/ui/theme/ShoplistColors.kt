package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ShoplistColors(
    val background: Color,
    val surface: Color,
    val content: Color,
    val contentMuted: Color,
    val ledOn: Color,
    val ledOff: Color,
    val danger: Color,
)

val ShoplistDarkColors = ShoplistColors(
    background = Color(0xFF0B0E11),
    surface = Color(0xFF14181D),
    content = Color(0xFFE8ECEF),
    contentMuted = Color(0xFF8A939B),
    ledOn = Color(0xFF3DDC84),
    ledOff = Color(0xFF2A2F35),
    danger = Color(0xFFFF5449),
)
