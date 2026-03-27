package app.vitriol.data.settings

import android.view.Gravity
import app.vitriol.data.Constants
import kotlinx.serialization.Serializable

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
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

internal enum class SettingCategory(val label: String) {
    GENERAL("GENERAL"),
    APPEARANCE("APPEARANCE"),
    SWIPE_GESTURES("SWIPE GESTURES"),
    SWIPE_2FINGERS_GESTURES("2FINGERS SWIPE GESTURES"),
    PINCH_AND_TAP_GESTURES("PINCH & TAP GESTURES"),
    SYSTEM("SYSTEM");

    override fun toString(): String = label
}

internal enum class SettingType { TOGGLE, SLIDER, DROPDOWN, BUTTON, APP_PICKER }

@Serializable
internal data class AppPreference(
    val label: String = "",
    val packageName: String = "",
    val activityClassName: String? = null,
    val userString: String = "",
)

internal data class SettingDescriptor(
    val name: String,
    val title: String,
    val description: String = "",
    val category: SettingCategory,
    val type: SettingType,
    val dependsOn: String = "",
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val options: List<String> = emptyList(),
    val valueType: SettingValueType = SettingValueType.BOOL,
)

internal enum class SettingValueType { BOOL, INT, FLOAT, APP_PREF }

internal data class AppSettings(
    val lockSettings: Boolean = false,
    val settingsLockPin: String = "",
    val showAppNames: Boolean = false,
    val immersiveMode: Boolean = false,
    val searchResultsFontSize: Float = 1.0f,
    val plainWallpaper: Boolean = false,
    val swipeUpAction: Int = Constants.SwipeAction.SEARCH,
    val swipeUpApp: AppPreference = AppPreference(),
    val swipeDownAction: Int = Constants.SwipeAction.NOTIFICATIONS,
    val swipeDownApp: AppPreference = AppPreference(),
    val swipeRightAction: Int = Constants.SwipeAction.NULL,
    val swipeRightApp: AppPreference = AppPreference(label = "Not set"),
    val swipeLeftAction: Int = Constants.SwipeAction.NULL,
    val swipeLeftApp: AppPreference = AppPreference(label = "Not set"),
    val twoFingerSwipeUpAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeUpApp: AppPreference = AppPreference(label = "Not set"),
    val twoFingerSwipeDownAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeDownApp: AppPreference = AppPreference(label = "Not set"),
    val twoFingerSwipeRightAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeRightApp: AppPreference = AppPreference(label = "Not set"),
    val twoFingerSwipeLeftAction: Int = Constants.SwipeAction.NULL,
    val twoFingerSwipeLeftApp: AppPreference = AppPreference(label = "Not set"),
    val oneTapAction: Int = Constants.SwipeAction.LOCKSCREEN,
    val oneTapApp: AppPreference = AppPreference(label = "Not set"),
    val doubleTapAction: Int = Constants.SwipeAction.NULL,
    val doubleTapApp: AppPreference = AppPreference(label = "Not set"),
    val pinchInAction: Int = Constants.SwipeAction.NULL,
    val pinchInApp: AppPreference = AppPreference(label = "Not set"),
    val pinchOutAction: Int = Constants.SwipeAction.NULL,
    val pinchOutApp: AppPreference = AppPreference(label = "Not set"),
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
    fun getValue(name: String): Any? = when (name) {
        "lockSettings" -> lockSettings
        "showAppNames" -> showAppNames
        "immersiveMode" -> immersiveMode
        "searchResultsFontSize" -> searchResultsFontSize
        "plainWallpaper" -> plainWallpaper
        "swipeUpAction" -> swipeUpAction
        "swipeUpApp" -> swipeUpApp
        "swipeDownAction" -> swipeDownAction
        "swipeDownApp" -> swipeDownApp
        "swipeRightAction" -> swipeRightAction
        "swipeRightApp" -> swipeRightApp
        "swipeLeftAction" -> swipeLeftAction
        "swipeLeftApp" -> swipeLeftApp
        "twoFingerSwipeUpAction" -> twoFingerSwipeUpAction
        "twoFingerSwipeUpApp" -> twoFingerSwipeUpApp
        "twoFingerSwipeDownAction" -> twoFingerSwipeDownAction
        "twoFingerSwipeDownApp" -> twoFingerSwipeDownApp
        "twoFingerSwipeRightAction" -> twoFingerSwipeRightAction
        "twoFingerSwipeRightApp" -> twoFingerSwipeRightApp
        "twoFingerSwipeLeftAction" -> twoFingerSwipeLeftAction
        "twoFingerSwipeLeftApp" -> twoFingerSwipeLeftApp
        "oneTapAction" -> oneTapAction
        "oneTapApp" -> oneTapApp
        "doubleTapAction" -> doubleTapAction
        "doubleTapApp" -> doubleTapApp
        "pinchInAction" -> pinchInAction
        "pinchInApp" -> pinchInApp
        "pinchOutAction" -> pinchOutAction
        "pinchOutApp" -> pinchOutApp
        else -> null
    }

    companion object {
        internal fun getDefault(): AppSettings = AppSettings()
    }
}

private val SWIPE_OPTIONS = listOf("None", "Search", "Notifications", "App", "Lockscreen")

internal class SettingsManager {

    val allDescriptors: List<SettingDescriptor> = listOf(
        SettingDescriptor("showAppNames", "Show App Names", category = SettingCategory.GENERAL, type = SettingType.TOGGLE, valueType = SettingValueType.BOOL),
        SettingDescriptor("immersiveMode", "Immersive Mode", category = SettingCategory.GENERAL, type = SettingType.TOGGLE, valueType = SettingValueType.BOOL),
        SettingDescriptor("swipeUpAction", "Swipe Up", category = SettingCategory.SWIPE_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("swipeDownAction", "Swipe Down", category = SettingCategory.SWIPE_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("swipeLeftAction", "Swipe Left", category = SettingCategory.SWIPE_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("swipeRightAction", "Swipe Right", category = SettingCategory.SWIPE_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("twoFingerSwipeUpAction", "2Finger Swipe Up", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("twoFingerSwipeDownAction", "2Finger Swipe Down", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("twoFingerSwipeRightAction", "2Finger Swipe Right", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("twoFingerSwipeLeftAction", "2Finger Swipe Left", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("oneTapAction", "One Tap", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("doubleTapAction", "Double Tap", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("pinchInAction", "Pinch In", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("pinchOutAction", "Pinch Out", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.DROPDOWN, options = SWIPE_OPTIONS, valueType = SettingValueType.INT),
        SettingDescriptor("plainWallpaper", "Set Black Wallpaper", description = "Set a plain black wallpaper", category = SettingCategory.APPEARANCE, type = SettingType.BUTTON, valueType = SettingValueType.BOOL),
        SettingDescriptor("searchResultsFontSize", "Search Results Font Size", category = SettingCategory.APPEARANCE, type = SettingType.SLIDER, min = 0.5f, max = 2.0f, step = 0.1f, valueType = SettingValueType.FLOAT),
    )

    val appPickerDescriptors: Map<String, SettingDescriptor> = mapOf(
        "swipeUpApp" to SettingDescriptor("swipeUpApp", "Swipe Up App", category = SettingCategory.SWIPE_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "swipeDownApp" to SettingDescriptor("swipeDownApp", "Swipe Down App", category = SettingCategory.SWIPE_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "swipeLeftApp" to SettingDescriptor("swipeLeftApp", "Swipe Left App", category = SettingCategory.SWIPE_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "swipeRightApp" to SettingDescriptor("swipeRightApp", "Swipe Right App", category = SettingCategory.SWIPE_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "twoFingerSwipeUpApp" to SettingDescriptor("twoFingerSwipeUpApp", "2Finger Swipe Up App", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "twoFingerSwipeDownApp" to SettingDescriptor("twoFingerSwipeDownApp", "2Finger Swipe Down App", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "twoFingerSwipeRightApp" to SettingDescriptor("twoFingerSwipeRightApp", "2Finger Swipe Right App", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "twoFingerSwipeLeftApp" to SettingDescriptor("twoFingerSwipeLeftApp", "2Finger Swipe Left App", category = SettingCategory.SWIPE_2FINGERS_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "oneTapApp" to SettingDescriptor("oneTapApp", "One Tap App", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "doubleTapApp" to SettingDescriptor("doubleTapApp", "Double Tap App", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "pinchInApp" to SettingDescriptor("pinchInApp", "Pinch In App", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
        "pinchOutApp" to SettingDescriptor("pinchOutApp", "Pinch Out App", category = SettingCategory.PINCH_AND_TAP_GESTURES, type = SettingType.APP_PICKER, valueType = SettingValueType.APP_PREF),
    )

    fun getSettingsByCategory(): Map<SettingCategory, List<SettingDescriptor>> =
        allDescriptors.groupBy { it.category }

    private val updaters: Map<String, (AppSettings, Any) -> AppSettings> = mapOf(
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

    fun updateSetting(settings: AppSettings, propertyName: String, value: Any): AppSettings =
        updaters[propertyName]?.invoke(settings, value) ?: settings

    fun isSettingEnabled(settings: AppSettings, descriptor: SettingDescriptor): Boolean {
        val dependsOn = descriptor.dependsOn
        if (dependsOn.isEmpty()) return true
        return settings.getValue(dependsOn) as? Boolean ?: true
    }
}
