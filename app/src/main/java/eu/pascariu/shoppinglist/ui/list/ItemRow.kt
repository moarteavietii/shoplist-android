package eu.pascariu.shoppinglist.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.pascariu.shoppinglist.data.model.Item
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
                    .pointerInput(onLongPress) {
                        detectTapGestures(onLongPress = { onLongPress?.invoke() })
                    }
                    .padding(horizontal = if (dragEnabled) 0.dp else 16.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircleCheckbox(
                    checked = item.isChecked,
                    onCheckedChange = { onCheckToggled(item) },
                )
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
private fun CircleCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "checkmarkProgress",
    )
    val circleColor by animateColorAsState(
        targetValue = if (checked) colors.ledOn else colors.ledOff,
        animationSpec = tween(200),
        label = "circleColor",
    )
    val checkColor = colors.background

    val checkPath = remember { Path() }
    val checkMeasure = remember { PathMeasure() }
    val partialPath = remember { Path() }

    Box(
        modifier = modifier
            .size(48.dp)
            .testTag(ItemRowTestTags.checkbox)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onCheckedChange() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            drawCircle(color = circleColor)
            if (checkProgress > 0f) {
                checkPath.reset()
                checkPath.moveTo(size.width * 0.25f, size.height * 0.52f)
                checkPath.lineTo(size.width * 0.42f, size.height * 0.70f)
                checkPath.lineTo(size.width * 0.78f, size.height * 0.32f)
                checkMeasure.setPath(checkPath, false)
                partialPath.reset()
                checkMeasure.getSegment(0f, checkMeasure.length * checkProgress, partialPath, true)
                drawPath(
                    path = partialPath,
                    color = checkColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}
