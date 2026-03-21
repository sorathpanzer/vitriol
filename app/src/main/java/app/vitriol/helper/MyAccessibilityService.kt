package app.vitriol.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.vitriol.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class MyAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    @RequiresApi(Build.VERSION_CODES.P)
    public override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == "LOCK_SCREEN") {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
        return START_STICKY
    }

    protected override fun onServiceConnected() {
        serviceScope.launch {
            val prefsDataStore = SettingsRepository(applicationContext)
            prefsDataStore.updateSetting { it.copy(lockMode = true) }
        }
        super.onServiceConnected()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    public override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } catch (_: Exception) {
            return
        }
    }

    public override fun onInterrupt() {
        // Not needed
    }
}
