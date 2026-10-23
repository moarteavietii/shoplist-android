package eu.pascariu.shoppinglist.ui.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun ShopModal(
    modal: ShopModalUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDeleteTapped: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    BackHandler { onDiscard() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onDiscard() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) { detectTapGestures { } }
                .background(colors.surface, shapes.large)
                .padding(24.dp),
        ) {
            UnderlinedNameInput(
                name = modal.name,
                onNameChanged = onNameChanged,
                enabled = !modal.saving,
            )

            modal.error?.let { error ->
                Text(
                    text = error,
                    style = typography.label,
                    color = colors.danger,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                if (modal.mode == ShopModalMode.EDIT) {
                    ModalAction(
                        label = "Delete",
                        color = colors.danger,
                        enabled = !modal.saving,
                        onClick = onDeleteTapped,
                    )
                }
                ModalAction(
                    label = modal.secondaryLabel,
                    color = colors.contentMuted,
                    enabled = !modal.saving,
                    onClick = onDiscard,
                )
                ModalAction(
                    label = "Save",
                    color = colors.ledOn,
                    enabled = modal.canSave,
                    onClick = onSave,
                )
            }
        }
    }

    if (modal.deleteConfirmOpen) {
        DeleteShopConfirmDialog(
            shopName = modal.shopName,
            onConfirm = onDeleteConfirmed,
            onCancel = onDeleteCancelled,
        )
    }
}

@Composable
private fun UnderlinedNameInput(
    name: String,
    onNameChanged: (String) -> Unit,
    enabled: Boolean,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Column {
        BasicTextField(
            value = name,
            onValueChange = onNameChanged,
            enabled = enabled,
            singleLine = true,
            textStyle = typography.title.copy(color = colors.content),
            cursorBrush = SolidColor(colors.ledOn),
            decorationBox = { inner ->
                Column {
                    if (name.isEmpty()) {
                        Text(
                            text = "Shop name",
                            style = typography.title,
                            color = colors.contentMuted,
                        )
                    }
                    inner()
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.contentMuted.copy(alpha = 0.5f)),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ModalAction(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val typography = LocalShoplistTypography.current
    Text(
        text = label,
        style = typography.label,
        color = if (enabled) color else color.copy(alpha = 0.4f),
        modifier = Modifier
            .clickable(enabled = enabled, role = Role.Button) { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun DeleteShopConfirmDialog(
    shopName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onCancel() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 320.dp)
                .pointerInput(Unit) { detectTapGestures { } }
                .background(colors.surface, shapes.medium)
                .padding(20.dp),
        ) {
            Text(
                text = "Delete \"$shopName\"?",
                style = typography.title,
                color = colors.content,
            )
            Text(
                text = "This cannot be undone.",
                style = typography.body,
                color = colors.contentMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ModalAction(label = "Cancel", color = colors.contentMuted, enabled = true, onClick = onCancel)
                ModalAction(label = "Delete", color = colors.danger, enabled = true, onClick = onConfirm)
            }
        }
    }
}
