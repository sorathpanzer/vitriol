package app.vitriol.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.vitriol.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class MyAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val settingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_LOCK_SCREEN) {
            lockScreenSafely()
        }
        return START_NOT_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            settingsRepository.updateSetting {
                it.copy(lockMode = true)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // prevent memory leaks
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun lockScreenSafely() {
        try {
            if (!performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)) {
                Log.w(TAG, "Lock screen action failed")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while locking screen", e)
        }
    }

    companion object {
        private const val TAG = "MyAccessibilityService"
        const val ACTION_LOCK_SCREEN = "LOCK_SCREEN"
    }
}
