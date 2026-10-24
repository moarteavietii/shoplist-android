package eu.pascariu.shoppinglist.ui.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.ui.drawer.ViewDrawer
import eu.pascariu.shoppinglist.ui.drawer.leftEdgeSwipeToOpen
import eu.pascariu.shoppinglist.ui.settings.SettingsDrawer
import eu.pascariu.shoppinglist.ui.settings.SettingsViewModel
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography
import eu.pascariu.shoppinglist.ui.quickmenu.QuickMenu
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ListScreen(
    viewModel: ListViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val changeToast by viewModel.changeToast.collectAsStateWithLifecycle()
    val settingsTheme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val settingsChangeToasts by settingsViewModel.changeToastsEnabled.collectAsStateWithLifecycle()
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var lastAddItemModal by remember { mutableStateOf<AddItemUiState?>(null) }
    if (state.addItemModal != null) {
        lastAddItemModal = state.addItemModal
    }
    var lastEditItemModal by remember { mutableStateOf<EditItemUiState?>(null) }
    if (state.editItemModal != null) {
        lastEditItemModal = state.editItemModal
    }
    var lastShopModal by remember { mutableStateOf<ShopModalUiState?>(null) }
    if (state.shopModal != null) {
        lastShopModal = state.shopModal
    }
    var draggingItemId by remember { mutableStateOf<Long?>(null) }
    var dragOrder by remember { mutableStateOf<List<Item>>(emptyList()) }
    val dragEnabled = state.dragEnabled

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.postSaveSignal) {
        if (state.postSaveSignal != null) {
            if (state.filteredItems.isNotEmpty()) {
                listState.scrollToItem(state.filteredItems.lastIndex)
            }
            searchFocusRequester.requestFocus()
            keyboard?.show()
            viewModel.consumePostSaveSignal()
        }
    }

    LaunchedEffect(state.searchExpanded) {
        if (state.searchExpanded) {
            searchFocusRequester.requestFocus()
            keyboard?.show()
        }
    }

    BackHandler(enabled = state.addItemModal != null) {
        viewModel.onAddItemDiscard()
    }

    BackHandler(enabled = state.editItemModal != null) {
        viewModel.onEditItemDiscard()
    }

    BackHandler(enabled = state.deleteConfirmItem != null) {
        viewModel.onDeleteCancelled()
    }

    BackHandler(enabled = state.shopModal != null) {
        viewModel.onShopDiscard()
    }

    BackHandler(
        enabled = state.searchExpanded &&
            state.addItemModal == null &&
            state.editItemModal == null &&
            state.deleteConfirmItem == null &&
            state.shopModal == null,
    ) {
        viewModel.onSearchExpandedChanged(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = state.watermarkName,
                    style = typography.display,
                    color = colors.content,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer(alpha = 0.07f),
                )

                val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                    val mutable = dragOrder.toMutableList()
                    val moved = mutable.removeAt(from.index)
                    mutable.add(to.index, moved)
                    dragOrder = mutable
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxSize()
                        .leftEdgeSwipeToOpen(
                            isEnabled = !state.drawerOpen,
                            onOpen = viewModel::onDrawerToggled,
                        ),
                ) {
                    val visibleItems = if (draggingItemId != null) dragOrder else state.filteredItems
                    items(visibleItems, key = { it.id }) { item ->
                        ReorderableItem(
                            state = reorderableState,
                            key = item.id,
                            enabled = dragEnabled,
                        ) { isDragging ->
                            val rowIsDragging = isDragging && draggingItemId == item.id
                            ItemRow(
                                item = item,
                                onCheckToggled = viewModel::onCheckToggled,
                                hideChecked = state.hideChecked,
                                dragEnabled = dragEnabled,
                                isDragging = rowIsDragging,
                                onLongPress = { viewModel.onItemLongPressed(item) },
                                dragHandleModifier = Modifier.draggableHandle(
                                    onDragStarted = {
                                        draggingItemId = item.id
                                        dragOrder = state.items
                                    },
                                    onDragStopped = {
                                        val dropIndex = dragOrder.indexOfFirst { it.id == item.id }
                                        if (dropIndex >= 0) {
                                            viewModel.onItemDropped(item, dropIndex)
                                        }
                                        draggingItemId = null
                                        dragOrder = emptyList()
                                    },
                                ),
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

                QuickMenu(
                    isDrawerOpen = state.drawerOpen,
                    isExpanded = state.isQuickMenuExpanded,
                    isSearchExpanded = state.searchExpanded,
                    isShowingCheckedItems = state.showChecked,
                    isSortModeEnabled = state.sortModeEnabled,
                    sortOrder = state.sortOrder,
                    globalUncheckedCount = state.drawerShops.firstOrNull { it.isGlobal }?.uncheckedCount ?: 0,
                    onControlTapped = viewModel::onQuickMenuControlTapped,
                    onSortOrderSelected = viewModel::onSortOrderChanged,
                    onExpandedChanged = viewModel::onQuickMenuExpandedChanged,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

            AnimatedVisibility(
                visible = state.showAddAffordance,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                Text(
                    text = "Add \"${state.searchQuery.trim()}\"",
                    style = typography.label,
                    color = colors.ledOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { viewModel.onAddAffordanceTapped() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            AnimatedVisibility(
                visible = state.showUndoBar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                Text(
                    text = "Undo",
                    style = typography.label,
                    color = colors.ledOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { viewModel.onUndoTapped() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                Text(
                    text = state.errorMessage.orEmpty(),
                    style = typography.label,
                    color = colors.danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { viewModel.onErrorDismissed() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            AnimatedVisibility(
                visible = isOffline,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    text = "Offline — changes will be queued",
                    style = typography.label,
                    color = colors.ledOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            AnimatedVisibility(
                visible = state.searchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ExpandableSearchField(
                    query = state.searchQuery,
                    onQueryChanged = viewModel::onSearchQueryChanged,
                    onDismiss = { viewModel.onSearchExpandedChanged(false) },
                    onImeAction = viewModel::onAddAffordanceTapped,
                    focusRequester = searchFocusRequester,
                )
            }
        }

        ViewDrawer(
            visible = state.drawerOpen,
            shops = state.drawerShops,
            onShopSelected = viewModel::onViewSelected,
            onShopEdit = viewModel::onShopEditTapped,
            onAddShop = viewModel::onAddShopTapped,
            onDismiss = viewModel::onDrawerDismissed,
            modifier = Modifier.matchParentSize(),
        )

        SettingsDrawer(
            visible = state.settingsDrawerOpen,
            activeThemeId = settingsTheme,
            changeToastsEnabled = settingsChangeToasts,
            onThemeSelected = settingsViewModel::onThemeSelected,
            onChangeToastsToggled = settingsViewModel::onChangeToastsToggled,
            onDismiss = viewModel::onSettingsDrawerDismissed,
            modifier = Modifier.matchParentSize(),
        )

        ChangeToastOverlay(
            toast = changeToast,
            onDismissed = viewModel::onToastSwipedDismissed,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (state.addItemModal != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { detectTapGestures { viewModel.onAddItemDiscard() } }
                    .background(colors.background.copy(alpha = 0.6f)),
            )
        }

        if (state.editItemModal != null || lastEditItemModal != null) {
            AnimatedVisibility(
                visible = state.editItemModal != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) { detectTapGestures { viewModel.onEditItemDiscard() } }
                        .background(colors.background.copy(alpha = 0.6f)),
                )
            }
        }

        AnimatedVisibility(
            visible = state.addItemModal != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            lastAddItemModal?.let { modal ->
                AddItemModal(
                    modal = modal,
                    onTitleChanged = viewModel::onAddItemTitleChanged,
                    onQuantityChanged = viewModel::onAddItemQuantityChanged,
                    onTemporaryToggled = viewModel::onAddItemTemporaryToggled,
                    onViewToggled = viewModel::onAddItemViewToggled,
                    onToggleAll = viewModel::onAddItemToggleAllTapped,
                    onSave = viewModel::onAddItemSave,
                    onDiscard = viewModel::onAddItemDiscard,
                    modifier = Modifier.background(colors.surface, shapes.large),
                )
            }
        }

        AnimatedVisibility(
            visible = state.editItemModal != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            lastEditItemModal?.let { modal ->
                EditItemModal(
                    modal = modal,
                    onNameChanged = viewModel::onEditItemTitleChanged,
                    onQuantityChanged = viewModel::onEditItemQuantityChanged,
                    onTemporaryToggled = viewModel::onEditItemTemporaryToggled,
                    onViewToggled = viewModel::onEditItemViewToggled,
                    onToggleAll = viewModel::onEditItemToggleAllTapped,
                    onSave = viewModel::onEditItemSave,
                    onDiscard = viewModel::onEditItemDiscard,
                    onDelete = viewModel::onEditItemDeleteTapped,
                    modifier = Modifier.background(colors.surface, shapes.large),
                )
            }
        }

        state.deleteConfirmItem?.let { item ->
            DeleteItemConfirmDialog(
                itemName = item.name,
                onConfirm = viewModel::onDeleteConfirmed,
                onCancel = viewModel::onDeleteCancelled,
            )
        }

        if (state.shopModal != null || lastShopModal != null) {
            AnimatedVisibility(
                visible = state.shopModal != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                lastShopModal?.let { modal ->
                    ShopModal(
                        modal = modal,
                        onNameChanged = viewModel::onShopNameChanged,
                        onSave = viewModel::onShopSave,
                        onDiscard = viewModel::onShopDiscard,
                        onDeleteTapped = viewModel::onShopDeleteTapped,
                        onDeleteConfirmed = viewModel::onShopDeleteConfirmed,
                        onDeleteCancelled = viewModel::onShopDeleteCancelled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { SEARCH_DISMISS_THRESHOLD.toPx() }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            Text(text = "Search or add", style = typography.body, color = colors.contentMuted)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (query.isNotBlank()) onImeAction() },
        ),
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
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear search",
                    tint = colors.contentMuted,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button) { onQueryChanged("") },
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                ) { _, dragAmount ->
                    dragTotal += dragAmount
                    if (dragTotal >= dismissThresholdPx) {
                        dragTotal = 0f
                        onDismiss()
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private val SEARCH_DISMISS_THRESHOLD = 96.dp

@Composable
private fun DeleteItemConfirmDialog(
    itemName: String,
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
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) { detectTapGestures { } }
                    .background(colors.surface, shapes.medium)
                    .padding(20.dp)
                    .semantics { paneTitle = "Delete item confirmation" },
            ) {
                Text(
                    text = "Delete \"$itemName\"?",
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
                    Text(
                        text = "Cancel",
                        style = typography.label,
                        color = colors.contentMuted,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(role = Role.Button) { onCancel() },
                    )
                    Text(
                        text = "Delete",
                        style = typography.label,
                        color = colors.danger,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(role = Role.Button) { onConfirm() },
                    )
                }
            }
        }
    }
}
