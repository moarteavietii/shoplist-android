package eu.pascariu.shoppinglist.ui.quickmenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

enum class QuickMenuControl {
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

fun deriveControlLedState(
    control: QuickMenuControl,
    isSearchExpanded: Boolean,
    isShowingCheckedItems: Boolean,
    isSortModeEnabled: Boolean,
    sortOrder: SortOrder,
): SortModeLedState? =
    when (control) {
        QuickMenuControl.SORT_MODE ->
            deriveSortModeLedState(sortModeEnabled = isSortModeEnabled, order = sortOrder)
        QuickMenuControl.SEARCH ->
            if (isSearchExpanded) SortModeLedState.FILLED else SortModeLedState.HOLLOW
        QuickMenuControl.CHECKED_ITEMS_TOGGLE ->
            if (isShowingCheckedItems) SortModeLedState.FILLED else SortModeLedState.HOLLOW
        else -> null
    }

@Composable
fun CornerLedDot(filled: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalShoplistColors.current
    Box(modifier = modifier.size(10.dp), contentAlignment = Alignment.Center) {
        if (filled) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.ledOn.copy(alpha = 0.25f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(colors.ledOn, CircleShape),
                contentAlignment = Alignment.TopStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 1.dp, top = 1.dp)
                        .size(2.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .border(1.5.dp, colors.ledOff, CircleShape),
            )
        }
    }
}

@Composable
fun QuickMenu(
    isDrawerOpen: Boolean,
    isExpanded: Boolean,
    isSearchExpanded: Boolean,
    isShowingCheckedItems: Boolean,
    isSortModeEnabled: Boolean,
    sortOrder: SortOrder,
    globalUncheckedCount: Int,
    onControlTapped: (QuickMenuControl) -> Unit,
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
        var collapsedDuringGesture = false
        detectVerticalDragGestures(
            onDragStart = { collapsedDuringGesture = false },
            onVerticalDrag = { change, _ ->
                if (!collapsedDuringGesture && change.positionChange().y < -SWIPE_COLLAPSE_THRESHOLD_PX) {
                    collapsedDuringGesture = true
                    onExpandedChanged(false)
                }
            },
        )
    }

    var sortOrderSelectorOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.then(collapseOnSwipeUp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(elevation = 4.dp, shape = shapes.medium)
                .background(colors.surface, shapes.medium)
                .padding(6.dp),
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            role = Role.Button,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onExpandedChanged(!isExpanded) },
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer(rotationZ = chevronRotation)
                            .semantics {
                                contentDescription =
                                    if (isExpanded) "Collapse quick menu" else "Expand quick menu"
                            },
                    ) {
                        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        val inset = 14.dp.toPx()
                        val midY = size.height / 2
                        val path = Path().apply {
                            moveTo(inset, midY - 2.dp.toPx())
                            lineTo(size.width / 2, midY + 2.dp.toPx())
                            lineTo(size.width - inset, midY - 2.dp.toPx())
                        }
                        drawPath(path, color = colors.contentMuted, style = stroke)
                    }
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    QuickMenuControl.entries.forEach { control ->
                        val ledState = deriveControlLedState(
                            control = control,
                            isSearchExpanded = isSearchExpanded,
                            isShowingCheckedItems = isShowingCheckedItems,
                            isSortModeEnabled = isSortModeEnabled,
                            sortOrder = sortOrder,
                        )
                        if (control == QuickMenuControl.SORT_MODE) {
                            Box {
                                QuickMenuControlButton(
                                    control = control,
                                    isActive = isSortModeEnabled || sortOrder == SortOrder.ALPHABETICAL,
                                    ledState = ledState,
                                    badgeCount = null,
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
                            QuickMenuControlButton(
                                control = control,
                                isActive = when (control) {
                                    QuickMenuControl.VIEW_SWITCHER -> isDrawerOpen
                                    QuickMenuControl.SEARCH -> isSearchExpanded
                                    QuickMenuControl.CHECKED_ITEMS_TOGGLE -> isShowingCheckedItems
                                    else -> false
                                },
                                ledState = ledState,
                                badgeCount = if (control == QuickMenuControl.VIEW_SWITCHER) {
                                    globalUncheckedCount.takeIf { it > 0 }
                                } else {
                                    null
                                },
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
        style = typography.ledDot.copy(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 10.sp,
        ),
        color = colors.background,
        modifier = modifier
            .background(colors.ledOn, RoundedCornerShape(8.dp))
            .border(2.dp, colors.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .semantics { contentDescription = "$count unchecked items" },
    )
}

@Composable
private fun QuickMenuControlButton(
    control: QuickMenuControl,
    isActive: Boolean,
    ledState: SortModeLedState?,
    badgeCount: Int?,
    onTapped: () -> Unit,
    onLongPressed: (() -> Unit)?,
) {
    val colors = LocalShoplistColors.current
    val shapes = LocalShoplistShapes.current
    val noIndication = remember { MutableInteractionSource() }
    val buttonModifier = if (onLongPressed != null) {
        Modifier.combinedClickable(
            role = Role.Button,
            onClick = onTapped,
            onLongClick = onLongPressed,
            indication = null,
            interactionSource = noIndication,
        )
    } else {
        Modifier.clickable(
            role = Role.Button,
            onClick = onTapped,
            indication = null,
            interactionSource = noIndication,
        )
    }
    Box(
        modifier = buttonModifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .background(
                color = if (isActive) colors.surface else colors.chrome,
                shape = shapes.small,
            )
            .border(1.dp, colors.border.copy(alpha = 0.5f), shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = control.icon(),
            contentDescription = control.contentDescription(),
            tint = if (isActive) colors.content else colors.contentFaint,
            modifier = Modifier.size(20.dp),
        )
        when (ledState) {
            SortModeLedState.FILLED -> CornerLedDot(
                filled = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp),
            )
            SortModeLedState.HOLLOW -> CornerLedDot(
                filled = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp),
            )
            SortModeLedState.NONE, null -> Unit
        }
        if (badgeCount != null) {
            SwitcherBadge(
                count = badgeCount,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

private fun QuickMenuControl.icon(): ImageVector = when (this) {
    QuickMenuControl.VIEW_SWITCHER -> Icons.Outlined.Store
    QuickMenuControl.SETTINGS -> Icons.Outlined.Settings
    QuickMenuControl.SORT_MODE -> Icons.Outlined.SwapVert
    QuickMenuControl.CHECKED_ITEMS_TOGGLE -> Icons.Outlined.Check
    QuickMenuControl.SEARCH -> Icons.Outlined.Search
}

private fun QuickMenuControl.contentDescription(): String = when (this) {
        QuickMenuControl.VIEW_SWITCHER -> "View switcher"
        QuickMenuControl.SETTINGS -> "Settings"
        QuickMenuControl.SORT_MODE -> "Sort mode"
        QuickMenuControl.CHECKED_ITEMS_TOGGLE -> "Checked items toggle"
        QuickMenuControl.SEARCH -> "Search"
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
                .shadow(elevation = 12.dp, shape = shapes.medium)
                .background(colors.surface, shapes.medium)
                .border(1.dp, colors.border, shapes.medium)
                .padding(vertical = 6.dp),
        ) {
            SortOrder.entries.forEach { order ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onSelected(order) },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = order.icon(),
                        contentDescription = null,
                        tint = if (order == selected) colors.ledOn else colors.contentMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = order.label(),
                        style = typography.body,
                        color = if (order == selected) colors.ledOn else colors.content,
                    )
                    if (order == selected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = colors.ledOn,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun SortOrder.icon(): ImageVector = when (this) {
    SortOrder.MANUAL -> Icons.Outlined.SwapVert
    SortOrder.ALPHABETICAL -> Icons.Outlined.SortByAlpha
    SortOrder.FREQUENCY -> Icons.Outlined.LocalFireDepartment
}

private fun SortOrder.label(): String = when (this) {
    SortOrder.MANUAL -> "Manual"
    SortOrder.ALPHABETICAL -> "A→Z"
    SortOrder.FREQUENCY -> "Frequency"
}
