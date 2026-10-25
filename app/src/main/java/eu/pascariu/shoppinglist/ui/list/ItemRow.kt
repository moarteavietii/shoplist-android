package eu.pascariu.shoppinglist.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.ui.common.LedSquareCheckbox
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

object ItemRowTestTags {
    const val checkbox = "item_row_checkbox"
}

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
        exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut(),
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
                    .pointerInput(onLongPress) {
                        detectTapGestures(onLongPress = { onLongPress?.invoke() })
                    }
                    .padding(horizontal = if (dragEnabled) 0.dp else 16.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LedSquareCheckbox(
                    checked = item.isChecked,
                    onCheckedChange = { onCheckToggled(item) },
                    testTag = ItemRowTestTags.checkbox,
                )
                Text(
                    text = item.name,
                    style = typography.body,
                    color = colors.content,
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

