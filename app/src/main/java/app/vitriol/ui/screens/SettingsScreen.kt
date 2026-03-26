package app.vitriol.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.vitriol.data.Constants
import app.vitriol.data.settings.AppPreference
import app.vitriol.data.settings.AppSettings
import app.vitriol.data.settings.Setting
import app.vitriol.data.settings.SettingCategory
import app.vitriol.data.settings.SettingType
import app.vitriol.data.settings.SettingsManager
import app.vitriol.helper.isVitriolDefault
import app.vitriol.helper.setPlainWallpaper
import app.vitriol.ui.AppSelectionType
import app.vitriol.ui.UiEvent
import app.vitriol.ui.BackHandler
import app.vitriol.ui.dialogs.SettingsLockDialog
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.reflect.KProperty1

private const val MAX_SIZE_FILL = 0.8f
private const val ALPHA = 0.5f

private sealed class SettingsDialog {
    data class Slider(
        val property: KProperty1<AppSettings, *>,
        val annotation: Setting,
    ) : SettingsDialog()

    data class Dropdown(
        val property: KProperty1<AppSettings, *>,
        val annotation: Setting,
    ) : SettingsDialog()

    data class AppPicker(
        val property: KProperty1<AppSettings, *>,
    ) : SettingsDialog()
}

private fun appSelectionTypeFor(propertyName: String): AppSelectionType? =
    when (propertyName) {
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
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager() }

    var currentDialog by remember { mutableStateOf<SettingsDialog?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetUnlockState() }
    }

    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val settingPin by viewModel.settingPin.collectAsState()

    BackHandler(onBack = {
        viewModel.resetUnlockState()
        onNavigateBack()
    })

    if (showLockDialog) {
        SettingsLockDialog(
            settingPin = settingPin,
            onDismiss = { viewModel.setShowLockDialog(false) },
            onConfirm = { pin ->
                if (settingPin) {
                    viewModel.setPin(pin)
                    viewModel.toggleLockSettings(true)
                    viewModel.setShowLockDialog(false)
                } else {
                    if (viewModel.validatePin(pin)) {
                        viewModel.setShowLockDialog(false)
                    }
                }
            },
        )
    }

    val callbacks = remember(viewModel, coroutineScope) {
        SettingCallbacks(
            onUpdate = { name, value -> viewModel.updateSetting(name, value) },
            onEmitEvent = { event -> viewModel.emitEvent(event) },
            onNavigateToHiddenApps = onNavigateToHiddenApps,
        )
    }

    SettingsDialogHandler(
        dialog = currentDialog,
        uiState = uiState,
        callbacks = callbacks,
        coroutineScope = coroutineScope,
        onDismiss = { currentDialog = null },
        onNavigateToDialog = { currentDialog = it },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (viewModel.loading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (effectiveLockState) {
            LockedSettingsView(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                onUnlock = { viewModel.setShowLockDialog(true, false) },
            )
            return@Scaffold
        }

        SettingsContent(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            uiState = uiState,
            settingsManager = settingsManager,
            onSettingClick = { property, annotation ->
                when (annotation.type) {
                    SettingType.TOGGLE -> Unit
                    SettingType.SLIDER -> currentDialog = SettingsDialog.Slider(property, annotation)
                    SettingType.DROPDOWN -> currentDialog = SettingsDialog.Dropdown(property, annotation)
                    SettingType.BUTTON -> {
                        when (property.name) {
                            "plainWallpaper" -> setPlainWallpaper(context, android.R.color.black)
                        }
                    }
                    SettingType.APP_PICKER -> currentDialog = SettingsDialog.AppPicker(property)
                }
            },
            viewModel = viewModel,
            context = context,
            coroutineScope = coroutineScope,
            onNavigateToHiddenApps = onNavigateToHiddenApps,
        )
    }
}

@Composable
private fun SettingsDialogHandler(
    dialog: SettingsDialog?,
    uiState: AppSettings,
    callbacks: SettingCallbacks,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
    onNavigateToDialog: (SettingsDialog?) -> Unit,
) {
    dialog ?: return

    when (dialog) {
        is SettingsDialog.Slider -> {
            SliderSettingDialog(
                title = dialog.annotation.title,
                currentValue = dialog.property.currentFloatValue(uiState),
                min = dialog.annotation.min,
                max = dialog.annotation.max,
                step = dialog.annotation.step,
                onDismiss = onDismiss,
                onValueSelected = { newValue ->
                    coroutineScope.launch {
                        val propertyName = dialog.property.name
                        when (dialog.property.returnType.classifier) {
                            Int::class -> callbacks.onUpdate(propertyName, newValue.toInt())
                            Float::class -> callbacks.onUpdate(propertyName, newValue)
                        }
                    }
                },
            )
        }

        is SettingsDialog.Dropdown -> {
            DropdownSettingDialog(
                title = dialog.annotation.title,
                options = dialog.annotation.options.toList(),
                selectedIndex = dialog.property.get(uiState) as Int,
                onDismiss = onDismiss,
                onOptionSelected = { index ->
                    coroutineScope.launch {
                        callbacks.onUpdate(dialog.property.name, index)
                        if (dialog.property.name.endsWith("Action") && index == Constants.SwipeAction.APP) {
                            val appPropertyName = dialog.property.name.replace("Action", "App")
                            val appProperty = AppSettings::class
                                .members
                                .filterIsInstance<KProperty1<AppSettings, *>>()
                                .firstOrNull { it.name == appPropertyName }
                            onNavigateToDialog(appProperty?.let { SettingsDialog.AppPicker(it) })
                        } else {
                            onDismiss()
                        }
                    }
                },
            )
        }

        is SettingsDialog.AppPicker -> {
            LaunchedEffect(dialog) {
                appSelectionTypeFor(dialog.property.name)?.let { selectionType ->
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(MAX_SIZE_FILL),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Settings Locked",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Settings are locked",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your PIN to access settings",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
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
    onSettingClick: (KProperty1<AppSettings, *>, Setting) -> Unit,
    viewModel: SettingsViewModel,
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onNavigateToHiddenApps: () -> Unit,
) {
    LazyColumn(modifier = modifier) {
        val settingsByCategory = settingsManager.getSettingsByCategory()

        for (category in SettingCategory.entries) {
            val categorySettings = settingsByCategory[category] ?: continue
            item {
                SettingsSection(title = category.displayName()) {
                    categorySettings.forEach { (property, annotation) ->
                        SettingItem(
                            property = property,
                            annotation = annotation,
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
                SystemSettings(
                    context = context,
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateToHiddenApps = onNavigateToHiddenApps,
                )
            }
        }
    }
}

// ... (SettingItem, ToggleSettingItem, SliderSettingItem, etc. remain mostly the same but cleaner)

@Composable
private fun SettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    settingsManager: SettingsManager,
    onSettingClick: (KProperty1<AppSettings, *>, Setting) -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    val isEnabled = settingsManager.isSettingEnabled(uiState, annotation)

    when (annotation.type) {
        SettingType.TOGGLE -> {
            if (property.returnType.classifier == Boolean::class) {
                ToggleSettingItem(
                    title = annotation.title,
                    description = annotation.description,
                    isChecked = property.get(uiState) as Boolean,
                    enabled = isEnabled,
                    onCheckedChange = { checked -> onToggle(property.name, checked) },
                )
            }
        }
        SettingType.SLIDER -> {
            SliderSettingItem(
                property = property,
                annotation = annotation,
                uiState = uiState,
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )
        }
        SettingType.DROPDOWN -> {
            DropdownSettingItem(
                property = property,
                annotation = annotation,
                uiState = uiState,
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )
        }
        SettingType.BUTTON -> {
            SettingsItem(  // Now uses the standard item instead of custom SettingsAction
                title = annotation.title,
                description = annotation.description.takeIf { it.isNotEmpty() },
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )
        }
        SettingType.APP_PICKER -> {
            AppPickerSettingItem(
                property = property,
                annotation = annotation,
                uiState = uiState,
                enabled = isEnabled,
                onClick = { onSettingClick(property, annotation) },
            )
        }
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
        modifier = Modifier
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
private fun SliderSettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    SettingsItem(
        title = annotation.title,
        subtitle = when (property.returnType.classifier) {
            Int::class -> "${property.get(uiState) as Int}"
            Float::class -> String.format(Locale.getDefault(), "%.1f", property.get(uiState) as Float)
            else -> ""
        },
        description = annotation.description.takeIf { it.isNotEmpty() },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun DropdownSettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val options = annotation.options
    val value = property.get(uiState) as Int
    val displayText = options.getOrNull(value) ?: "Unknown"

    val subtitle = if (property.name.endsWith("Action")) {
        val appPropertyName = property.name.replace("Action", "App")
        val appProperty = AppSettings::class
            .members
            .filterIsInstance<KProperty1<AppSettings, *>>()
            .firstOrNull { it.name == appPropertyName }

        val appName = appProperty?.let {
            when (val appPref = it.get(uiState)) {
                is AppPreference -> appPref.label
                else -> "Select app"
            }
        } ?: "Select app"

        if (value == Constants.SwipeAction.APP) "$displayText: $appName" else displayText
    } else {
        displayText
    }

    SettingsItem(
        title = annotation.title,
        subtitle = subtitle,
        description = annotation.description.takeIf { it.isNotEmpty() },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun AppPickerSettingItem(
    property: KProperty1<AppSettings, *>,
    annotation: Setting,
    uiState: AppSettings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val appName = when (val appPref = property.get(uiState)) {
        is AppPreference -> appPref.label
        else -> "Not set"
    }

    SettingsItem(
        title = annotation.title,
        subtitle = appName,
        description = annotation.description.takeIf { it.isNotEmpty() },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun SystemSettings(
    context: android.content.Context,
    uiState: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateToHiddenApps: () -> Unit,
) {
    SettingsItem(
        title = "Set as Default Launcher",
        subtitle = if (isVitriolDefault(context)) "Vitriol is default" else "Vitriol is not default",
        onClick = {
            context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        },
        enabled = true,
    )

    SettingsToggle(
        title = "Lock Settings",
        description = "Prevent changes to settings without a PIN",
        isChecked = uiState.lockSettings,
        onCheckedChange = { locked ->
            if (locked) viewModel.setShowLockDialog(true, true)
            else viewModel.toggleLockSettings(false)
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
                }
            )
        },
    )
}

// Extension functions
private fun KProperty1<AppSettings, *>.currentFloatValue(state: AppSettings): Float =
    when (returnType.classifier) {
        Int::class -> (get(state) as Int).toFloat()
        Float::class -> get(state) as Float
        else -> 0f
    }

private fun SettingCategory.displayName(): String =
    name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }

// Reusable UI components
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
            color = onSurface.copy(alpha = if (enabled) 1f else 0.5f)
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
        modifier = Modifier
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
        modifier = Modifier
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
            enabled = enabled
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
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
                        modifier = Modifier
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
