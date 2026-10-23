package eu.pascariu.shoppinglist.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun ItemRow(
    item: Item,
    onCheckToggled: (Item) -> Unit,
    modifier: Modifier = Modifier,
    hideChecked: Boolean = false,
    dragEnabled: Boolean = false,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val dragShadowPx = with(LocalDensity.current) { 16.dp.toPx() }
    val dragShape = RoundedCornerShape(12.dp)

    AnimatedVisibility(
        visible = !(hideChecked && item.isChecked),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (isDragging) {
                        shadowElevation = dragShadowPx
                        shape = dragShape
                        scaleX = 1.02f
                        scaleY = 1.02f
                    } else {
                        shadowElevation = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                }
                .background(if (isDragging) colors.surface else Color.Transparent, dragShape)
                .zIndex(if (isDragging) 1f else 0f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (dragEnabled) {
                DragHandle(modifier = dragHandleModifier)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        toggleableState =
                            if (item.isChecked) ToggleableState.On else ToggleableState.Off
                    }
                    .combinedClickable(
                        onClick = { onCheckToggled(item) },
                        onLongClick = onLongPress,
                        role = Role.Checkbox,
                    )
                    .padding(horizontal = if (dragEnabled) 0.dp else 16.dp, vertical = 10.dp),
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
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    val colors = LocalShoplistColors.current
    Box(
        modifier = modifier
            .size(width = 28.dp, height = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(3) {
                Canvas(
                    modifier = Modifier
                        .padding(vertical = 1.dp)
                        .size(width = 12.dp, height = 2.dp),
                ) {
                    drawRoundRect(
                        color = colors.contentMuted,
                        cornerRadius = CornerRadius(1.dp.toPx()),
                    )
                }
            }
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
