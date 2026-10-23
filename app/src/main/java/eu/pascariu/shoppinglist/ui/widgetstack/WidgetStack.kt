package eu.pascariu.shoppinglist.ui.widgetstack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

enum class WidgetStackControl {
    VIEW_SWITCHER,
    SETTINGS,
    SORT_MODE,
    CHECKED_ITEMS_TOGGLE,
    SEARCH,
}

@Composable
fun LedDot(filled: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalShoplistColors.current
    Box(
        modifier = modifier
            .size(6.dp)
            .background(
                color = if (filled) colors.ledOn else colors.ledOff,
                shape = CircleShape,
            ),
    )
}

@Composable
fun WidgetStack(
    isDrawerOpen: Boolean,
    isExpanded: Boolean,
    isSearchExpanded: Boolean,
    isShowingCheckedItems: Boolean,
    globalUncheckedCount: Int,
    onControlTapped: (WidgetStackControl) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val shapes = LocalShoplistShapes.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevronRotation",
    )

    val collapseOnSwipeUp = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, _ ->
                if (change.positionChange().y < -SWIPE_COLLAPSE_THRESHOLD_PX) {
                    onExpandedChanged(false)
                }
            },
        )
    }

    Box(modifier = modifier.then(collapseOnSwipeUp)) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.background(colors.surface, shapes.medium),
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(role = Role.Button) { onExpandedChanged(!isExpanded) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse widget stack" else "Expand widget stack",
                        tint = colors.contentMuted,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .graphicsLayer(rotationZ = chevronRotation),
                    )
                }
                if (globalUncheckedCount > 0) {
                    SwitcherBadge(
                        count = globalUncheckedCount,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    WidgetStackControl.entries.forEach { control ->
                        WidgetStackControlRow(
                            control = control,
                            isActive = when (control) {
                                WidgetStackControl.VIEW_SWITCHER -> isDrawerOpen
                                WidgetStackControl.SEARCH -> isSearchExpanded
                                WidgetStackControl.CHECKED_ITEMS_TOGGLE -> isShowingCheckedItems
                                else -> false
                            },
                            onTapped = { onControlTapped(control) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitcherBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    Text(
        text = count.toString(),
        style = typography.label,
        color = colors.background,
        modifier = modifier
            .background(colors.ledOn, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clearAndSetSemantics { },
    )
}

@Composable
private fun WidgetStackControlRow(
    control: WidgetStackControl,
    isActive: Boolean,
    onTapped: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onTapped)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = control.icon(),
            contentDescription = control.contentDescription(),
            tint = if (isActive) colors.ledOn else colors.contentMuted,
            modifier = Modifier.minimumInteractiveComponentSize(),
        )
        if (control.showsLedDot) {
            LedDot(filled = isActive)
        }
    }
}

private val WidgetStackControl.showsLedDot: Boolean
    get() = this != WidgetStackControl.VIEW_SWITCHER && this != WidgetStackControl.SETTINGS

private fun WidgetStackControl.icon(): ImageVector = when (this) {
    WidgetStackControl.VIEW_SWITCHER -> Icons.Filled.ViewWeek
    WidgetStackControl.SETTINGS -> Icons.Filled.Settings
    WidgetStackControl.SORT_MODE -> Icons.Filled.Sort
    WidgetStackControl.CHECKED_ITEMS_TOGGLE -> Icons.Filled.Visibility
    WidgetStackControl.SEARCH -> Icons.Filled.Search
}

private fun WidgetStackControl.contentDescription(): String = when (this) {
        WidgetStackControl.VIEW_SWITCHER -> "View switcher"
        WidgetStackControl.SETTINGS -> "Settings"
        WidgetStackControl.SORT_MODE -> "Sort mode"
        WidgetStackControl.CHECKED_ITEMS_TOGGLE -> "Checked items toggle"
        WidgetStackControl.SEARCH -> "Search"
    }

private const val SWIPE_COLLAPSE_THRESHOLD_PX = 24f
