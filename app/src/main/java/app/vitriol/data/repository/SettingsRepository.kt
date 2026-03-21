package app.vitriol.data.repository

import android.content.Context
import android.util.Log
import android.view.Gravity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vitriol.data.Constants
import app.vitriol.data.settings.AppPreference
import app.vitriol.data.settings.AppSettings
import app.vitriol.data.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Extension property for Context to access the DataStore instance
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app.vitriol.settings",
)

// * Repository for managing application settings
internal class SettingsRepository(
    private val context: Context,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        } // Configure Json instance

    private val settingsManager = SettingsManager()

    companion object {
        // Define all preference keys
        val SHOW_APP_NAMES = booleanPreferencesKey("SHOW_APP_NAMES")
        val SHOW_HIDDEN_APPS_IN_SEARCH = booleanPreferencesKey("SHOW_HIDDEN_APPS_IN_SEARCH")
        val AUTO_OPEN_FILTERED_APP = booleanPreferencesKey("AUTO_OPEN_FILTERED_APP")
        val APP_THEME = intPreferencesKey("APP_THEME")
        val TEXT_SIZE_SCALE = floatPreferencesKey("TEXT_SIZE_SCALE")
        val FONT_WEIGHT = intPreferencesKey("FONT_WEIGHT")
        val USE_SYSTEM_FONT = booleanPreferencesKey("USE_SYSTEM_FONT")
        val ICON_CORNER_RADIUS = intPreferencesKey("ICON_CORNER_RADIUS")
        val ITEM_SPACING = intPreferencesKey("ITEM_SPACING")
        val IMMERSIVE_MODE = booleanPreferencesKey("IMMERSIVE_MODE")
        val SWIPE_DOWN_ACTION = intPreferencesKey("SWIPE_DOWN_ACTION")
        val SWIPE_UP_ACTION = intPreferencesKey("SWIPE_UP_ACTION")
        val TWOFINGER_SWIPE_DOWN_ACTION = intPreferencesKey("TWOFINGER_SWIPE_DOWN_ACTION")
        val TWOFINGER_SWIPE_UP_ACTION = intPreferencesKey("TWOFINGER_SWIPE_UP_ACTION")
        val FIRST_OPEN = booleanPreferencesKey("FIRST_OPEN")
        val FIRST_OPEN_TIME = longPreferencesKey("FIRST_OPEN_TIME")
        val FIRST_SETTINGS_OPEN = booleanPreferencesKey("FIRST_SETTINGS_OPEN")
        val FIRST_HIDE = booleanPreferencesKey("FIRST_HIDE")
        val USER_STATE = stringPreferencesKey("USER_STATE")
        val LOCK_MODE = booleanPreferencesKey("LOCK_MODE")
        val KEYBOARD_MESSAGE = booleanPreferencesKey("KEYBOARD_MESSAGE")
        val PLAIN_WALLPAPER = booleanPreferencesKey("PLAIN_WALLPAPER")
        val APP_LABEL_ALIGNMENT = intPreferencesKey("APP_LABEL_ALIGNMENT")
        val HIDDEN_APPS = stringSetPreferencesKey("HIDDEN_APPS")
        val HIDDEN_APPS_UPDATED = booleanPreferencesKey("HIDDEN_APPS_UPDATED")
        val SHOW_HINT_COUNTER = intPreferencesKey("SHOW_HINT_COUNTER")
        val ABOUT_CLICKED = booleanPreferencesKey("ABOUT_CLICKED")
        val RATE_CLICKED = booleanPreferencesKey("RATE_CLICKED")
        val SHARE_SHOWN_TIME = longPreferencesKey("SHARE_SHOWN_TIME")
        val SEARCH_RESULTS_USE_HOME_FONT = booleanPreferencesKey("SEARCH_RESULTS_USE_HOME_FONT")
        val SEARCH_RESULTS_FONT_SIZE = floatPreferencesKey("SEARCH_RESULTS_FONT_SIZE")
        val SHOW_HOME_SCREEN_ICONS = booleanPreferencesKey("SHOW_HOME_SCREEN_ICONS")
        val SCALE_HOME_APPS = booleanPreferencesKey("SCALE_HOME_APPS")
        val RENAMED_APPS_JSON = stringPreferencesKey("RENAMED_APPS_JSON")

        val HOME_APPS_JSON = stringPreferencesKey("HOME_APPS_JSON")
        val SWIPE_LEFT_APP_JSON = stringPreferencesKey("SWIPE_LEFT_APP_JSON")
        val SWIPE_RIGHT_APP_JSON = stringPreferencesKey("SWIPE_RIGHT_APP_JSON")
        val ONE_TAP_APP_JSON = stringPreferencesKey("ONE_TAP_APP_JSON")
        val DOUBLE_TAP_APP_JSON = stringPreferencesKey("DOUBLE_TAP_APP_JSON")
        val SWIPE_UP_APP_JSON = stringPreferencesKey("SWIPE_UP_APP_JSON")
        val SWIPE_DOWN_APP_JSON = stringPreferencesKey("SWIPE_DOWN_APP_JSON")
        val TWOFINGER_SWIPE_UP_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_UP_APP_JSON")
        val TWOFINGER_SWIPE_DOWN_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_DOWN_APP_JSON")
        val TWOFINGER_SWIPE_RIGHT_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_RIGHT_APP_JSON")
        val TWOFINGER_SWIPE_LEFT_APP_JSON = stringPreferencesKey("TWOFINGER_SWIPE_LEFT_APP_JSON")
        val PINCH_IN_APP_JSON = stringPreferencesKey("PINCH_IN_APP_JSON")
        val PINCH_OUT_APP_JSON = stringPreferencesKey("PINCH_OUT_APP_JSON")

        val HOME_LAYOUT = stringPreferencesKey("HOME_LAYOUT_JSON")

        val LOCK_SETTINGS = booleanPreferencesKey("LOCK_SETTINGS")
        val SETTINGS_LOCK_PIN = stringPreferencesKey("SETTINGS_LOCK_PIN")

        val SWIPE_LEFT_ACTION = intPreferencesKey("SWIPE_LEFT_ACTION")
        val SWIPE_RIGHT_ACTION = intPreferencesKey("SWIPE_RIGHT_ACTION")
        val TWOFINGER_SWIPE_LEFT_ACTION = intPreferencesKey("TWOFINGER_SWIPE_LEFT_ACTION")
        val TWOFINGER_SWIPE_RIGHT_ACTION = intPreferencesKey("TWOFINGER_SWIPE_RIGHT_ACTION")
        val ONE_TAP_ACTION = intPreferencesKey("ONE_TAP_ACTION")
        val DOUBLE_TAP_ACTION = intPreferencesKey("DOUBLE_TAP_ACTION")
        val PINCH_IN_ACTION = intPreferencesKey("PINCH_IN_ACTION")
        val PINCH_OUT_ACTION = intPreferencesKey("PINCH_OUT_ACTION")

        val HOME_SCREEN_ROWS = intPreferencesKey("HOME_SCREEN_ROWS")
        val HOME_SCREEN_COLUMNS = intPreferencesKey("HOME_SCREEN_COLUMNS")

        val SELECTED_ICON_PACK = stringPreferencesKey("SELECTED_ICON_PACK")
    }

    private val defaultAppSettings = AppSettings.getDefault()
    private val defaultSwipeLeftApp: AppPreference = defaultAppSettings.swipeLeftApp
    private val defaultSwipeRightApp: AppPreference = defaultAppSettings.swipeRightApp
    private val defaultOneTapApp: AppPreference = defaultAppSettings.oneTapApp
    private val defaultDoubleTapApp: AppPreference = defaultAppSettings.doubleTapApp
    private val defaultSwipeUpApp: AppPreference = defaultAppSettings.swipeUpApp
    private val defaultSwipeDownApp: AppPreference = defaultAppSettings.swipeDownApp
    private val defaultTwoFingerSwipeUpApp: AppPreference = defaultAppSettings.twoFingerSwipeUpApp
    private val defaultTwoFingerSwipeDownApp: AppPreference = defaultAppSettings.twoFingerSwipeDownApp
    private val defaultTwoFingerSwipeLeftApp: AppPreference = defaultAppSettings.twoFingerSwipeLeftApp
    private val defaultTwoFingerSwipeRightApp: AppPreference = defaultAppSettings.twoFingerSwipeRightApp
    private val defaultPinchInApp: AppPreference = defaultAppSettings.pinchInApp
    private val defaultPinchOutApp: AppPreference = defaultAppSettings.pinchOutApp

    // * Flow of settings that emits whenever any setting changes

    inline fun <reified T> getJsonPref(
        prefs: Preferences,
        key: Preferences.Key<String>,
        default: T,
    ): T =
        prefs[key]?.let {
            json.decodeFromStringCatching(it, default)
        } ?: default

    val settings: Flow<AppSettings> =
        context.settingsDataStore.data.map { prefs ->

            val swipeLeftApp = getJsonPref(prefs, SWIPE_LEFT_APP_JSON, defaultSwipeLeftApp)
            val swipeRightApp = getJsonPref(prefs, SWIPE_RIGHT_APP_JSON, defaultSwipeRightApp)
            val twoFingerSwipeLeftApp = getJsonPref(prefs, TWOFINGER_SWIPE_LEFT_APP_JSON, defaultTwoFingerSwipeLeftApp)
            val twoFingerSwipeRightApp =
                getJsonPref(prefs, TWOFINGER_SWIPE_RIGHT_APP_JSON, defaultTwoFingerSwipeRightApp)
            val oneTapApp = getJsonPref(prefs, ONE_TAP_APP_JSON, defaultOneTapApp)
            val doubleTapApp = getJsonPref(prefs, DOUBLE_TAP_APP_JSON, defaultDoubleTapApp)
            val swipeUpApp = getJsonPref(prefs, SWIPE_UP_APP_JSON, defaultSwipeUpApp)
            val swipeDownApp = getJsonPref(prefs, SWIPE_DOWN_APP_JSON, defaultSwipeDownApp)
            val twoFingerSwipeUpApp = getJsonPref(prefs, TWOFINGER_SWIPE_UP_APP_JSON, defaultTwoFingerSwipeUpApp)
            val twoFingerSwipeDownApp = getJsonPref(prefs, TWOFINGER_SWIPE_DOWN_APP_JSON, defaultTwoFingerSwipeDownApp)
            val pinchInApp = getJsonPref(prefs, PINCH_IN_APP_JSON, defaultPinchInApp)
            val pinchOutApp = getJsonPref(prefs, PINCH_OUT_APP_JSON, defaultPinchOutApp)

            val renamedApps =
                prefs[RENAMED_APPS_JSON]?.let {
                    try {
                        json.decodeFromString<Map<String, String>>(it)
                    } catch (e: SerializationException) {
                        Log.e("SettingsRepo", "Failed to decode renamed apps JSON: ${e.message}")
                        mapOf<String, String>()
                    }
                } ?: mapOf()

            AppSettings(
                // General settings
                showAppNames = prefs[SHOW_APP_NAMES] ?: false,
                showHiddenAppsOnSearch = prefs[SHOW_HIDDEN_APPS_IN_SEARCH] ?: false,
                // Layout settings
                immersiveMode = prefs[IMMERSIVE_MODE] ?: false,
                // Gestures settings
                swipeDownAction = prefs[SWIPE_DOWN_ACTION] ?: Constants.SwipeAction.NOTIFICATIONS,
                swipeUpAction = prefs[SWIPE_UP_ACTION] ?: Constants.SwipeAction.SEARCH,
                twoFingerSwipeDownAction = prefs[TWOFINGER_SWIPE_DOWN_ACTION] ?: Constants.SwipeAction.NULL,
                twoFingerSwipeUpAction = prefs[TWOFINGER_SWIPE_UP_ACTION] ?: Constants.SwipeAction.NULL,
                twoFingerSwipeRightAction = prefs[TWOFINGER_SWIPE_RIGHT_ACTION] ?: Constants.SwipeAction.NULL,
                twoFingerSwipeLeftAction = prefs[TWOFINGER_SWIPE_LEFT_ACTION] ?: Constants.SwipeAction.NULL,
                swipeLeftAction = prefs[SWIPE_LEFT_ACTION] ?: Constants.SwipeAction.NULL,
                swipeRightAction = prefs[SWIPE_RIGHT_ACTION] ?: Constants.SwipeAction.NULL,
                oneTapAction = prefs[ONE_TAP_ACTION] ?: Constants.SwipeAction.LOCKSCREEN,
                doubleTapAction = prefs[DOUBLE_TAP_ACTION] ?: Constants.SwipeAction.NULL,
                pinchInAction = prefs[PINCH_IN_ACTION] ?: Constants.SwipeAction.NULL,
                pinchOutAction = prefs[PINCH_OUT_ACTION] ?: Constants.SwipeAction.NULL,
                lockSettings = prefs[LOCK_SETTINGS] ?: false,
                settingsLockPin = prefs[SETTINGS_LOCK_PIN] ?: "",
                // Other properties
                firstOpen = prefs[FIRST_OPEN] ?: true,
                firstOpenTime = prefs[FIRST_OPEN_TIME] ?: 0L,
                firstSettingsOpen = prefs[FIRST_SETTINGS_OPEN] ?: true,
                firstHide = prefs[FIRST_HIDE] ?: true,
                lockMode = prefs[LOCK_MODE] ?: false,
                keyboardMessage = prefs[KEYBOARD_MESSAGE] ?: false,
                plainWallpaper = prefs[PLAIN_WALLPAPER] ?: false,
                appLabelAlignment = prefs[APP_LABEL_ALIGNMENT] ?: Gravity.START,
                hiddenApps = prefs[HIDDEN_APPS] ?: emptySet(),
                hiddenAppsUpdated = prefs[HIDDEN_APPS_UPDATED] ?: false,
                showHintCounter = prefs[SHOW_HINT_COUNTER] ?: 1,
                aboutClicked = prefs[ABOUT_CLICKED] ?: false,
                searchResultsFontSize = prefs[SEARCH_RESULTS_FONT_SIZE] ?: 1.0f,
                swipeLeftApp = swipeLeftApp,
                swipeRightApp = swipeRightApp,
                oneTapApp = oneTapApp,
                doubleTapApp = doubleTapApp,
                swipeUpApp = swipeUpApp,
                swipeDownApp = swipeDownApp,
                twoFingerSwipeUpApp = twoFingerSwipeUpApp,
                twoFingerSwipeDownApp = twoFingerSwipeDownApp,
                twoFingerSwipeLeftApp = twoFingerSwipeLeftApp,
                twoFingerSwipeRightApp = twoFingerSwipeRightApp,
                pinchInApp = pinchInApp,
                pinchOutApp = pinchOutApp,
                renamedApps = renamedApps,
            )
        }

    private inline fun <reified T> Json.decodeFromStringCatching(
        jsonString: String,
        default: T,
    ): T =
        try {
            this.decodeFromString<T>(jsonString)
        } catch (e: SerializationException) {
            Log.e("SettingsRepo", "Failed to decode JSON for ${T::class.simpleName}: ${e.message}. Using default.")
            default
        }

    private val settingUpdaters: Map<String, suspend (MutablePreferences, Any) -> Unit> =
        mapOf(
            // General
            "showAppNames" to { prefs, v -> prefs[SHOW_APP_NAMES] = v as Boolean },
            "showHiddenAppsOnSearch" to { prefs, v -> prefs[SHOW_HIDDEN_APPS_IN_SEARCH] = v as Boolean },
            // Appearance
            "immersiveMode" to { prefs, v -> prefs[IMMERSIVE_MODE] = v as Boolean },
            "showHomeScreenIcons" to { prefs, v -> prefs[SHOW_HOME_SCREEN_ICONS] = v as Boolean },
            "plainWallpaper" to { prefs, v -> prefs[PLAIN_WALLPAPER] = v as Boolean },
            "searchResultsFontSize" to { prefs, v -> prefs[SEARCH_RESULTS_FONT_SIZE] = v as Float },
            // Gestures
            "swipeDownAction" to { prefs, v -> prefs[SWIPE_DOWN_ACTION] = v as Int },
            "swipeUpAction" to { prefs, v -> prefs[SWIPE_UP_ACTION] = v as Int },
            "twoFingerSwipeDownAction" to { prefs, v -> prefs[TWOFINGER_SWIPE_DOWN_ACTION] = v as Int },
            "twoFingerSwipeUpAction" to { prefs, v -> prefs[TWOFINGER_SWIPE_UP_ACTION] = v as Int },
            "twoFingerSwipeRightAction" to { prefs, v -> prefs[TWOFINGER_SWIPE_RIGHT_ACTION] = v as Int },
            "twoFingerSwipeLeftAction" to { prefs, v -> prefs[TWOFINGER_SWIPE_LEFT_ACTION] = v as Int },
            "swipeLeftAction" to { prefs, v -> prefs[SWIPE_LEFT_ACTION] = v as Int },
            "swipeRightAction" to { prefs, v -> prefs[SWIPE_RIGHT_ACTION] = v as Int },
            "oneTapAction" to { prefs, v -> prefs[ONE_TAP_ACTION] = v as Int },
            "doubleTapAction" to { prefs, v -> prefs[DOUBLE_TAP_ACTION] = v as Int },
            "pinchInAction" to { prefs, v -> prefs[PINCH_IN_ACTION] = v as Int },
            "pinchOutAction" to { prefs, v -> prefs[PINCH_OUT_ACTION] = v as Int },
            // Lockscreen
            "lockSettings" to { prefs, v -> prefs[LOCK_SETTINGS] = v as Boolean },
            "settingsLockPin" to { prefs, v -> prefs[SETTINGS_LOCK_PIN] = v as String },
            // First-launch flags
            "firstOpen" to { prefs, v -> prefs[FIRST_OPEN] = v as Boolean },
            "firstOpenTime" to { prefs, v -> prefs[FIRST_OPEN_TIME] = v as Long },
            "firstSettingsOpen" to { prefs, v -> prefs[FIRST_SETTINGS_OPEN] = v as Boolean },
            "firstHide" to { prefs, v -> prefs[FIRST_HIDE] = v as Boolean },
            // Misc
            "lockMode" to { prefs, v -> prefs[LOCK_MODE] = v as Boolean },
            "keyboardMessage" to { prefs, v -> prefs[KEYBOARD_MESSAGE] = v as Boolean },
            "appLabelAlignment" to { prefs, v -> prefs[APP_LABEL_ALIGNMENT] = v as Int },
            "hiddenAppsUpdated" to { prefs, v -> prefs[HIDDEN_APPS_UPDATED] = v as Boolean },
            "showHintCounter" to { prefs, v -> prefs[SHOW_HINT_COUNTER] = v as Int },
            "aboutClicked" to { prefs, v -> prefs[ABOUT_CLICKED] = v as Boolean },
            "hiddenApps" to { prefs, v ->
                if (v is Set<*>) {
                    prefs[HIDDEN_APPS] = v.filterIsInstance<String>().toSet()
                } else {
                    error("Expected Set<String>, got ${v::class.simpleName}")
                }
            },
            // AppPreference (JSON-encoded)
            "swipeLeftApp" to { prefs, v -> prefs[SWIPE_LEFT_APP_JSON] = json.encodeToString(v) },
            "swipeRightApp" to { prefs, v -> prefs[SWIPE_RIGHT_APP_JSON] = json.encodeToString(v) },
            "oneTapApp" to { prefs, v -> prefs[ONE_TAP_APP_JSON] = json.encodeToString(v) },
            "doubleTapApp" to { prefs, v -> prefs[DOUBLE_TAP_APP_JSON] = json.encodeToString(v) },
            "swipeUpApp" to { prefs, v -> prefs[SWIPE_UP_APP_JSON] = json.encodeToString(v) },
            "swipeDownApp" to { prefs, v -> prefs[SWIPE_DOWN_APP_JSON] = json.encodeToString(v) },
            "twoFingerSwipeUpApp" to { prefs, v -> prefs[TWOFINGER_SWIPE_UP_APP_JSON] = json.encodeToString(v) },
            "twoFingerSwipeDownApp" to { prefs, v -> prefs[TWOFINGER_SWIPE_DOWN_APP_JSON] = json.encodeToString(v) },
            "twoFingerSwipeLeftApp" to { prefs, v -> prefs[TWOFINGER_SWIPE_LEFT_APP_JSON] = json.encodeToString(v) },
            "twoFingerSwipeRightApp" to { prefs, v -> prefs[TWOFINGER_SWIPE_RIGHT_APP_JSON] = json.encodeToString(v) },
            "pinchInApp" to { prefs, v -> prefs[PINCH_IN_APP_JSON] = json.encodeToString(v) },
            "pinchOutApp" to { prefs, v -> prefs[PINCH_OUT_APP_JSON] = json.encodeToString(v) },
            "renamedApps" to { prefs, v -> prefs[RENAMED_APPS_JSON] = json.encodeToString(v) },
        )

    private suspend fun applyChangedFields(
        current: AppSettings,
        updated: AppSettings,
        prefs: MutablePreferences,
    ) {
        AppSettings::class.java.declaredFields.forEach { field ->
            field.isAccessible = true
            val name = field.name
            val oldValue = field.get(current)
            val newValue = field.get(updated)

            if (oldValue != newValue) {
                newValue?.let { settingUpdaters[name]?.invoke(prefs, it) }
            }
        }
    }

    internal suspend fun updateSetting(update: (AppSettings) -> AppSettings) {
        val currentSettings = settings.first()
        val updatedSettings = update(currentSettings)

        context.settingsDataStore.edit { prefs ->
            applyChangedFields(currentSettings, updatedSettings, prefs)
        }
    }

    // * Update a setting by property name
    internal suspend fun updateSetting(
        propertyName: String,
        value: Any,
    ) {
        val currentSettings = settings.first()
        val updatedSettings = settingsManager.updateSetting(currentSettings, propertyName, value)
        updateSetting { updatedSettings }
    }

    // * Methods for managing other settable apps
    internal suspend fun setGestureApp(
        key: Preferences.Key<String>,
        app: AppPreference,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(app)
        }
    }

    suspend fun setSwipeLeftApp(app: AppPreference) = setGestureApp(SWIPE_LEFT_APP_JSON, app)

    suspend fun setSwipeRightApp(app: AppPreference) = setGestureApp(SWIPE_RIGHT_APP_JSON, app)

    suspend fun setSwipeUpApp(app: AppPreference) = setGestureApp(SWIPE_UP_APP_JSON, app)

    suspend fun setSwipeDownApp(app: AppPreference) = setGestureApp(SWIPE_DOWN_APP_JSON, app)

    suspend fun setTwoFingerSwipeLeftApp(app: AppPreference) = setGestureApp(TWOFINGER_SWIPE_LEFT_APP_JSON, app)

    suspend fun setTwoFingerSwipeRightApp(app: AppPreference) = setGestureApp(TWOFINGER_SWIPE_RIGHT_APP_JSON, app)

    suspend fun setTwoFingerSwipeUpApp(app: AppPreference) = setGestureApp(TWOFINGER_SWIPE_UP_APP_JSON, app)

    suspend fun setTwoFingerSwipeDownApp(app: AppPreference) = setGestureApp(TWOFINGER_SWIPE_DOWN_APP_JSON, app)

    suspend fun setOneTapApp(app: AppPreference) = setGestureApp(ONE_TAP_APP_JSON, app)

    suspend fun setDoubleTapApp(app: AppPreference) = setGestureApp(DOUBLE_TAP_APP_JSON, app)

    suspend fun setPinchInApp(app: AppPreference) = setGestureApp(PINCH_IN_APP_JSON, app)

    suspend fun setPinchOutApp(app: AppPreference) = setGestureApp(PINCH_OUT_APP_JSON, app)

    internal suspend fun setSettingsLock(locked: Boolean) {
        updateSetting { it.copy(lockSettings = locked) }
    }

    internal suspend fun setSettingsLockPin(pin: String) {
        updateSetting { it.copy(settingsLockPin = pin) }
    }

    internal suspend fun validateSettingsPin(pin: String): Boolean = settings.first().settingsLockPin == pin

    // * Methods for managing hidden apps
    internal suspend fun toggleAppHidden(packageKey: String) {
        updateSetting {
            val updatedHiddenApps = it.hiddenApps.toMutableSet()
            if (updatedHiddenApps.contains(packageKey)) {
                updatedHiddenApps.remove(packageKey)
            } else {
                updatedHiddenApps.add(packageKey)
            }
            it.copy(hiddenApps = updatedHiddenApps)
        }
    }

    internal suspend fun setFirstOpen(value: Boolean) {
        updateSetting { it.copy(firstOpen = value) }
    }

    internal suspend fun setAppCustomName(
        appKey: String,
        customName: String,
    ) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()

        if (customName.isBlank()) {
            updatedRenamedApps.remove(appKey)
        } else {
            updatedRenamedApps[appKey] = customName
        }

        context.settingsDataStore.edit { prefs ->
            prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps)
        }
    }

    internal suspend fun removeAppCustomName(appKey: String) {
        val currentSettings = settings.first()
        val updatedRenamedApps = currentSettings.renamedApps.toMutableMap()
        updatedRenamedApps.remove(appKey)

        context.settingsDataStore.edit { prefs ->
            prefs[RENAMED_APPS_JSON] = json.encodeToString(updatedRenamedApps)
        }
    }
}
