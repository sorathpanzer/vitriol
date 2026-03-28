package app.vitriol.ui

import android.content.ActivityNotFoundException
import android.content.Context
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
import app.vitriol.ui.screens.AppDrawerScreen
import app.vitriol.ui.screens.HiddenAppsScreen
import app.vitriol.ui.screens.HomeScreen
import app.vitriol.ui.screens.SettingsScreen
import app.vitriol.ui.util.SystemUIController
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val ANIMATION_TWEEN_VAL = 300

private fun Context.startActivitySafely(intent: Intent) {
    try {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
        )
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val message = "Failed to start activity: ${e.localizedMessage}"
        Log.e("Navigation", message, e)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

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
internal fun VitriolNavigation(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    currentScreen: String,
    onScreenChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settingsState.collectAsState()
    SystemUIController(immersiveMode = settings.immersiveMode)

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
                    is UiEvent.StartActivityForResult -> context.startActivitySafely(event.intent)
                    is UiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    is UiEvent.NavigateToAppSelection -> {
                        currentSelectionType = event.selectionType
                        onScreenChange(Navigation.APP_DRAWER)
                    }
                    else -> {}
                }
            }
        }

    LaunchedEffect(viewModel, settingsViewModel) {
        launch { viewModel.eventsFlow.collectLatest(handleEvent) }
        launch { settingsViewModel.events.collectLatest(handleEvent) }
    }

    NavigationContent(
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
private fun NavigationContent(
    controllers: NavigationControllers,
    state: NavigationState,
) {
    AnimatedContent(
        targetState = state.currentScreen,
        transitionSpec = { getTransition(initialState, targetState) },
        contentAlignment = Alignment.Center,
    ) { screen ->
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Navigation.HOME ->
                    HomeScreen(
                        viewModel = controllers.mainViewModel,
                        settingsViewModel = controllers.settingsViewModel,
                        onNavigateToAppDrawer = { state.onScreenChange(Navigation.APP_DRAWER) },
                        onNavigateToSettings = { state.onScreenChange(Navigation.SETTINGS) },
                    )
                Navigation.APP_DRAWER ->
                    AppDrawerScreen(
                        viewModel = controllers.mainViewModel,
                        settingsViewModel = controllers.settingsViewModel,
                        onAppClick = { app ->
                            state.currentSelectionType?.let {
                                controllers.mainViewModel.selectedApp(app, it.flag)
                                state.onClearSelection()
                                state.onScreenChange(Navigation.SETTINGS)
                            } ?: controllers.mainViewModel.launchApp(app)
                        },
                        onSwipeDown = { state.onScreenChange(Navigation.HOME) },
                        selectionMode = state.currentSelectionType != null,
                        selectionTitle = state.currentSelectionType?.title.orEmpty(),
                    )
                Navigation.SETTINGS ->
                    SettingsScreen(
                        viewModel = controllers.settingsViewModel,
                        onNavigateBack = { state.onScreenChange(Navigation.HOME) },
                        onNavigateToHiddenApps = { state.onScreenChange(Navigation.HIDDEN_APPS) },
                    )
                Navigation.HIDDEN_APPS ->
                    HiddenAppsScreen(
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
                    initialOffsetY = { fullHeight -> -fullHeight },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                ) togetherWith
                    slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(ANIMATION_TWEEN_VAL),
                    )
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                ) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(ANIMATION_TWEEN_VAL),
                    )
            }
        }
        Navigation.APP_DRAWER ->
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(ANIMATION_TWEEN_VAL),
            ) togetherWith
                slideOutVertically(
                    targetOffsetY = { fullHeight -> -fullHeight },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                )
        Navigation.SETTINGS, Navigation.HIDDEN_APPS ->
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(ANIMATION_TWEEN_VAL),
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(ANIMATION_TWEEN_VAL),
                )
        else ->
            fadeIn(animationSpec = tween(ANIMATION_TWEEN_VAL)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(ANIMATION_TWEEN_VAL)) togetherWith
                fadeOut(animationSpec = tween(ANIMATION_TWEEN_VAL)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(ANIMATION_TWEEN_VAL))
    }

@Composable
internal fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)

    val backCallback =
        remember {
            object : OnBackPressedCallback(enabled) {
                override fun handleOnBackPressed() {
                    currentOnBack()
                }
            }
        }

    SideEffect {
        backCallback.isEnabled = enabled
    }

    val backDispatcher =
        checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
            "No OnBackPressedDispatcherOwner provided"
        }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        onDispose { backCallback.remove() }
    }
}
