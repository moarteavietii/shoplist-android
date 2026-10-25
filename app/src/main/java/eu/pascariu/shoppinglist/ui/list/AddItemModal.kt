package eu.pascariu.shoppinglist.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.ui.common.LedSquareCheckbox
import eu.pascariu.shoppinglist.ui.common.ModalButton
import eu.pascariu.shoppinglist.ui.common.ModalButtonStyle
import eu.pascariu.shoppinglist.ui.common.ModalHairlineDivider
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

private const val MIN_QUANTITY = 1
private const val MAX_QUANTITY = 99

object ItemModalTestTags {
    const val visibilityCheckbox = "item_modal_visibility_checkbox"
}

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
        checkCount = null,
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
        checkCount = modal.checkCount,
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
    checkCount: Int?,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            UnderlinedNameInput(
                name = name,
                onNameChanged = onNameChanged,
                modifier = Modifier.weight(1f),
            )
            QuantityStepperWell(
                quantity = quantity,
                onQuantityChanged = onQuantityChanged,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checkCount != null) {
                ChecksCountPill(count = checkCount)
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            TemporaryChip(
                isTemporary = isTemporary,
                onToggled = onTemporaryToggled,
            )
        }

        ModalHairlineDivider()

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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LedSquareCheckbox(
                    checked = selected,
                    onCheckedChange = { onViewToggled(view.id) },
                    touchTargetSize = 36.dp,
                    testTag = ItemModalTestTags.visibilityCheckbox,
                )
                Text(text = view.name, style = typography.body, color = colors.content)
            }
        }

        ModalHairlineDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            if (onDelete != null) {
                ModalButton(
                    label = "Delete",
                    style = ModalButtonStyle.Danger,
                    onClick = onDelete,
                )
            }
            ModalButton(
                label = secondaryLabel,
                style = ModalButtonStyle.Quiet,
                onClick = onDiscard,
            )
            ModalButton(
                label = "Save",
                style = ModalButtonStyle.Primary,
                onClick = onSave,
                enabled = canSave,
            )
        }
    }
}

@Composable
private fun UnderlinedNameInput(
    name: String,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        BasicTextField(
            value = name,
            onValueChange = onNameChanged,
            singleLine = true,
            textStyle = typography.body.copy(color = colors.content),
            cursorBrush = SolidColor(colors.ledOn),
            decorationBox = { inner ->
                Column {
                    if (name.isEmpty()) {
                        Text(
                            text = "Item name",
                            style = typography.body,
                            color = colors.contentMuted,
                        )
                    }
                    inner()
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                if (isFocused) colors.ledOn else colors.contentMuted.copy(alpha = 0.5f),
                            ),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
        )
    }
}

@Composable
private fun TemporaryChip(
    isTemporary: Boolean,
    onToggled: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .toggleable(
                value = isTemporary,
                role = Role.Switch,
                onValueChange = { onToggled() },
            )
            .border(
                width = 1.dp,
                color = if (isTemporary) colors.accent else colors.border,
                shape = RoundedCornerShape(50),
            )
            .background(
                color = if (isTemporary) colors.accent.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = if (isTemporary) colors.accentDeep else colors.contentMuted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Temporary",
            style = typography.chip,
            color = if (isTemporary) colors.accentDeep else colors.contentMuted,
        )
    }
}

@Composable
private fun ChecksCountPill(count: Int) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .background(colors.chrome, RoundedCornerShape(50))
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .semantics { contentDescription = "Times checked: $count" },
    ) {
        Icon(
            imageVector = Icons.Filled.Repeat,
            contentDescription = null,
            tint = colors.contentMuted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = count.toString(),
            style = typography.chip,
            color = colors.contentMuted,
        )
    }
}

@Composable
private fun QuantityStepperWell(
    quantity: Int,
    onQuantityChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Row(
        modifier = modifier
            .background(colors.chrome, RoundedCornerShape(50))
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StepperKey(
            symbol = "−",
            enabled = quantity > MIN_QUANTITY,
            contentDescription = "Decrease quantity",
            onClick = { onQuantityChanged(quantity - 1) },
        )
        Text(
            text = quantity.toString(),
            style = typography.stepperValue,
            color = colors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 22.dp)
                .semantics { contentDescription = "Quantity: $quantity" },
        )
        StepperKey(
            symbol = "+",
            enabled = quantity < MAX_QUANTITY,
            contentDescription = "Increase quantity",
            onClick = { onQuantityChanged(quantity + 1) },
        )
    }
}

@Composable
private fun StepperKey(
    symbol: String,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Box(
        modifier = Modifier
            .size(26.dp)
            .background(colors.surface, CircleShape)
            .border(1.dp, colors.border.copy(alpha = 0.5f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                enabled = enabled,
            ) { onClick() }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = typography.stepperKey,
            color = if (enabled) colors.accentDeep else colors.ledOff,
        )
    }
}
