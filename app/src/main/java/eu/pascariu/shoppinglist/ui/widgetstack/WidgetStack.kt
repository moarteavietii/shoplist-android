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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import eu.pascariu.shoppinglist.data.prefs.SortOrder
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

enum class SortModeLedState { FILLED, HOLLOW, NONE }

fun deriveSortModeLedState(sortModeEnabled: Boolean, order: SortOrder): SortModeLedState =
    when (order) {
        SortOrder.MANUAL ->
            if (sortModeEnabled) SortModeLedState.FILLED else SortModeLedState.HOLLOW
        SortOrder.ALPHABETICAL,
        SortOrder.FREQUENCY,
        -> SortModeLedState.NONE
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
    isSortModeEnabled: Boolean,
    sortOrder: SortOrder,
    globalUncheckedCount: Int,
    onControlTapped: (WidgetStackControl) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit,
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

    var sortOrderSelectorOpen by remember { mutableStateOf(false) }

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
                        val ledState = if (control == WidgetStackControl.SORT_MODE) {
                            deriveSortModeLedState(sortModeEnabled = isSortModeEnabled, order = sortOrder)
                        } else {
                            null
                        }
                        if (control == WidgetStackControl.SORT_MODE) {
                            Box {
                                WidgetStackControlRow(
                                    control = control,
                                    isActive = isSortModeEnabled || sortOrder == SortOrder.ALPHABETICAL,
                                    ledState = ledState,
                                    onTapped = { onControlTapped(control) },
                                    onLongPressed = { sortOrderSelectorOpen = true },
                                )
                                SortOrderSelector(
                                    expanded = sortOrderSelectorOpen,
                                    selected = sortOrder,
                                    onSelected = { order ->
                                        sortOrderSelectorOpen = false
                                        onSortOrderSelected(order)
                                    },
                                    onDismiss = { sortOrderSelectorOpen = false },
                                )
                            }
                        } else {
                            WidgetStackControlRow(
                                control = control,
                                isActive = when (control) {
                                    WidgetStackControl.VIEW_SWITCHER -> isDrawerOpen
                                    WidgetStackControl.SEARCH -> isSearchExpanded
                                    WidgetStackControl.CHECKED_ITEMS_TOGGLE -> isShowingCheckedItems
                                    else -> false
                                },
                                ledState = ledState,
                                onTapped = { onControlTapped(control) },
                                onLongPressed = null,
                            )
                        }
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
    ledState: SortModeLedState?,
    onTapped: () -> Unit,
    onLongPressed: (() -> Unit)?,
) {
    val colors = LocalShoplistColors.current
    val rowModifier = if (onLongPressed != null) {
        Modifier.combinedClickable(
            role = Role.Button,
            onClick = onTapped,
            onLongClick = onLongPressed,
        )
    } else {
        Modifier.clickable(role = Role.Button, onClick = onTapped)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = control.icon(),
            contentDescription = control.contentDescription(),
            tint = if (isActive) colors.ledOn else colors.contentMuted,
            modifier = Modifier.minimumInteractiveComponentSize(),
        )
        when (ledState) {
            SortModeLedState.FILLED -> LedDot(filled = true)
            SortModeLedState.HOLLOW -> LedDot(filled = false)
            SortModeLedState.NONE, null -> Unit
        }
    }
}

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

@Composable
private fun SortOrderSelector(
    expanded: Boolean,
    selected: SortOrder,
    onSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val shapes = LocalShoplistShapes.current
    val typography = LocalShoplistTypography.current
    val rowWidthPx = with(LocalDensity.current) { 48.dp.toPx() }.roundToInt()
    val verticalGapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    if (!expanded) return
    Popup(
        onDismissRequest = onDismiss,
        alignment = Alignment.TopEnd,
        offset = IntOffset(-rowWidthPx, verticalGapPx),
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .background(colors.surface, shapes.medium)
                .shadow(elevation = 12.dp, shape = shapes.medium)
                .padding(vertical = 6.dp),
        ) {
            SortOrder.entries.forEach { order ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClick = { onSelected(order) },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LedDot(filled = order == selected)
                    Text(
                        text = order.label(),
                        style = typography.body,
                        color = if (order == selected) colors.ledOn else colors.content,
                    )
                }
            }
        }
    }
}

private fun SortOrder.label(): String = when (this) {
    SortOrder.MANUAL -> "Manual"
    SortOrder.ALPHABETICAL -> "A→Z"
    SortOrder.FREQUENCY -> "Frequency"
}
