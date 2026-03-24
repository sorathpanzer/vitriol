package app.vitriol.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import app.vitriol.data.repository.SettingsRepository
import kotlinx.coroutines.*

internal class MyAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val settingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == "LOCK_SCREEN") {
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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ⚠️ Do NOT blindly lock on every event
        // Only react to specific conditions if needed
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // جلوگیری memory leaks
    }

    // -------------------------
    // Helpers
    // -------------------------
    @RequiresApi(Build.VERSION_CODES.P)
    private fun lockScreenSafely() {
        try {
            if (!performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)) {
                println("Lock screen action failed")
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
