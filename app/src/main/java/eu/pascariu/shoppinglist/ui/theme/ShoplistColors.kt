package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ShoplistColors(
    val background: Color,
    val surface: Color,
    val content: Color,
    val contentMuted: Color,
    val contentFaint: Color,
    val accent: Color,
    val accentDeep: Color,
    val border: Color,
    val chrome: Color,
    val ledOn: Color,
    val ledOff: Color,
    val danger: Color,
    val surfaceInverse: Color = content,
    val contentInverse: Color = background,
)
