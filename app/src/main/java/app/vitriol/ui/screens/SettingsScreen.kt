package app.vitriol.ui.screens

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
import app.vitriol.data.settings.SettingCategory
import app.vitriol.data.settings.SettingDescriptor
import app.vitriol.data.settings.SettingType
import app.vitriol.data.settings.SettingValueType
import app.vitriol.data.settings.SettingsManager
import app.vitriol.helper.setPlainWallpaper
import app.vitriol.ui.AppSelectionType
import app.vitriol.ui.BackHandler
import app.vitriol.ui.UiEvent
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private const val MAX_SIZE_FILL = 0.8f
private const val ALPHA = 0.5f

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

private data class SettingCallbacks(
    val onUpdate: suspend (String, Any) -> Unit,
    val onEmitEvent: suspend (UiEvent) -> Unit,
    val onNavigateToHiddenApps: () -> Unit,
)

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
    val settingsManager = remember { SettingsManager() }
    var currentDialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val effectiveLockState by viewModel.effectiveLockState.collectAsState()

    LaunchedEffect(Unit) {
        if (effectiveLockState && activity != null) {
            activity.showBiometricPrompt(
                activity = activity,
                onSuccess = { viewModel.setUnlocked(true) },
                onError = { error ->
                    viewModel.emitEvent(UiEvent.ShowToast("Authentication failed: $error"))
                },
            )
        }
    }

    // Expires the session unlock when the user leaves the settings screen.
    DisposableEffect(Unit) {
        onDispose { viewModel.resetUnlockState() }
    }

    BackHandler(onBack = {
        viewModel.resetUnlockState()
        onNavigateBack()
    })

    val callbacks =
        remember(viewModel, coroutineScope) {
            SettingCallbacks(
                onUpdate = { name, value -> viewModel.updateSetting(name, value) },
                onEmitEvent = { event -> viewModel.emitEvent(event) },
                onNavigateToHiddenApps = onNavigateToHiddenApps,
            )
        }

    SettingsDialogHandler(
        dialog = currentDialog,
        uiState = uiState,
        settingsManager = settingsManager,
        callbacks = callbacks,
        coroutineScope = coroutineScope,
        onDismiss = { currentDialog = null },
        onNavigateToDialog = { currentDialog = it },
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { paddingValues ->

        if (viewModel.loading.value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Single, authoritative lock gate.
        if (effectiveLockState) {
            LockedSettingsView(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                onUnlock = {
                    activity?.let { mainActivity ->
                        mainActivity.showBiometricPrompt(
                            activity = mainActivity,
                            onSuccess = { viewModel.setUnlocked(true) },
                            onError = { error ->
                                viewModel.emitEvent(UiEvent.ShowToast("Authentication failed: $error"))
                            },
                        )
                    }
                },
            )
            return@Scaffold
        }

        SettingsContent(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            uiState = uiState,
            settingsManager = settingsManager,
            onSettingClick = { descriptor ->
                when (descriptor.type) {
                    SettingType.TOGGLE -> Unit
                    SettingType.SLIDER -> currentDialog = SettingsDialog.Slider(descriptor)
                    SettingType.DROPDOWN -> currentDialog = SettingsDialog.Dropdown(descriptor)
                    SettingType.BUTTON -> {
                        if (descriptor.name == "plainWallpaper") {
                            setPlainWallpaper(context, android.R.color.black)
                        }
                    }
                    SettingType.APP_PICKER -> currentDialog = SettingsDialog.AppPicker(descriptor)
                }
            },
            viewModel = viewModel,
            context = context,
            activity = activity,
            coroutineScope = coroutineScope,
            onNavigateToHiddenApps = onNavigateToHiddenApps,
        )
    }
}

@Composable
private fun SettingsDialogHandler(
    dialog: SettingsDialog?,
    uiState: AppSettings,
    settingsManager: SettingsManager,
    callbacks: SettingCallbacks,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
    onNavigateToDialog: (SettingsDialog?) -> Unit,
) {
    dialog ?: return
    when (dialog) {
        is SettingsDialog.Slider -> {
            val currentFloat =
                when (dialog.descriptor.valueType) {
                    SettingValueType.INT -> (uiState.getValue(dialog.descriptor.name) as Int).toFloat()
                    SettingValueType.FLOAT -> uiState.getValue(dialog.descriptor.name) as Float
                    else -> 0f
                }
            SliderSettingDialog(
                title = dialog.descriptor.title,
                currentValue = currentFloat,
                min = dialog.descriptor.min,
                max = dialog.descriptor.max,
                step = dialog.descriptor.step,
                onDismiss = onDismiss,
                onValueSelected = { newValue ->
                    coroutineScope.launch {
                        when (dialog.descriptor.valueType) {
                            SettingValueType.INT -> callbacks.onUpdate(dialog.descriptor.name, newValue.toInt())
                            SettingValueType.FLOAT -> callbacks.onUpdate(dialog.descriptor.name, newValue)
                            else -> Unit
                        }
                    }
                },
            )
        }
        is SettingsDialog.Dropdown -> {
            DropdownSettingDialog(
                title = dialog.descriptor.title,
                options = dialog.descriptor.options,
                selectedIndex = uiState.getValue(dialog.descriptor.name) as Int,
                onDismiss = onDismiss,
                onOptionSelected = { index ->
                    coroutineScope.launch {
                        callbacks.onUpdate(dialog.descriptor.name, index)
                        if (dialog.descriptor.name.endsWith("Action") && index == Constants.SwipeAction.APP) {
                            val appName = dialog.descriptor.name.replace("Action", "App")
                            val appDescriptor = settingsManager.appPickerDescriptors[appName]
                            onNavigateToDialog(appDescriptor?.let { SettingsDialog.AppPicker(it) })
                        } else {
                            onDismiss()
                        }
                    }
                },
            )
        }
        is SettingsDialog.AppPicker -> {
            LaunchedEffect(dialog.descriptor.name) {
                appSelectionTypeFor(dialog.descriptor.name)?.let { selectionType ->
                    callbacks.onEmitEvent(UiEvent.NavigateToAppSelection(selectionType))
                }
                onDismiss()
            }
        }
    }
}

@Composable
private fun LockedSettingsView(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().padding(top = 128.dp), contentAlignment = Alignment.TopCenter) {
        Card(modifier = Modifier.padding(16.dp).fillMaxWidth(MAX_SIZE_FILL)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Settings Locked",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Settings are locked",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Authenticate to access settings",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth()) {
                    Text("Unlock Settings")
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    uiState: AppSettings,
    settingsManager: SettingsManager,
    onSettingClick: (SettingDescriptor) -> Unit,
    viewModel: SettingsViewModel,
    context: android.content.Context,
    activity: MainActivity?,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onNavigateToHiddenApps: () -> Unit,
) {
    LazyColumn(modifier = modifier) {
        val settingsByCategory = settingsManager.getSettingsByCategory()
        for (category in SettingCategory.entries) {
            val categorySettings = settingsByCategory[category] ?: continue
            item {
                SettingsSection(title = category.displayName()) {
                    categorySettings.forEach { descriptor ->
                        SettingItem(
                            descriptor = descriptor,
                            uiState = uiState,
                            settingsManager = settingsManager,
                            onSettingClick = onSettingClick,
                            onToggle = { name, value ->
                                coroutineScope.launch { viewModel.updateSetting(name, value) }
                            },
                        )
                    }
                }
            }
        }
        item {
            SettingsSection(title = "System") {
                SystemSettings(context, activity, uiState, viewModel, onNavigateToHiddenApps)
            }
        }
    }
}

@Composable
private fun SettingItem(
    descriptor: SettingDescriptor,
    uiState: AppSettings,
    settingsManager: SettingsManager,
    onSettingClick: (SettingDescriptor) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    val isEnabled = settingsManager.isSettingEnabled(uiState, descriptor)
    val value = uiState.getValue(descriptor.name)
    when (descriptor.type) {
        SettingType.TOGGLE -> {
            ToggleSettingItem(
                title = descriptor.title,
                description = descriptor.description.takeIf { it.isNotEmpty() },
                isChecked = value as Boolean,
                enabled = isEnabled,
                onCheckedChange = { checked -> onToggle(descriptor.name, checked) },
            )
        }
        SettingType.SLIDER -> {
            val subtitle =
                when (descriptor.valueType) {
                    SettingValueType.INT -> "${value as Int}"
                    SettingValueType.FLOAT -> String.format(Locale.getDefault(), "%.1f", value as Float)
                    else -> ""
                }
            SettingsItem(
                title = descriptor.title,
                subtitle = subtitle,
                description = descriptor.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onSettingClick(descriptor) },
            )
        }
        SettingType.DROPDOWN -> {
            val index = value as Int
            val displayText = descriptor.options.getOrNull(index) ?: "Unknown"
            val subtitle =
                if (descriptor.name.endsWith("Action")) {
                    val appName = descriptor.name.replace("Action", "App")
                    val appValue = uiState.getValue(appName) as? AppPreference
                    if (index == Constants.SwipeAction.APP) {
                        "$displayText: ${appValue?.label ?: "Select app"}"
                    } else {
                        displayText
                    }
                } else {
                    displayText
                }
            SettingsItem(
                title = descriptor.title,
                subtitle = subtitle,
                description = descriptor.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onSettingClick(descriptor) },
            )
        }
        SettingType.BUTTON -> {
            SettingsItem(
                title = descriptor.title,
                description = descriptor.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onSettingClick(descriptor) },
            )
        }
        SettingType.APP_PICKER -> {
            val appName = (value as? AppPreference)?.label ?: "Not set"
            SettingsItem(
                title = descriptor.title,
                subtitle = appName,
                description = descriptor.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onSettingClick(descriptor) },
            )
        }
    }
}

private fun SettingCategory.displayName(): String = name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 5.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                content()
            }
        }
    }
}

@Composable
private fun SettingTextBlock(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = onSurface.copy(alpha = if (enabled) 1f else 0.5f),
        )
        subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = onSurface.copy(alpha = 0.7f))
        }
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = if (enabled) 0.5f else 0.3f),
                modifier = Modifier.padding(top = 4.dp),
            )
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
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 8.dp)
                .alpha(if (enabled) 1f else ALPHA),
    ) {
        SettingTextBlock(
            title = title,
            subtitle = subtitle,
            description = description,
            modifier = Modifier.padding(16.dp),
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
                .padding(16.dp)
                .alpha(if (enabled) 1f else ALPHA),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingTextBlock(
            title = title,
            description = description,
            modifier = Modifier.weight(1f),
            enabled = enabled,
        )
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SliderSettingDialog(
    title: String,
    currentValue: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onValueSelected: (Float) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("%.1f".format(sliderValue))
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        val steps = ((it - min) / step).toInt()
                        sliderValue = min + (steps * step)
                    },
                    valueRange = min..max,
                    steps = ((max - min) / step).toInt() - 1,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueSelected(sliderValue)
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DropdownSettingDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit,
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = index }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == index, onClick = { selected = index })
                        Spacer(Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOptionSelected(selected) }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
                .clickable(enabled = enabled) { onCheckedChange(!isChecked) }
                .padding(16.dp)
                .alpha(if (enabled) 1f else ALPHA),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SystemSettings(
    context: android.content.Context,
    activity: MainActivity?,
    uiState: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateToHiddenApps: () -> Unit,
) {
    SettingsToggle(
        title = "Lock Settings",
        description = "Prevent changes to settings without biometric authentication",
        isChecked = uiState.lockSettings,
        onCheckedChange = { locked ->
            // Require biometrics for both enabling AND disabling.
            activity?.let { mainActivity ->
                mainActivity.showBiometricPrompt(
                    activity = mainActivity,
                    onSuccess = { viewModel.toggleLockSettings(locked) },
                    onError = { /* Auth failed — leave lock state unchanged */ },
                )
            }
        },
    )
    SettingsItem(title = "Hidden Apps", onClick = onNavigateToHiddenApps)
    SettingsItem(
        title = "About Vitriol",
        subtitle = "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = Constants.URL_ABOUT_VITRIOL.toUri()
                },
            )
        },
    )
}
