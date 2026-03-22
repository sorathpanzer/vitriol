package app.vitriol.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vitriol.MainViewModel
import app.vitriol.data.AppModel
import app.vitriol.data.Constants
import app.vitriol.helper.openSearch
import app.vitriol.ui.BackHandler
import app.vitriol.ui.util.detectSwipeGestures
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

private const val DELAY_APP_OPEN = 300L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AppDrawerScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    onAppClick: (AppModel) -> Unit,
    selectionMode: Boolean = false,
    selectionTitle: String = "",
    onSwipeDown: () -> Unit,
) {
    BackHandler(onBack = onSwipeDown)

    val context = LocalContext.current
    val uiState by viewModel.appDrawerState.collectAsState()
    val settings by settingsViewModel.settingsState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isSearchFocused by remember { mutableStateOf(false) }

    val searchResultsFontSize = settings.searchResultsFontSize

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val handleAppClick: (AppModel) -> Unit = { app ->
        searchQuery = ""
        viewModel.searchApps("")
        focusManager.clearFocus()
        keyboardController?.hide()
        onAppClick(app)
    }

    LaunchedEffect(Unit) { viewModel.loadApps() }
    LaunchedEffect(searchQuery) { viewModel.searchApps(searchQuery) }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    val scrollState = rememberLazyListState()

    LaunchedEffect(searchQuery, scrollState) {
        if (searchQuery.isEmpty() &&
            (scrollState.firstVisibleItemIndex != 0 || scrollState.firstVisibleItemScrollOffset != 0)
        ) {
            scrollState.scrollToItem(0)
        }
    }

    LaunchedEffect(scrollState, keyboardController, focusManager, focusRequester, isSearchFocused) {
        var previousIndex = scrollState.firstVisibleItemIndex
        var previousOffset = scrollState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset,
                scrollState.isScrollInProgress,
            )
        }.collect { (currentIndex, currentOffset, isScrolling) ->
            if (isScrolling) {
                val actualScrollHappened = currentIndex != previousIndex || currentOffset != previousOffset
                if (actualScrollHappened) {
                    val verticalScrollDelta =
                        when {
                            currentIndex > previousIndex -> 1
                            currentIndex < previousIndex -> -1
                            else -> currentOffset - previousOffset
                        }

                    if (verticalScrollDelta > 0) {
                        if (isSearchFocused) {
                            focusManager.clearFocus()
                        }
                        keyboardController?.hide()
                    } else if (verticalScrollDelta < 0) {
                        if (currentIndex == 0 && currentOffset == 0) {
                            if (!isSearchFocused) {
                                focusRequester.requestFocus()
                            }
                        }
                    }
                }
            }
            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .detectSwipeGestures(
                    onSwipeDown = { onSwipeDown() },
                    onSwipeUp = {
                        if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
                            onSwipeDown()
                        }
                    },
                ).statusBarsPadding(),
    ) {
        if (selectionMode) {
            TopAppBar(
                title = {
                    Text(
                        text = selectionTitle,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }

        AppDrawerSearch(
            searchQuery = searchQuery,
            onSearchChanged = { query ->
                searchQuery = query
                viewModel.searchApps(query)
            },
            modifier = Modifier.focusRequester(focusRequester),
            onEnterPressed = {
                if (uiState.filteredApps.isNotEmpty()) {
                    handleAppClick(uiState.filteredApps[0])
                }
            },
            onFocusStateChanged = { focused ->
                isSearchFocused = focused
            },
            showCalculatorResult = uiState.showCalculatorResult,
            calculatorResult = uiState.calculatorResult,
            viewModel = viewModel, // Add this line to pass the viewModel
        )

        val appsToShow = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps

        LaunchedEffect(appsToShow, searchQuery, handleAppClick) {
            delay(DELAY_APP_OPEN)
            if (searchQuery.isNotEmpty() && appsToShow.size == 1) {
                handleAppClick(appsToShow[0])
            }
        }

        when {
            uiState.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Error: ${uiState.error}") }
            uiState.apps.isEmpty() && searchQuery.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No apps found")
                }
            uiState.filteredApps.isEmpty() && searchQuery.isNotEmpty() -> {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 250.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (!uiState.showCalculatorResult) {
                            Button(
                                onClick = {
                                    if (searchQuery.startsWith("!")) {
                                        context.openSearch(
                                            Constants.URL_DUCK_SEARCH + searchQuery.substring(1).replace(" ", "%20"),
                                        )
                                    } else {
                                        context.openSearch(searchQuery.trim())
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp),
                            ) { Text("Search Web") }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = appsToShow,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" },
                    ) { app ->
                        if (settings.showAppNames) {
                            AppListItem(
                                app = app,
                                showAppNames = settings.showAppNames,
                                fontScale = searchResultsFontSize,
                                onClick = { handleAppClick(app) },
                                onLongClick = {
                                    selectedApp = app
                                    showContextMenu = true
                                },
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null,
                                        placementSpec = tween(durationMillis = 300),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showContextMenu && selectedApp != null) {
        val app = selectedApp ?: return
        val hiddenApps by viewModel.hiddenApps.collectAsState()
        val hidden = hiddenApps.any { it.getKey() == app.getKey() }

        var renameDialogVisible by remember { mutableStateOf(false) }
        var newAppName by remember { mutableStateOf(app.appLabel) }

        AlertDialog(
            onDismissRequest = {
                showContextMenu = false
                selectedApp = null
            },
            title = { Text(app.appLabel) },
            text = {
                Column {
                    ContextMenuItem("Open App", Icons.Default.AdsClick) {
                        handleAppClick(app)
                        showContextMenu = false
                        selectedApp = null
                    }
                    ContextMenuItem(if (hidden) "Unhide App" else "Hide App", Icons.Default.Settings) {
                        viewModel.toggleAppHidden(app)
                        showContextMenu =
                            false
                        selectedApp = null
                    }
                    ContextMenuItem("Rename App", Icons.Default.DriveFileRenameOutline) { renameDialogVisible = true }
                    ContextMenuItem("App Info", Icons.Default.Info) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.appPackage, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                        showContextMenu = false
                        selectedApp = null
                    }
                }
            },
            confirmButton = {
                TextButton({
                    showContextMenu = false
                    selectedApp = null
                }) { Text("Close") }
            },
        )

        if (renameDialogVisible) {
            AlertDialog(
                onDismissRequest = { renameDialogVisible = false },
                title = { Text("Rename ${app.appLabel}") },
                text = {
                    TextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("New name") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renameApp(app, newAppName)
                        renameDialogVisible = false
                        showContextMenu = false
                        selectedApp = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogVisible = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppModel,
    showAppNames: Boolean,
    fontScale: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 300))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (showAppNames) app.appLabel else "",
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ContextMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, text, Modifier.padding(end = 16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AppDrawerSearch(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEnterPressed: () -> Unit = {},
    onFocusStateChanged: (Boolean) -> Unit,
    showCalculatorResult: Boolean,
    calculatorResult: String,
    viewModel: MainViewModel,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showCalculatorResult) {
            CalculatorResultDisplay(searchQuery, calculatorResult)
        }

        SearchTextField(
            searchQuery = searchQuery,
            onSearchChanged = onSearchChanged,
            onEnterPressed = onEnterPressed,
            onFocusStateChanged = onFocusStateChanged,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun CalculatorResultDisplay(
    searchQuery: String,
    calculatorResult: String,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        with(MaterialTheme.typography) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = searchQuery,
                    style = titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    text = calculatorResult,
                    style = displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SearchTextField(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onEnterPressed: () -> Unit,
    onFocusStateChanged: (Boolean) -> Unit,
    viewModel: MainViewModel,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val width = minOf(maxWidth, 600.dp)

        TextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier =
                Modifier
                    .width(width)
                    .onFocusChanged {
                        onFocusStateChanged(it.isFocused)
                        if (it.isFocused) keyboardController?.show()
                    },
            placeholder = {
                Text(
                    "Search App...",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            singleLine = true,
            textStyle =
                TextStyle(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.searchApps(searchQuery, true)
                        onEnterPressed()
                    },
                ),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }
}
