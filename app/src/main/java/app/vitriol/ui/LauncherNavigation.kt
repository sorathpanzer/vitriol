package app.vitriol.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.vitriol.MainViewModel
import app.vitriol.data.Constants
import app.vitriol.data.Navigation
import app.vitriol.ui.screens.appDrawerScreen
import app.vitriol.ui.screens.hiddenAppsScreen
import app.vitriol.ui.screens.homeScreen
import app.vitriol.ui.screens.settingsScreen
import app.vitriol.ui.util.systemUIController
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

private const val ANIMATION_TWEEN_VAL = 300

private data class NavigationControllers(
    val mainViewModel: MainViewModel,
    val settingsViewModel: SettingsViewModel,
)

private data class NavigationState(
    val currentScreen: String,
    val currentSelectionType: AppSelectionType?,
    val onClearSelection: () -> Unit,
    val onScreenChange: (String) -> Unit,
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun vitriolNavigation(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    currentScreen: String,
    onScreenChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()
    systemUIController(immersiveMode = settings.immersiveMode)

    var currentSelectionType by remember { mutableStateOf<AppSelectionType?>(null) }

    val handleEvent =
        remember(context, onScreenChange) {
            { event: UiEvent ->
                when (event) {
                    is UiEvent.NavigateToAppDrawer -> onScreenChange(Navigation.APP_DRAWER)
                    is UiEvent.NavigateToSettings -> onScreenChange(Navigation.SETTINGS)
                    is UiEvent.NavigateToHiddenApps -> onScreenChange(Navigation.HIDDEN_APPS)
                    is UiEvent.NavigateBack -> {
                        onScreenChange(Navigation.HOME)
                        settingsViewModel.resetUnlockState()
                    }
                    is UiEvent.StartActivityForResult -> {
                        try {
                            event.intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                            )
                            context.startActivity(event.intent)
                        } catch (e: ActivityNotFoundException) {
                            val message = "Failed to start widget configuration: ${e.localizedMessage}"
                            Log.e("Navigation", message, e)
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                    is UiEvent.ShowToast -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiEvent.NavigateToAppSelection -> {
                        currentSelectionType = event.selectionType
                        onScreenChange(Navigation.APP_DRAWER)
                    }
                    else -> Unit
                }
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.eventsFlow.collectLatest(handleEvent)
    }

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.events.collectLatest(handleEvent)
    }

    navigationContent(
        controllers = NavigationControllers(viewModel, settingsViewModel),
        state =
            NavigationState(
                currentScreen = currentScreen,
                currentSelectionType = currentSelectionType,
                onClearSelection = { currentSelectionType = null },
                onScreenChange = onScreenChange,
            ),
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun navigationContent(
    controllers: NavigationControllers,
    state: NavigationState,
) {
    AnimatedContent(
        targetState = state.currentScreen,
        transitionSpec = {
            getTransition(initialState, targetState)
        },
        contentAlignment = Alignment.Center,
    ) { screen ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Navigation.HOME ->
                    homeScreen(
                        viewModel = controllers.mainViewModel,
                        settingsViewModel = controllers.settingsViewModel,
                        onNavigateToAppDrawer = { state.onScreenChange(Navigation.APP_DRAWER) },
                        onNavigateToSettings = { state.onScreenChange(Navigation.SETTINGS) },
                    )

                Navigation.APP_DRAWER ->
                    appDrawerScreen(
                        viewModel = controllers.mainViewModel,
                        settingsViewModel = controllers.settingsViewModel,
                        onAppClick = { app ->
                            state.currentSelectionType?.let {
                                controllers.mainViewModel.selectedApp(app, it.toFlag())
                                state.onClearSelection()
                                state.onScreenChange(Navigation.SETTINGS)
                            } ?: controllers.mainViewModel.launchApp(app)
                        },
                        onSwipeDown = { state.onScreenChange(Navigation.HOME) },
                        selectionMode = state.currentSelectionType != null,
                        selectionTitle = state.currentSelectionType?.toTitle().orEmpty(),
                    )

                Navigation.SETTINGS ->
                    settingsScreen(
                        viewModel = controllers.settingsViewModel,
                        onNavigateBack = { state.onScreenChange(Navigation.HOME) },
                        onNavigateToHiddenApps = { state.onScreenChange(Navigation.HIDDEN_APPS) },
                    )

                Navigation.HIDDEN_APPS ->
                    hiddenAppsScreen(
                        viewModel = controllers.mainViewModel,
                        onNavigateBack = { state.onScreenChange(Navigation.SETTINGS) },
                    )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun getTransition(
    initial: String,
    target: String,
): ContentTransform =
    when (target) {
        Navigation.HOME -> {
            if (initial == Navigation.APP_DRAWER) {
                slideInVertically(
                    initialOffsetY = { fullSize -> -fullSize },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { fullSize -> fullSize },
                        animationSpec = tween(ANIMATION_TWEEN_VAL),
                    )
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullSize -> -fullSize },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                ) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullSize -> fullSize },
                        animationSpec = tween(ANIMATION_TWEEN_VAL),
                    )
            }
        }

        Navigation.APP_DRAWER ->
            slideInVertically(
                initialOffsetY = { fullSize -> fullSize },
                animationSpec = tween(ANIMATION_TWEEN_VAL),
            ) togetherWith
                slideOutVertically(
                    targetOffsetY = { fullSize -> -fullSize },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                )

        Navigation.SETTINGS, Navigation.HIDDEN_APPS ->
            slideInHorizontally(
                initialOffsetX = { fullSize -> fullSize },
                animationSpec = tween(ANIMATION_TWEEN_VAL),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { fullSize -> -fullSize },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                )

        else ->
            fadeIn(animationSpec = tween(ANIMATION_TWEEN_VAL)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(ANIMATION_TWEEN_VAL)) togetherWith
                fadeOut(animationSpec = tween(ANIMATION_TWEEN_VAL)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(ANIMATION_TWEEN_VAL))
    }

private fun AppSelectionType.toFlag(): Int =
    when (this) {
        AppSelectionType.SWIPE_UP_APP -> Constants.FLAG_SET_SWIPE_UP_APP
        AppSelectionType.SWIPE_DOWN_APP -> Constants.FLAG_SET_SWIPE_DOWN_APP
        AppSelectionType.SWIPE_LEFT_APP -> Constants.FLAG_SET_SWIPE_LEFT_APP
        AppSelectionType.SWIPE_RIGHT_APP -> Constants.FLAG_SET_SWIPE_RIGHT_APP
        AppSelectionType.TWOFINGER_SWIPE_UP_APP -> Constants.FLAG_SET_TWOFINGER_SWIPE_UP_APP
        AppSelectionType.TWOFINGER_SWIPE_DOWN_APP -> Constants.FLAG_SET_TWOFINGER_SWIPE_DOWN_APP
        AppSelectionType.TWOFINGER_SWIPE_LEFT_APP -> Constants.FLAG_SET_TWOFINGER_SWIPE_LEFT_APP
        AppSelectionType.TWOFINGER_SWIPE_RIGHT_APP -> Constants.FLAG_SET_TWOFINGER_SWIPE_RIGHT_APP
        AppSelectionType.ONE_TAP_APP -> Constants.FLAG_SET_ONE_TAP_APP
        AppSelectionType.DOUBLE_TAP_APP -> Constants.FLAG_SET_DOUBLE_TAP_APP
        AppSelectionType.PINCH_IN_APP -> Constants.FLAG_SET_PINCH_IN_APP
        AppSelectionType.PINCH_OUT_APP -> Constants.FLAG_SET_PINCH_OUT_APP
    }

private fun AppSelectionType.toTitle(): String =
    when (this) {
        AppSelectionType.SWIPE_UP_APP -> "Select Swipe Up Action App"
        AppSelectionType.SWIPE_DOWN_APP -> "Select Swipe Down Action App"
        AppSelectionType.SWIPE_LEFT_APP -> "Select Swipe Left App"
        AppSelectionType.SWIPE_RIGHT_APP -> "Select Swipe Right App"
        AppSelectionType.TWOFINGER_SWIPE_UP_APP -> "Select 2 fingers Swipe Up Action App"
        AppSelectionType.TWOFINGER_SWIPE_DOWN_APP -> "Select 2 fingers Swipe Down Action App"
        AppSelectionType.TWOFINGER_SWIPE_LEFT_APP -> "Select 2 fingers Swipe Left App"
        AppSelectionType.TWOFINGER_SWIPE_RIGHT_APP -> "Select 2 fingers Swipe Right App"
        AppSelectionType.ONE_TAP_APP -> "Select One Tap App"
        AppSelectionType.DOUBLE_TAP_APP -> "Select Double Tap App"
        AppSelectionType.PINCH_IN_APP -> "Select Pinch In App"
        AppSelectionType.PINCH_OUT_APP -> "Select Pinch Out App"
    }

@Composable
internal fun backHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    // Safely update the current `onBack` lambda when a new one is provided
    val currentOnBack by rememberUpdatedState(onBack)
    // Remember in Composition a back callback that calls the `onBack` lambda
    val backCallback =
        remember {
            object : OnBackPressedCallback(enabled) {
                public override fun handleOnBackPressed() {
                    currentOnBack()
                }
            }
        }
    // On every successful composition, update the callback with the `enabled` value
    SideEffect {
        backCallback.isEnabled = enabled
    }
    val backDispatcher =
        checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
            "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
        }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backDispatcher) {
        // Add callback to the backDispatcher
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        // When the effect leaves the Composition, remove the callback
        onDispose {
            backCallback.remove()
        }
    }
}
