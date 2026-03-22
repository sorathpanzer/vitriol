package app.vitriol

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vitriol.data.Navigation
import app.vitriol.data.repository.SettingsRepository
import app.vitriol.helper.setPlainWallpaper
import app.vitriol.ui.UiEvent
import app.vitriol.ui.viewmodels.SettingsViewModel
import app.vitriol.ui.VitriolNavigation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val APP_WIDGETHOST_ID = 1024

internal class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }
    private val appWidgetHost by lazy { AppWidgetHost(applicationContext, APP_WIDGETHOST_ID) }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        )
        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "AppWidgetHost created with ID: $APP_WIDGETHOST_ID")

        handleFirstOpen()

        setContent {
            var currentScreen by rememberSaveable { mutableStateOf(Navigation.HOME) }

            val resetFailed by viewModel.launcherResetFailed.collectAsStateWithLifecycle(initialValue = false)

            VitriolTheme {
                VitriolNavigation(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                )
            }

            LaunchedEffect(resetFailed) {
                if (resetFailed) {
                    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
            }

            LaunchedEffect(Unit) {
                viewModel.loadApps()
            }
        }

        // ✅ corrigido
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    lifecycleScope.launch { viewModel.emitEvent(UiEvent.NavigateBack) }
                }
            }
        )
    }

    private fun handleFirstOpen() {
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.firstOpen) {
                viewModel.firstOpen(false)
                settingsRepository.setFirstOpen(false)
                settingsRepository.updateSetting {
                    it.copy(firstOpenTime = System.currentTimeMillis())
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()

            if (
                settings.plainWallpaper &&
                AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ) {
                setPlainWallpaper(this@MainActivity, android.R.color.black)
                if (!isFinishing) recreate()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { viewModel.emitEvent(UiEvent.NavigateBack) }
        }
    }
}
