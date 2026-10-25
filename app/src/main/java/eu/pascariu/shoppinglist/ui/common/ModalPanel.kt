package eu.pascariu.shoppinglist.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes

@Composable
fun Modifier.modalPanel(shape: Shape): Modifier = this
    .shadow(elevation = 12.dp, shape = shape)
    .background(LocalShoplistColors.current.surface, shape)

@Composable
fun ModalHairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = LocalShoplistColors.current.border.copy(alpha = 0.5f),
    )
}
