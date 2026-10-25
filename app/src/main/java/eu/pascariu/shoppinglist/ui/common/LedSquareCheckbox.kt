package eu.pascariu.shoppinglist.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors

@Composable
fun LedSquareCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    touchTargetSize: Dp = 48.dp,
    testTag: String? = null,
) {
    val colors = LocalShoplistColors.current

    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "ledSquareCheckProgress",
    )
    val fillColor by animateColorAsState(
        targetValue = if (checked) colors.ledOn else Color.Transparent,
        animationSpec = tween(200),
        label = "ledSquareFillColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colors.ledOn else colors.border,
        animationSpec = tween(200),
        label = "ledSquareBorderColor",
    )
    val checkColor = colors.background

    val checkPath = remember { Path() }
    val checkMeasure = remember { PathMeasure() }
    val partialPath = remember { Path() }

    var boxModifier = modifier
        .size(touchTargetSize)
        .toggleable(
            value = checked,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Checkbox,
            onValueChange = { onCheckedChange() },
        )
    if (testTag != null) {
        boxModifier = boxModifier.testTag(testTag)
    }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val cornerRadius = CornerRadius(4.dp.toPx())
            if (fillColor != Color.Transparent) {
                drawRoundRect(color = fillColor, cornerRadius = cornerRadius)
            }
            drawRoundRect(
                color = borderColor,
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.5.dp.toPx()),
            )
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
