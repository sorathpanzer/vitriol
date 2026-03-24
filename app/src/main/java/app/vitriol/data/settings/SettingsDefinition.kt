package app.vitriol.data.settings

import android.view.Gravity
import app.vitriol.data.Constants
import kotlinx.serialization.Serializable
import kotlin.reflect.KProperty1

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Setting(
    val title: String,
    val description: String = "",
    val category: SettingCategory,
    val type: SettingType,
    val dependsOn: String = "",
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val options: Array<String> = [],
)

internal enum class SettingCategory(
    val label: String,
) {
    GENERAL("GENERAL"),
    APPEARANCE("APPEARANCE"),
    SWIPE_GESTURES("SWIPE GESTURES"),
    SWIPE_2FINGERS_GESTURES("2FINGERS SWIPE GESTURES"),
    PINCH_AND_TAP_GESTURES("PINCH & TAP GESTURES"),
    SYSTEM("SYSTEM"),
    ;

    override fun toString(): String = label
}

internal enum class SettingType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    BUTTON,
    APP_PICKER,
}

@Serializable
internal data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
)

internal data class AppSettings(
    val lockSettings: Boolean = false,
    val settingsLockPin: String = "",
    @Setting(
        title = "Show App Names",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE,
    )
    val showAppNames: Boolean = false,
    @Setting(
        title = "Immersive Mode",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE,
    )
    val immersiveMode: Boolean = false,
    @Setting(
        title = "Search Results Font Size",
        category = SettingCategory.APPEARANCE,
        type = SettingType.SLIDER,
        min = 0.5f,
        max = 2.0f,
        step = 0.1f,
    )
    val searchResultsFontSize: Float = 1.0f,
    @Setting(
        title = "Set Black Wallpaper",
        category = SettingCategory.APPEARANCE,
        type = SettingType.BUTTON,
        description = "Set a plain black wallpaper",
    )
    val plainWallpaper: Boolean = false,
    @Setting(
        title = "Swipe Up",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,
    val swipeUpApp: AppPreference = AppPreference(),
    @Setting(
        title = "Swipe Down",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,
    val swipeDownApp: AppPreference = AppPreference(),
    @Setting(
        title = "Swipe Right",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val swipeRightAction: Int = Constants.SwipeAction.NULL,
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "Swipe Left",
        category = SettingCategory.SWIPE_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val swipeLeftAction: Int = Constants.SwipeAction.NULL,
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "2Finger Swipe Up",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val twoFingerSwipeUpAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeUpApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "2Finger Swipe Down",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val twoFingerSwipeDownAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeDownApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "2Finger Swipe Right",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val twoFingerSwipeRightAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeRightApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "2Finger Swipe Left",
        category = SettingCategory.SWIPE_2FINGERS_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val twoFingerSwipeLeftAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeLeftApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "One Tap",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val oneTapAction: Int = Constants.SwipeAction.LOCKSCREEN,
    val oneTapApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "Double Tap",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val doubleTapAction: Int = Constants.SwipeAction.NULL,
    val doubleTapApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "Pinch In",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val pinchInAction: Int = Constants.SwipeAction.NULL,
    val pinchInApp: AppPreference = AppPreference(label = "Not set"),
    @Setting(
        title = "Pinch Out",
        category = SettingCategory.PINCH_AND_TAP_GESTURES,
        type = SettingType.DROPDOWN,
        options = ["None", "Search", "Notifications", "App", "Lockscreen"],
    )
    val pinchOutAction: Int = Constants.SwipeAction.NULL,
    val pinchOutApp: AppPreference = AppPreference(label = "Not set"),
    // Non-UI
    val firstOpen: Boolean = true,
    val firstOpenTime: Long = 0L,
    val firstSettingsOpen: Boolean = true,
    val firstHide: Boolean = true,
    val lockMode: Boolean = false,
    val keyboardMessage: Boolean = false,
    val renamedApps: Map<String, String> = mapOf(),
    val appLabelAlignment: Int = Gravity.START,
    val hiddenApps: Set<String> = emptySet(),
    val hiddenAppsUpdated: Boolean = false,
    val showHintCounter: Int = 1,
    val aboutClicked: Boolean = false,
    val rateClicked: Boolean = false,
    val shareShownTime: Long = 0L,
) {
    companion object {
        internal fun getDefault(): AppSettings = AppSettings()
    }
}

internal class SettingsManager {
    private val orderedSettingNames =
        listOf(
            "showAppNames",
            "immersiveMode",
            "swipeUpAction",
            "swipeUpApp",
            "swipeDownAction",
            "swipeDownApp",
            "swipeLeftAction",
            "swipeLeftApp",
            "swipeRightAction",
            "swipeRightApp",
            "twoFingerSwipeUpAction",
            "twoFingerSwipeUpApp",
            "twoFingerSwipeDownAction",
            "twoFingerSwipeDownApp",
            "twoFingerSwipeRightAction",
            "twoFingerSwipeRightApp",
            "twoFingerSwipeLeftAction",
            "twoFingerSwipeLeftApp",
            "oneTapAction",
            "oneTapApp",
            "doubleTapAction",
            "doubleTapApp",
            "pinchInAction",
            "pinchInApp",
            "pinchOutAction",
            "pinchOutApp",
            "plainWallpaper",
            "searchResultsFontSize",
        )

    private val allSettings: List<Pair<KProperty1<AppSettings, *>, Setting>> =
        orderedSettingNames.mapNotNull { name ->
            AppSettings::class
                .members
                .filterIsInstance<KProperty1<AppSettings, *>>()
                .find { it.name == name }
                ?.let { prop ->
                    prop.annotations.filterIsInstance<Setting>().firstOrNull()?.let { annotation ->
                        prop to annotation
                    }
                }
        }

    internal fun getSettingsByCategory(): Map<SettingCategory, List<Pair<KProperty1<AppSettings, *>, Setting>>> =
        allSettings.groupBy {
            it.second.category
        }

    private val updaters: Map<String, (AppSettings, Any) -> AppSettings> =
        mapOf(
            "showAppNames" to { s, v -> s.copy(showAppNames = v as Boolean) },
            "immersiveMode" to { s, v -> s.copy(immersiveMode = v as Boolean) },
            "searchResultsFontSize" to { s, v -> s.copy(searchResultsFontSize = v as Float) },
            "plainWallpaper" to { s, v -> s.copy(plainWallpaper = v as Boolean) },
            "swipeUpAction" to { s, v -> s.copy(swipeUpAction = v as Int) },
            "swipeUpApp" to { s, v -> s.copy(swipeUpApp = v as AppPreference) },
            "swipeDownAction" to { s, v -> s.copy(swipeDownAction = v as Int) },
            "swipeDownApp" to { s, v -> s.copy(swipeDownApp = v as AppPreference) },
            "swipeRightAction" to { s, v -> s.copy(swipeRightAction = v as Int) },
            "swipeRightApp" to { s, v -> s.copy(swipeRightApp = v as AppPreference) },
            "swipeLeftAction" to { s, v -> s.copy(swipeLeftAction = v as Int) },
            "swipeLeftApp" to { s, v -> s.copy(swipeLeftApp = v as AppPreference) },
            "twoFingerSwipeUpAction" to { s, v -> s.copy(twoFingerSwipeUpAction = v as Int) },
            "twoFingerSwipeUpApp" to { s, v -> s.copy(twoFingerSwipeUpApp = v as AppPreference) },
            "twoFingerSwipeDownAction" to { s, v -> s.copy(twoFingerSwipeDownAction = v as Int) },
            "twoFingerSwipeDownApp" to { s, v -> s.copy(twoFingerSwipeDownApp = v as AppPreference) },
            "twoFingerSwipeRightAction" to { s, v -> s.copy(twoFingerSwipeRightAction = v as Int) },
            "twoFingerSwipeRightApp" to { s, v -> s.copy(twoFingerSwipeRightApp = v as AppPreference) },
            "twoFingerSwipeLeftAction" to { s, v -> s.copy(twoFingerSwipeLeftAction = v as Int) },
            "twoFingerSwipeLeftApp" to { s, v -> s.copy(twoFingerSwipeLeftApp = v as AppPreference) },
            "oneTapAction" to { s, v -> s.copy(oneTapAction = v as Int) },
            "oneTapApp" to { s, v -> s.copy(oneTapApp = v as AppPreference) },
            "doubleTapAction" to { s, v -> s.copy(doubleTapAction = v as Int) },
            "doubleTapApp" to { s, v -> s.copy(doubleTapApp = v as AppPreference) },
            "pinchInAction" to { s, v -> s.copy(pinchInAction = v as Int) },
            "pinchInApp" to { s, v -> s.copy(pinchInApp = v as AppPreference) },
            "pinchOutAction" to { s, v -> s.copy(pinchOutAction = v as Int) },
            "pinchOutApp" to { s, v -> s.copy(pinchOutApp = v as AppPreference) },
        )

    internal fun updateSetting(
        settings: AppSettings,
        propertyName: String,
        value: Any,
    ): AppSettings = updaters[propertyName]?.invoke(settings, value) ?: settings

    internal fun isSettingEnabled(
        settings: AppSettings,
        annotation: Setting,
    ): Boolean {
        val dependsOn = annotation.dependsOn
        if (dependsOn.isEmpty()) return true

        val dependency =
            AppSettings::class
                .members
                .filterIsInstance<KProperty1<AppSettings, *>>()
                .find { it.name == dependsOn }

        return when (val value = dependency?.get(settings)) {
            is Boolean -> value
            else -> true
        }
    }
}
