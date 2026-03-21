package app.vitriol.helper

import android.app.SearchManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.core.graphics.createBitmap
import app.vitriol.data.AppModel
import app.vitriol.data.Constants
import app.vitriol.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.Collator

private const val WALLPAPER_WIDTH = 1000
private const val WALLPAPER_HEIGHT = 2000

private class AppLoadingException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)

internal suspend fun getAppsList(
    context: Context,
    settingsRepository: SettingsRepository,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
): MutableList<AppModel> =
    withContext(Dispatchers.IO) {
        val appList: MutableList<AppModel> = mutableListOf()

        try {
            val settings = settingsRepository.settings.first()
            val hiddenApps = settings.hiddenApps
            val renamedApps = settings.renamedApps

            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()

            for (profile in userManager.userProfiles) {
                for (app in launcherApps.getActivityList(null, profile)) {
                    if (app.applicationInfo.packageName == context.packageName) continue

                    val tempAppModel =
                        AppModel(
                            "temp",
                            null,
                            app.applicationInfo.packageName,
                            app.componentName.className,
                            false,
                            profile,
                            null,
                            false,
                            profile.toString(),
                        )
                    val appKey = tempAppModel.getKey()

                    val defaultLabel =
                        app.label.toString() +
                            if (profile != android.os.Process.myUserHandle()) " (Clone)" else ""

                    val appLabelShown = renamedApps[appKey] ?: defaultLabel

                    val appModel =
                        AppModel(
                            appLabelShown,
                            collator.getCollationKey(app.label.toString()),
                            app.applicationInfo.packageName,
                            app.componentName.className,
                            (System.currentTimeMillis() - app.firstInstallTime) < Constants.ONE_HOUR_IN_MILLIS,
                            profile,
                        )

                    val dupAppKey = "${app.applicationInfo.packageName}/${profile.hashCode()}"

                    val hidden = hiddenApps.contains(dupAppKey)

                    if (hidden) {
                        if (includeHiddenApps) {
                            appList.add(appModel.copy(isHidden = true))
                        }
                    } else {
                        if (includeRegularApps) {
                            appList.add(appModel)
                        }
                    }
                }
            }
            appList.sortBy { it.appLabel.lowercase() }
        } catch (e: IOException) {
            println("Error loading apps: ${e.message}")
            e.printStackTrace()
        }
        appList
    }

internal fun getUserHandleFromString(
    context: Context,
    userHandleString: String,
): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return android.os.Process.myUserHandle()
}

internal fun isVitriolDefault(context: Context): Boolean {
    val launcherPackageName = getDefaultLauncherPackage(context)
    return context.packageName == launcherPackageName
}

private fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent()
    intent.action = Intent.ACTION_MAIN
    intent.addCategory(Intent.CATEGORY_HOME)
    val packageManager = context.packageManager
    val result = packageManager.resolveActivity(intent, 0)
    return if (result?.activityInfo != null) {
        result.activityInfo.packageName
    } else {
        "android"
    }
}

internal fun setPlainWallpaper(
    context: Context,
    color: Int,
) {
    try {
        val bitmap = createBitmap(WALLPAPER_WIDTH, WALLPAPER_HEIGHT)
        bitmap.eraseColor(context.getColor(color))
        val manager = WallpaperManager.getInstance(context)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM)
        manager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK)
    } catch (e: IOException) {
        throw AppLoadingException("Failed to set wallpaper", e)
    }
}

internal fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}

internal fun expandNotificationDrawer(context: Context) {
    try {
        // Fall back -> reflection for older versions
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (_: Exception) {
        // If all else fails, try to use the notification intent
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            context.startActivity(intent)
        } catch (e2: ActivityNotFoundException) {
            e2.printStackTrace()
        }
    }
}

internal fun isAccessServiceEnabled(context: Context): Boolean {
    val enabled =
        try {
            Settings.Secure.getInt(context.applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: SettingNotFoundException) {
            throw AppLoadingException("Failed to enable service", e)
            0
        }
    if (enabled == 1) {
        val enabledServicesString: String? =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        return enabledServicesString?.contains(context.packageName + "/" + MyAccessibilityService::class.java.name)
            ?: false
    }
    return false
}
