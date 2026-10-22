package eu.pascariu.shoppinglist.ui.list

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun ItemRow(
    item: Item,
    onCheckToggled: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onCheckToggled(item) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LedCheckbox(checked = item.isChecked)
        Text(
            text = item.name,
            style = typography.body,
            color = if (item.isChecked) colors.contentMuted else colors.content,
            modifier = Modifier.graphicsLayer(alpha = if (item.isChecked) 0.6f else 1f),
        )
        if (item.quantity > 1) {
            Text(
                text = "×${item.quantity}",
                style = typography.label,
                color = colors.contentMuted,
            )
        }
    }
}

@Composable
private fun LedCheckbox(checked: Boolean) {
    val colors = LocalShoplistColors.current
    Box(
        modifier = Modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = if (checked) colors.ledOn else colors.ledOff)
        }
    }
}
