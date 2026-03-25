package app.vitriol.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

private const val ANIMATION_DURATION_MS = 300
private const val DELAY_APP_OPEN = ANIMATION_DURATION_MS.toLong()

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
    val hiddenApps by viewModel.hiddenApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    val handleAppClick = remember(viewModel, onAppClick, focusManager, keyboardController) {
        { app: AppModel ->
            searchQuery = ""
            focusManager.clearFocus()
            keyboardController?.hide()
            onAppClick(app)
        }
    }

    LaunchedEffect(Unit) { viewModel.loadApps() }
    LaunchedEffect(searchQuery) { viewModel.searchApps(searchQuery) }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

    val scrollState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty() &&
            (scrollState.firstVisibleItemIndex != 0 || scrollState.firstVisibleItemScrollOffset != 0)
        ) scrollState.scrollToItem(0)
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .detectSwipeGestures(
                onSwipeDown = { onSwipeDown() },
                onSwipeUp = {
                    if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0)
                        onSwipeDown()
                },
            )
            .statusBarsPadding(),
    ) {
        if (selectionMode) {
            TopAppBar(
                title = {
                    Text(
                        text = selectionTitle,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        }

        val appsToShow = if (searchQuery.isEmpty()) uiState.apps else uiState.filteredApps

        AppDrawerSearch(
            searchQuery = searchQuery,
            onSearchChanged = { searchQuery = it },
            modifier = Modifier.focusRequester(focusRequester),
            onEnterPressed = { appsToShow.firstOrNull()?.let { handleAppClick(it) } },
            onFocusStateChanged = { },
            onSearchAction = { viewModel.searchApps(searchQuery, true) },
        )

        LaunchedEffect(searchQuery, appsToShow) {
            if (searchQuery.isNotEmpty() && appsToShow.size == 1) {
                delay(DELAY_APP_OPEN)
                handleAppClick(appsToShow[0])
            }
        }

        when {
            uiState.loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Error: ${uiState.error}") }
            uiState.apps.isEmpty() && searchQuery.isEmpty() ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No apps found") }
            uiState.filteredApps.isEmpty() && searchQuery.isNotEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.TopCenter) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("No apps found")
                    }
                }
            }
            else -> {
                val visibleApps = if (settings.showAppNames) appsToShow else emptyList()
                LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
                    items(
                        items = visibleApps,
                        key = { app -> "${app.appPackage}/${app.activityClassName ?: ""}/${app.user.hashCode()}" },
                    ) { app ->
                        AppListItem(
                            app = app,
                            fontScale = settings.searchResultsFontSize,
                            onClick = { handleAppClick(app) },
                            onLongClick = { selectedApp = app; showContextMenu = true },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = tween(durationMillis = ANIMATION_DURATION_MS),
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showContextMenu) {
        val app = selectedApp ?: return
        val hiddenKeys = remember(hiddenApps) { hiddenApps.map { it.getKey() }.toSet() }
        val hidden = app.getKey() in hiddenKeys
        var renameDialogVisible by remember { mutableStateOf(false) }
        var newAppName by remember { mutableStateOf(app.appLabel) }
        fun dismiss() { showContextMenu = false; selectedApp = null }

        AlertDialog(
            onDismissRequest = { dismiss() },
            title = { Text(app.appLabel) },
            text = {
                Column {
                    ContextMenuItem("Open App", Icons.Default.AdsClick) { handleAppClick(app); dismiss() }
                    ContextMenuItem(if (hidden) "Unhide App" else "Hide App", Icons.Default.Settings) {
                        viewModel.toggleAppHidden(app); dismiss()
                    }
                    ContextMenuItem("Rename App", Icons.Default.DriveFileRenameOutline) { renameDialogVisible = true }
                    ContextMenuItem("App Info", Icons.Default.Info) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.appPackage, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                        dismiss()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dismiss() }) { Text("Close") } },
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
                    TextButton(onClick = { viewModel.renameApp(app, newAppName); renameDialogVisible = false; dismiss() }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { renameDialogVisible = false }) { Text("Cancel") }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppModel,
    fontScale: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(ANIMATION_DURATION_MS))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = app.appLabel,
            style = MaterialTheme.typography.bodyLarge.copy(
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
private fun ContextMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp))
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
    onSearchAction: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SearchTextField(
            searchQuery = searchQuery,
            onSearchChanged = onSearchChanged,
            onEnterPressed = onEnterPressed,
            onFocusStateChanged = onFocusStateChanged,
            onSearchAction = onSearchAction,
        )
    }
}

@Composable
private fun SearchTextField(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onEnterPressed: () -> Unit,
    onFocusStateChanged: (Boolean) -> Unit,
    onSearchAction: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .width(minOf(maxWidth, 600.dp))
                .onFocusChanged {
                    onFocusStateChanged(it.isFocused)
                    if (it.isFocused) keyboardController?.show()
                },
            placeholder = {
                Text("Search App...", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            singleLine = true,
            textStyle = TextStyle(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearchAction()
                onEnterPressed()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}
