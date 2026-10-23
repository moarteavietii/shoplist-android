package eu.pascariu.shoppinglist.ui.list

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

private const val MIN_QUANTITY = 1
private const val MAX_QUANTITY = 99

@Composable
fun AddItemModal(
    modal: AddItemUiState,
    onTitleChanged: (String) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    onTemporaryToggled: () -> Unit,
    onViewToggled: (Long) -> Unit,
    onToggleAll: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ItemModalContent(
        name = modal.title,
        availableViews = modal.availableViews,
        selectedViewIds = modal.selectedViewIds,
        allViewsSelected = modal.allViewsSelected,
        quantity = modal.quantity,
        isTemporary = modal.isTemporary,
        onNameChanged = onTitleChanged,
        onQuantityChanged = onQuantityChanged,
        onTemporaryToggled = onTemporaryToggled,
        onViewToggled = onViewToggled,
        onToggleAll = onToggleAll,
        onDiscard = onDiscard,
        canSave = modal.canSave,
        onDelete = null,
        onSave = onSave,
        secondaryLabel = modal.secondaryLabel,
        modifier = modifier,
    )
}

@Composable
fun EditItemModal(
    modal: EditItemUiState,
    onNameChanged: (String) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    onTemporaryToggled: () -> Unit,
    onViewToggled: (Long) -> Unit,
    onToggleAll: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ItemModalContent(
        name = modal.name,
        availableViews = modal.availableViews,
        selectedViewIds = modal.selectedViewIds,
        allViewsSelected = modal.allViewsSelected,
        quantity = modal.quantity,
        isTemporary = modal.isTemporary,
        onNameChanged = onNameChanged,
        onQuantityChanged = onQuantityChanged,
        onTemporaryToggled = onTemporaryToggled,
        onViewToggled = onViewToggled,
        onToggleAll = onToggleAll,
        onDiscard = onDiscard,
        canSave = modal.canSave,
        onDelete = onDelete,
        onSave = onSave,
        secondaryLabel = modal.secondaryLabel,
        modifier = modifier,
    )
}

@Composable
private fun ItemModalContent(
    name: String,
    availableViews: List<Shop>,
    selectedViewIds: Set<Long>,
    allViewsSelected: Boolean,
    quantity: Int,
    isTemporary: Boolean,
    onNameChanged: (String) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    onTemporaryToggled: () -> Unit,
    onViewToggled: (Long) -> Unit,
    onToggleAll: () -> Unit,
    onDiscard: () -> Unit,
    canSave: Boolean,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
    secondaryLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = name,
            onValueChange = onNameChanged,
            placeholder = {
                Text(text = "Item name", style = typography.body, color = colors.contentMuted)
            },
            singleLine = true,
            shape = shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedTextColor = colors.content,
                unfocusedTextColor = colors.content,
                cursorColor = colors.ledOn,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "−",
                    style = typography.body,
                    color = if (quantity > MIN_QUANTITY) colors.ledOn else colors.ledOff,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Button,
                            enabled = quantity > MIN_QUANTITY,
                        ) { onQuantityChanged(quantity - 1) }
                        .semantics { contentDescription = "Decrease quantity" },
                )
                Text(
                    text = quantity.toString(),
                    style = typography.body,
                    color = colors.content,
                    modifier = Modifier.semantics {
                        contentDescription = "Quantity: $quantity"
                    },
                )
                Text(
                    text = "+",
                    style = typography.body,
                    color = if (quantity < MAX_QUANTITY) colors.ledOn else colors.ledOff,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Button,
                            enabled = quantity < MAX_QUANTITY,
                        ) { onQuantityChanged(quantity + 1) }
                        .semantics { contentDescription = "Increase quantity" },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.toggleable(
                    value = isTemporary,
                    role = Role.Switch,
                    onValueChange = { onTemporaryToggled() },
                ),
            ) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = if (isTemporary) colors.ledOn else colors.ledOff)
                    }
                }
                Text(text = "Temporary", style = typography.label, color = colors.content)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Visible in",
                style = typography.label,
                color = colors.contentMuted,
            )
            Text(
                text = if (allViewsSelected) "Deselect all" else "Select all",
                style = typography.label,
                color = colors.ledOn,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button) { onToggleAll() },
            )
        }

        availableViews.forEach { view ->
            val selected = view.id in selectedViewIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = selected,
                        role = Role.Checkbox,
                        onValueChange = { onViewToggled(view.id) },
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = if (selected) colors.ledOn else colors.ledOff)
                    }
                }
                Text(text = view.name, style = typography.body, color = colors.content)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            if (onDelete != null) {
                Text(
                    text = "Delete",
                    style = typography.label,
                    color = colors.danger,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onDelete() },
                )
            }
            Text(
                text = secondaryLabel,
                style = typography.label,
                color = colors.contentMuted,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button) { onDiscard() },
            )
            Text(
                text = "Save",
                style = typography.label,
                color = if (canSave) colors.ledOn else colors.ledOff,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(
                        role = Role.Button,
                        enabled = canSave,
                    ) { onSave() },
            )
        }
    }
}
