package app.vitriol.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import app.vitriol.MainViewModel
import app.vitriol.data.Constants
import app.vitriol.data.settings.AppSettings
import app.vitriol.helper.expandNotificationDrawer
import app.vitriol.helper.isAccessServiceEnabled
import app.vitriol.ui.util.detectPinchGestures
import app.vitriol.ui.util.detectSwipeGestures
import app.vitriol.ui.util.detectTwoFingerSwipes
import app.vitriol.ui.viewmodels.SettingsViewModel

private const val ZOOM_DELTA_VAL = 150f

private fun checkAccessibilityAndLock(
    context: Context,
    viewModel: MainViewModel,
) {
    if (!isAccessServiceEnabled(context)) {
        Toast.makeText(context, "Enable accessibility permission to lock screen.", Toast.LENGTH_SHORT).show()
        val intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } else {
        viewModel.lockScreen()
    }
}

private fun handleAction(
    context: Context,
    viewModel: MainViewModel,
    action: Int,
    onSearch: () -> Unit,
    launchApp: () -> Unit = {},
) {
    when (action) {
        Constants.SwipeAction.NOTIFICATIONS -> expandNotificationDrawer(context)
        Constants.SwipeAction.SEARCH -> onSearch()
        Constants.SwipeAction.APP -> launchApp()
        Constants.SwipeAction.LOCKSCREEN -> checkAccessibilityAndLock(context, viewModel)
        Constants.SwipeAction.NULL -> Unit
        else -> Unit
    }
}

private fun Modifier.applySwipeGestures(
    context: Context,
    viewModel: MainViewModel,
    settings: AppSettings,
    onNavigateToAppDrawer: () -> Unit,
) = this.detectSwipeGestures(
    onSwipeUp = {
        handleAction(context, viewModel, settings.swipeUpAction, onNavigateToAppDrawer) {
            viewModel.launchSwipeUpApp()
        }
    },
    onSwipeDown = {
        handleAction(context, viewModel, settings.swipeDownAction, onNavigateToAppDrawer) {
            viewModel.launchSwipeDownApp()
        }
    },
    onSwipeLeft = {
        handleAction(context, viewModel, settings.swipeLeftAction, onNavigateToAppDrawer) {
            viewModel.launchSwipeLeftApp()
        }
    },
    onSwipeRight = {
        handleAction(context, viewModel, settings.swipeRightAction, onNavigateToAppDrawer) {
            viewModel.launchSwipeRightApp()
        }
    },
)

private fun Modifier.applyTwoFingerSwipes(
    context: Context,
    viewModel: MainViewModel,
    settings: AppSettings,
    onNavigateToAppDrawer: () -> Unit,
) = this.detectTwoFingerSwipes(
    onSwipeUp = {
        handleAction(context, viewModel, settings.twoFingerSwipeUpAction, onNavigateToAppDrawer) {
            viewModel.launchTwoFingerSwipeUpApp()
        }
    },
    onSwipeDown = {
        handleAction(context, viewModel, settings.twoFingerSwipeDownAction, onNavigateToAppDrawer) {
            viewModel.launchTwoFingerSwipeDownApp()
        }
    },
    onSwipeRight = {
        handleAction(context, viewModel, settings.twoFingerSwipeRightAction, onNavigateToAppDrawer) {
            viewModel.launchTwoFingerSwipeRightApp()
        }
    },
    onSwipeLeft = {
        handleAction(context, viewModel, settings.twoFingerSwipeLeftAction, onNavigateToAppDrawer) {
            viewModel.launchTwoFingerSwipeLeftApp()
        }
    },
)

private fun Modifier.applyPinchGestures(
    context: Context,
    viewModel: MainViewModel,
    settings: AppSettings,
    onNavigateToAppDrawer: () -> Unit,
) = this.detectPinchGestures { zoomDelta ->
    when {
        zoomDelta > ZOOM_DELTA_VAL -> {
            handleAction(context, viewModel, settings.pinchOutAction, onNavigateToAppDrawer) {
                viewModel.launchPinchOutApp()
            }
        }

        zoomDelta < -ZOOM_DELTA_VAL -> {
            handleAction(context, viewModel, settings.pinchInAction, onNavigateToAppDrawer) {
                viewModel.launchPinchInApp()
            }
        }
    }
}

private fun Modifier.applyTapGestures(
    context: Context,
    viewModel: MainViewModel,
    settings: AppSettings,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) = this.pointerInput(Unit) {
    detectTapGestures(
        onDoubleTap = {
            handleAction(context, viewModel, settings.doubleTapAction, onNavigateToAppDrawer) {
                viewModel.launchDoubleTapApp()
            }
        },
        onLongPress = { onNavigateToSettings() },
        onTap = {
            handleAction(context, viewModel, settings.oneTapAction, onNavigateToAppDrawer) {
                viewModel.launchOneTapApp()
            }
        },
    )
}

@Composable
private fun Modifier.applyGestureHandlers(
    context: Context,
    viewModel: MainViewModel,
    settings: AppSettings,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
): Modifier =
    this
        .applySwipeGestures(context, viewModel, settings, onNavigateToAppDrawer)
        .applyTwoFingerSwipes(context, viewModel, settings, onNavigateToAppDrawer)
        .applyPinchGestures(context, viewModel, settings, onNavigateToAppDrawer)
        .applyTapGestures(context, viewModel, settings, onNavigateToAppDrawer, onNavigateToSettings)

@Composable
internal fun homeScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToAppDrawer: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .applyGestureHandlers(
                    context = context,
                    viewModel = viewModel,
                    settings = settings,
                    onNavigateToAppDrawer = onNavigateToAppDrawer,
                    onNavigateToSettings = onNavigateToSettings,
                ),
    ) {
        // * Add your HomeScreen UI content here
    }
}
