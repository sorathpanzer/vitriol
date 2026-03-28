package app.vitriol.data.repository

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import app.vitriol.data.AppModel
import app.vitriol.helper.getAppsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException

// * Repository for app-related operations
internal class AppRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()

    class AppLoadingException(
        message: String,
        cause: Throwable,
    ) : Exception(message, cause)

    // * Load all visible apps
    internal suspend fun loadApps() {
        withContext(Dispatchers.IO) {
            try {
                val settings = settingsRepository.settings.first()
                val apps =
                    getAppsList(context, settingsRepository, includeRegularApps = true, includeHiddenApps = false)
                _appList.value = apps
            } catch (e: IOException) {
                throw AppLoadingException("Failed to load apps", e)
            }
        }
    }

    internal suspend fun loadHiddenApps() {
        withContext(Dispatchers.IO) {
            try {
                val hiddenApps =
                    getAppsList(context, settingsRepository, includeRegularApps = false, includeHiddenApps = true)
                _hiddenApps.value = hiddenApps
            } catch (e: IOException) {
                throw AppLoadingException("Failed to load hidden apps", e)
            }
        }
    }

    // * Toggle app hidden state
    internal suspend fun toggleAppHidden(app: AppModel) {
        withContext(Dispatchers.IO) {
            try {
                val appKey = "${app.appPackage}/${app.user.hashCode()}"

                settingsRepository.toggleAppHidden(appKey)

                loadApps()
                loadHiddenApps()
            } catch (e: IOException) {
                println("Error toggling app hidden state: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    // * Launch an app
    internal suspend fun launchApp(appModel: AppModel) {
        withContext(Dispatchers.Main) {
            // --- 1) Check if the app is actually installed ---
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(appModel.appPackage)

            // App not installed → ignore silently
            if (launchIntent == null) {
                return@withContext
            }

            try {
                // --- 2) Try launching normally through LauncherApps ---
                val component =
                    ComponentName(
                        appModel.appPackage,
                        appModel.activityClassName ?: "",
                    )

                launcherApps.startMainActivity(
                    component,
                    appModel.user,
                    null,
                    null,
                )
            } catch (e: SecurityException) {
                // log original exception
                println("LauncherApps startMainActivity failed: ${e.message}")
                e.printStackTrace()

                // fallback to normal launch
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
    }

    // * Exception for app launch failures
    class AppLaunchException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)
}
