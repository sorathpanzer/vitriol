package app.vitriol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import app.vitriol.data.Navigation
import app.vitriol.ui.UiEvent
import app.vitriol.ui.VitriolNavigation
import app.vitriol.ui.viewmodels.SettingsViewModel
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

internal class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // 1. DEFINE THE RECEIVER HERE
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // When an app is added, removed, or changed, bypass the gatekeeper
            viewModel.forceLoadApps() 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onActivityCreated()

        // 2. REGISTER THE RECEIVER
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package") // Mandatory for package events
        }
        registerReceiver(packageReceiver, filter)

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

    // 3. UNREGISTER TO PREVENT MEMORY LEAKS
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.emitEvent(UiEvent.NavigateBack)
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
    
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }
        }
    
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
    
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Settings Lock")
            .setSubtitle("Authenticate to access launcher settings")
            // This allows them to use Fingerprint, Face, or their Lock Screen PIN/Pattern
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    
        biometricPrompt.authenticate(promptInfo)
    }
}
