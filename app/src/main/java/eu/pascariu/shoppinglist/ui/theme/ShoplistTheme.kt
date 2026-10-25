package eu.pascariu.shoppinglist.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class ShoplistThemeTokens(
    val colors: ShoplistColors,
    val typography: ShoplistTypography,
    val shapes: ShoplistShapes,
)

val LocalShoplistColors = staticCompositionLocalOf<ShoplistColors> {
    error("ShoplistColors not provided")
}

val LocalShoplistTypography = staticCompositionLocalOf<ShoplistTypography> {
    error("ShoplistTypography not provided")
}

val LocalShoplistShapes = staticCompositionLocalOf<ShoplistShapes> {
    error("ShoplistShapes not provided")
}

private val ShoplistTypographyDefault = ShoplistTypography(
    display = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 62.sp,
    ),
    title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    ledDot = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    micro = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 10.sp,
    ),
    chip = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
    stepperKey = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    stepperValue = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
)

@Composable
fun ShoplistTheme(
    themeId: ThemeId = ThemeId.DEFAULT,
    content: @Composable () -> Unit,
) {
    val tokens = ShoplistThemeTokens(
        colors = ShoplistThemePalettes.getValue(themeId),
        typography = ShoplistTypographyDefault,
        shapes = ShoplistShapesDefault,
    )
    CompositionLocalProvider(
        LocalShoplistColors provides tokens.colors,
        LocalShoplistTypography provides tokens.typography,
        LocalShoplistShapes provides tokens.shapes,
    ) {
        content()
    }
}
