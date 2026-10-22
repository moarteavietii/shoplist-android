package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable

@Immutable
data class ShoplistTypography(
    val display: androidx.compose.ui.text.TextStyle,
    val title: androidx.compose.ui.text.TextStyle,
    val body: androidx.compose.ui.text.TextStyle,
    val label: androidx.compose.ui.text.TextStyle,
    val ledDot: androidx.compose.ui.text.TextStyle,
)

fun ShoplistTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = display,
    titleLarge = title,
    bodyLarge = body,
    labelLarge = label,
)
