package eu.pascariu.shoppinglist.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun ListScreen(
    viewModel: ListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(state.postSaveSignal) {
        if (state.postSaveSignal != null) {
            listState.scrollToItem(0)
            searchFocusRequester.requestFocus()
            keyboard?.show()
            viewModel.consumePostSaveSignal()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Text(
            text = state.watermarkName,
            style = typography.display,
            color = colors.content,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(alpha = 0.07f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(state.filteredItems, key = { it.id }) { item ->
                    ItemRow(item = item, onCheckToggled = viewModel::onCheckToggled)
                }
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

            SearchField(
                query = state.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onImeAction = viewModel::onAddAffordanceTapped,
                focusRequester = searchFocusRequester,
            )

            AnimatedVisibility(
                visible = state.addItemModal != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                lastAddItemModal?.let { modal ->
                    AddItemModal(
                        modal = modal,
                        onTitleChanged = viewModel::onAddItemTitleChanged,
                        onViewToggled = viewModel::onAddItemViewToggled,
                        onToggleAll = viewModel::onAddItemToggleAllTapped,
                        onSave = viewModel::onAddItemSave,
                        onDiscard = viewModel::onAddItemDiscard,
                        modifier = Modifier.background(colors.surface, shapes.large),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onImeAction: () -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val shapes = LocalShoplistShapes.current

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
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
