package eu.pascariu.shoppinglist.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

data class DrawerShop(
    val id: Long,
    val name: String,
    val isGlobal: Boolean,
    val isActive: Boolean,
    val uncheckedCount: Int,
)

@Composable
fun ViewDrawer(
    visible: Boolean,
    shops: List<DrawerShop>,
    onShopSelected: (Long) -> Unit,
    onShopEdit: (Long) -> Unit,
    onAddShop: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    DrawerShell(visible = visible, onDismiss = onDismiss, modifier = modifier) {
        Text(
            text = "Views",
            style = typography.label,
            color = colors.contentMuted,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            shops.forEach { shop ->
                DrawerShopRow(
                    shop = shop,
                    onSelected = { onShopSelected(shop.id) },
                    onEdit = { onShopEdit(shop.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Add shop",
            style = typography.label,
            color = colors.ledOn,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onAddShop() }
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun DrawerShopRow(
    shop: DrawerShop,
    onSelected: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onSelected() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = shop.name,
            style = typography.body,
            color = if (shop.isActive) colors.ledOn else colors.content,
            modifier = Modifier.weight(1f),
        )

        UncheckedCountBadge(count = shop.uncheckedCount)

        if (!shop.isGlobal) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit ${shop.name}",
                tint = colors.contentMuted,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button) { onEdit() },
            )
        }
    }
}

@Composable
private fun UncheckedCountBadge(count: Int) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    Text(
        text = count.toString(),
        style = typography.label,
        color = if (count > 0) colors.background else colors.contentMuted,
        modifier = Modifier
            .then(
                if (count > 0) {
                    Modifier.background(colors.ledOn, shapes.small)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = "$count unchecked items" },
    )
}
