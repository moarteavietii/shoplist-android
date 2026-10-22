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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun AddItemModal(
    modal: AddItemUiState,
    onTitleChanged: (String) -> Unit,
    onViewToggled: (Long) -> Unit,
    onToggleAll: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = modal.title,
            onValueChange = onTitleChanged,
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
            Text(
                text = "Visible in",
                style = typography.label,
                color = colors.contentMuted,
            )
            Text(
                text = if (modal.allViewsSelected) "Deselect all" else "Select all",
                style = typography.label,
                color = colors.ledOn,
                modifier = Modifier.clickable(role = Role.Button) { onToggleAll() },
            )
        }

        modal.availableViews.forEach { view ->
            val selected = view.id in modal.selectedViewIds
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
            Text(
                text = "Discard",
                style = typography.label,
                color = colors.contentMuted,
                modifier = Modifier
                    .clickable(role = Role.Button) { onDiscard() }
                    .padding(8.dp),
            )
            Text(
                text = "Save",
                style = typography.label,
                color = if (modal.canSave) colors.ledOn else colors.ledOff,
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        enabled = modal.canSave,
                    ) { onSave() }
                    .padding(8.dp),
            )
        }
    }
}
