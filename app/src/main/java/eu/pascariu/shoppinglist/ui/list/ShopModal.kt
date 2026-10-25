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
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.pascariu.shoppinglist.ui.common.ModalButton
import eu.pascariu.shoppinglist.ui.common.ModalButtonStyle
import eu.pascariu.shoppinglist.ui.common.modalPanel
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

    BackHandler(enabled = !modal.deleteConfirmOpen) { onDiscard() }
    BackHandler(enabled = modal.deleteConfirmOpen) { onDeleteCancelled() }

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
                .modalPanel(shapes.large)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) { detectTapGestures { } }
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
                    ModalButton(
                        label = "Delete",
                        style = ModalButtonStyle.Danger,
                        enabled = !modal.saving,
                        onClick = onDeleteTapped,
                    )
                }
                ModalButton(
                    label = modal.secondaryLabel,
                    style = ModalButtonStyle.Quiet,
                    enabled = !modal.saving,
                    onClick = onDiscard,
                )
                ModalButton(
                    label = "Save",
                    style = ModalButtonStyle.Primary,
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
                    Box {
                        inner()
                        if (name.isEmpty()) {
                            Text(
                                text = "Shop name",
                                style = typography.title,
                                color = colors.contentFaint,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                        }
                    }
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
private fun DeleteShopConfirmDialog(
    shopName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
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
                    .padding(20.dp)
                    .semantics { paneTitle = "Delete shop confirmation" },
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
                    ModalButton(label = "Cancel", style = ModalButtonStyle.Quiet, onClick = onCancel)
                    ModalButton(label = "Delete", style = ModalButtonStyle.Danger, onClick = onConfirm)
                }
            }
        }
    }
}
