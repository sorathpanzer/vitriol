package app.vitriol.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vitriol.MainActivity
import app.vitriol.data.Constants
import app.vitriol.data.settings.AppPreference
import app.vitriol.data.settings.AppSettings
import app.vitriol.data.settings.SettingDescriptor
import app.vitriol.data.settings.SettingType
import app.vitriol.data.settings.SettingValueType
import app.vitriol.data.settings.SettingsManager
import app.vitriol.helper.setPlainWallpaper
import app.vitriol.ui.AppSelectionType
import app.vitriol.ui.BackHandler
import app.vitriol.ui.UiEvent
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// --- Constants ---

private object SettingsDimens {
    val PaddingSmall = 5.dp
    val PaddingNormal = 8.dp
    val PaddingMedium = 12.dp
    val PaddingLarge = 16.dp
    val PaddingExtraLarge = 24.dp
    val LockTopOffset = 128.dp
    val IconSizeLarge = 48.dp
    const val AlphaDisabled = 0.5f
    const val AlphaMuted = 0.7f
    const val CardWidthPercent = 0.8f
}

// --- Data Classes ---

private data class SettingsState(
    val uiState: AppSettings,
    val loading: Boolean,
    val isLocked: Boolean,
    val manager: SettingsManager,
)

private data class SettingsActions(
    val viewModel: SettingsViewModel,
    val scope: CoroutineScope,
    val activity: MainActivity?,
    val onNavigateToHiddenApps: () -> Unit,
    val onDialogChange: (SettingsDialog?) -> Unit,
)

private sealed class SettingsDialog {
    data class Slider(
        val descriptor: SettingDescriptor,
    ) : SettingsDialog()

    data class Dropdown(
        val descriptor: SettingDescriptor,
    ) : SettingsDialog()

    data class AppPicker(
        val descriptor: SettingDescriptor,
    ) : SettingsDialog()
}

// --- Main Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    var currentDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    SettingsLifecycleHandler(viewModel, activity, effectiveLockState, onNavigateBack)

    val manager = remember { SettingsManager() }
    val state = SettingsState(uiState, viewModel.loading.value, effectiveLockState, manager)
    val actions =
        SettingsActions(
            viewModel = viewModel,
            scope = coroutineScope,
            activity = activity,
            onNavigateToHiddenApps = onNavigateToHiddenApps,
            onDialogChange = { currentDialog = it },
        )

    SettingsDialogHandler(currentDialog, state, actions) { currentDialog = null }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        SettingsScreenBody(Modifier.padding(padding), state, actions)
    }
}

@Composable
private fun SettingsLifecycleHandler(
    vm: SettingsViewModel,
    activity: MainActivity?,
    isLocked: Boolean,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (isLocked) {
            activity?.showBiometricPrompt(
                activity,
                { vm.setUnlocked(true) },
                { vm.emitEvent(UiEvent.ShowToast("Failed: $it")) },
            )
        }
    }
    DisposableEffect(Unit) { onDispose { vm.resetUnlockState() } }
    BackHandler {
        vm.resetUnlockState()
        onBack()
    }
}

@Composable
private fun SettingsScreenBody(
    modifier: Modifier,
    state: SettingsState,
    actions: SettingsActions,
) {
    when {
        state.loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        state.isLocked ->
            LockedSettingsView(modifier.fillMaxSize()) {
                actions.activity?.showBiometricPrompt(
                    actions.activity,
                    { actions.viewModel.setUnlocked(true) },
                    { actions.viewModel.emitEvent(UiEvent.ShowToast("Auth failed: $it")) },
                )
            }
        else -> SettingsContent(modifier.fillMaxSize(), state, actions)
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier,
    state: SettingsState,
    actions: SettingsActions,
) {
    val context = LocalContext.current

    // Define the desired order of categories
    val categoryOrder = listOf("GENERAL", "APPEARANCE", "GESTURES", "SEARCH")

    LazyColumn(modifier = modifier) {
        state.manager
            .getSettingsByCategory()
            .toList()
            // Sort based on our defined list; if not found, put it at the end
            .sortedBy { (category, _) ->
                val index = categoryOrder.indexOf(category.name.uppercase())
                if (index != -1) index else Int.MAX_VALUE
            }.forEach { (category, settings) ->
                item {
                    val title =
                        category.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() }
                    SettingsSection(title = title) {
                        settings.forEach { descriptor ->
                            SettingItem(descriptor, state, actions, context)
                        }
                    }
                }
            }

        item {
            SettingsSection(title = "System") {
                SystemSettings(state.uiState, actions, context)
            }
        }
    }
}

@Composable
private fun SettingItem(
    descriptor: SettingDescriptor,
    state: SettingsState,
    actions: SettingsActions,
    context: Context,
) {
    val enabled = state.manager.isSettingEnabled(state.uiState, descriptor)
    val value = state.uiState.getValue(descriptor.name)

    if (descriptor.type == SettingType.TOGGLE) {
        ToggleSettingItem(
            title = descriptor.title,
            description = descriptor.description.takeIf { it.isNotEmpty() },
            isChecked = value as Boolean,
            enabled = enabled,
        ) {
            actions.scope.launch {
                actions.viewModel.updateSetting(descriptor.name, it)
            }
        }
    } else {
        SettingsItem(
            title = descriptor.title,
            subtitle = formatSubtitle(descriptor, value, state.uiState),
            description = descriptor.description.takeIf { it.isNotEmpty() },
            enabled = enabled,
            onClick = { handleSettingClick(descriptor, actions, context) },
        )
    }
}

private fun formatSubtitle(
    descriptor: SettingDescriptor,
    value: Any?,
    uiState: AppSettings,
): String? =
    when (descriptor.type) {
        SettingType.SLIDER -> if (value is Int) "$value" else "%.1f".format(value)
        SettingType.DROPDOWN -> {
            val label = descriptor.options.getOrNull(value as Int) ?: "Unknown"
            if (descriptor.name.endsWith("Action") && value == Constants.SwipeAction.APP) {
                val appKey = descriptor.name.replace("Action", "App")
                val app = uiState.getValue(appKey) as? AppPreference
                "$label: ${app?.label ?: "Select app"}"
            } else {
                label
            }
        }
        SettingType.APP_PICKER -> (value as? AppPreference)?.label ?: "Not set"
        else -> null
    }

private fun handleSettingClick(
    descriptor: SettingDescriptor,
    actions: SettingsActions,
    context: Context,
) {
    when (descriptor.type) {
        SettingType.SLIDER -> actions.onDialogChange(SettingsDialog.Slider(descriptor))
        SettingType.DROPDOWN -> actions.onDialogChange(SettingsDialog.Dropdown(descriptor))
        SettingType.APP_PICKER -> actions.onDialogChange(SettingsDialog.AppPicker(descriptor))
        SettingType.BUTTON -> {
            if (descriptor.name == "plainWallpaper") {
                setPlainWallpaper(context, android.R.color.black)
            }
        }
        else -> Unit
    }
}

@Composable
private fun SettingsDialogHandler(
    dialog: SettingsDialog?,
    state: SettingsState,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        is SettingsDialog.Slider -> {
            val current = state.uiState.getValue(dialog.descriptor.name)
            val floatVal = if (current is Int) current.toFloat() else current as Float
            SliderSettingDialog(
                title = dialog.descriptor.title,
                current = floatVal,
                min = dialog.descriptor.min,
                max = dialog.descriptor.max,
                step = dialog.descriptor.step,
                onDismiss = onDismiss,
            ) {
                actions.scope.launch {
                    val finalVal =
                        if (dialog.descriptor.valueType == SettingValueType.INT) {
                            it.toInt()
                        } else {
                            it
                        }
                    actions.viewModel.updateSetting(dialog.descriptor.name, finalVal)
                }
            }
        }
        is SettingsDialog.Dropdown ->
            DropdownSettingDialog(
                title = dialog.descriptor.title,
                options = dialog.descriptor.options,
                selected = state.uiState.getValue(dialog.descriptor.name) as Int,
                onDismiss = onDismiss,
            ) { index ->
                actions.scope.launch {
                    actions.viewModel.updateSetting(dialog.descriptor.name, index)
                    val isAppAction =
                        dialog.descriptor.name.endsWith("Action") &&
                            index == Constants.SwipeAction.APP
                    if (isAppAction) {
                        val appName = dialog.descriptor.name.replace("Action", "App")
                        state.manager.appPickerDescriptors[appName]?.let {
                            actions.onDialogChange(SettingsDialog.AppPicker(it))
                        }
                    } else {
                        onDismiss()
                    }
                }
            }
        is SettingsDialog.AppPicker ->
            LaunchedEffect(dialog.descriptor.name) {
                appSelectionTypeFor(dialog.descriptor.name)?.let {
                    actions.viewModel.emitEvent(UiEvent.NavigateToAppSelection(it))
                }
                onDismiss()
            }
        null -> Unit
    }
}

@Composable
private fun SystemSettings(
    uiState: AppSettings,
    actions: SettingsActions,
    context: Context,
) {
    SettingsToggle(
        title = "Lock Settings",
        description = "Biometric required",
        isChecked = uiState.lockSettings,
    ) { locked ->
        actions.activity?.let { act ->
            act.showBiometricPrompt(
                act,
                { actions.viewModel.toggleLockSettings(locked) },
                {},
            )
        }
    }
    SettingsItem("Hidden Apps", onClick = actions.onNavigateToHiddenApps)
    val version =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
    SettingsItem("About Vitriol", "Version $version") {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Constants.URL_ABOUT_VITRIOL.toUri()
            }
        context.startActivity(intent)
    }
}

@Composable
private fun LockedSettingsView(
    modifier: Modifier,
    onUnlock: () -> Unit,
) {
    Box(
        modifier = modifier.padding(top = SettingsDimens.LockTopOffset),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier =
                Modifier
                    .padding(SettingsDimens.PaddingLarge)
                    .fillMaxWidth(SettingsDimens.CardWidthPercent),
        ) {
            Column(
                modifier = Modifier.padding(SettingsDimens.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsDimens.IconSizeLarge),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Settings are locked",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(SettingsDimens.PaddingLarge))
                Button(
                    onClick = onUnlock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlock Settings")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = SettingsDimens.PaddingNormal,
                    horizontal = SettingsDimens.PaddingSmall,
                ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            Column {
                Text(
                    text = title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = SettingsDimens.PaddingLarge),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled, onClick = onClick)
                .alpha(if (enabled) 1f else SettingsDimens.AlphaDisabled),
    ) {
        Column(Modifier.padding(SettingsDimens.PaddingLarge)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = SettingsDimens.AlphaMuted),
                )
            }
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = SettingsDimens.AlphaDisabled),
                )
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!isChecked) }
                .padding(SettingsDimens.PaddingLarge),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = SettingsDimens.AlphaDisabled),
                )
            }
        }
        Switch(isChecked, onCheckedChange)
    }
}

@Composable
private fun ToggleSettingItem(
    title: String,
    description: String?,
    isChecked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled) { onCheckedChange(!isChecked) }
                .padding(SettingsDimens.PaddingLarge)
                .alpha(if (enabled) 1f else SettingsDimens.AlphaDisabled),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = SettingsDimens.AlphaDisabled),
                )
            }
        }
        Switch(isChecked, onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SliderSettingDialog(
    title: String,
    current: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit,
) {
    var value by remember { mutableFloatStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("%.1f".format(value))
                Slider(
                    value = value,
                    onValueChange = {
                        val s = ((it - min) / step).toInt()
                        value = min + (s * step)
                    },
                    valueRange = min..max,
                    steps = ((max - min) / step).toInt() - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelect(value)
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DropdownSettingDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    var current by remember { mutableIntStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { i, opt ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { current = i }
                                .padding(
                                    vertical = SettingsDimens.PaddingMedium,
                                    horizontal = SettingsDimens.PaddingLarge,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(current == i, { current = i })
                        Spacer(Modifier.width(SettingsDimens.PaddingNormal))
                        Text(opt)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(current) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun appSelectionTypeFor(name: String): AppSelectionType? =
    when (name) {
        "swipeLeftApp" -> AppSelectionType.SWIPE_LEFT_APP
        "swipeRightApp" -> AppSelectionType.SWIPE_RIGHT_APP
        "oneTapApp" -> AppSelectionType.ONE_TAP_APP
        "doubleTapApp" -> AppSelectionType.DOUBLE_TAP_APP
        "swipeUpApp" -> AppSelectionType.SWIPE_UP_APP
        "swipeDownApp" -> AppSelectionType.SWIPE_DOWN_APP
        "twoFingerSwipeUpApp" -> AppSelectionType.TWOFINGER_SWIPE_UP_APP
        "twoFingerSwipeDownApp" -> AppSelectionType.TWOFINGER_SWIPE_DOWN_APP
        "twoFingerSwipeLeftApp" -> AppSelectionType.TWOFINGER_SWIPE_LEFT_APP
        "twoFingerSwipeRightApp" -> AppSelectionType.TWOFINGER_SWIPE_RIGHT_APP
        "pinchInApp" -> AppSelectionType.PINCH_IN_APP
        "pinchOutApp" -> AppSelectionType.PINCH_OUT_APP
        else -> null
    }
