package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

@Immutable
data class ShoplistTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val ledDot: TextStyle,
    val micro: TextStyle,
    val chip: TextStyle,
    val stepperKey: TextStyle,
    val stepperValue: TextStyle,
)
