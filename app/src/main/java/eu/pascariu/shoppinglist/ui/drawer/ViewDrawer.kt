package eu.pascariu.shoppinglist.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
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
            text = "Shops",
            style = typography.title,
            color = colors.content,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shops.forEach { shop ->
                DrawerShopRow(
                    shop = shop,
                    onSelected = { onShopSelected(shop.id) },
                    onEdit = { onShopEdit(shop.id) },
                )
            }
        }

        AddShopButton(onClick = onAddShop)
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
    val cardShape = LocalShoplistShapes.current.medium

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, if (shop.isActive) colors.accent else colors.border, cardShape)
                .background(if (shop.isActive) colors.accent else colors.surface, cardShape)
                .clickable(role = Role.Button) { onSelected() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = shop.name,
                style = typography.body,
                color = if (shop.isActive) colors.background else colors.content,
                modifier = Modifier.weight(1f),
            )

            if (shop.uncheckedCount > 0) {
                UncheckedCountBadge(count = shop.uncheckedCount, isActive = shop.isActive)
            } else if (shop.uncheckedCount == 0) {
                Text(
                    text = "0",
                    style = typography.micro,
                    color = colors.contentFaint,
                )
            }
        }

        if (!shop.isGlobal) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, colors.border, cardShape)
                    .clickable(role = Role.Button) { onEdit() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${shop.name}",
                    tint = colors.content,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UncheckedCountBadge(count: Int, isActive: Boolean) {
    val colors = LocalShoplistColors.current
    val badgeShape = LocalShoplistShapes.current.medium
    val badgeStyle = LocalShoplistTypography.current.micro.copy(lineHeight = 14.sp)

    Text(
        text = count.toString(),
        style = badgeStyle,
        color = if (isActive) colors.accentDeep else colors.background,
        modifier = Modifier
            .background(if (isActive) colors.background else colors.accentDeep, badgeShape)
            .padding(horizontal = 4.dp)
            .semantics { contentDescription = "$count unchecked items" },
    )
}

@Composable
private fun AddShopButton(onClick: () -> Unit) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val buttonShape = LocalShoplistShapes.current.medium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(48.dp)
            .border(1.dp, colors.border, buttonShape)
            .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "+", style = typography.title, color = colors.content)
    }
}
