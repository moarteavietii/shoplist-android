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
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 40.sp,
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
