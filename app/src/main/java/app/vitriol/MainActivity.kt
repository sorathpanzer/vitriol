package app.vitriol

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.vitriol.data.Navigation
import app.vitriol.data.repository.SettingsRepository
import app.vitriol.ui.UiEvent
import app.vitriol.ui.VitriolNavigation
import app.vitriol.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleFirstOpen()

        setContent {
            var currentScreen by rememberSaveable { mutableStateOf(Navigation.HOME) }
            val resetFailed by viewModel.launcherResetFailed.collectAsStateWithLifecycle(false)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch {
                viewModel.emitEvent(UiEvent.NavigateBack)
            }
        }
    }
}
