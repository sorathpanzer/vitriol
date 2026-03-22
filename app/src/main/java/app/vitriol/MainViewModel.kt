package app.vitriol

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vitriol.data.AppModel
import app.vitriol.data.Constants
import app.vitriol.data.repository.AppRepository
import app.vitriol.data.repository.SettingsRepository
import app.vitriol.data.settings.AppPreference
import app.vitriol.data.settings.AppSettings
import app.vitriol.helper.MyAccessibilityService
import app.vitriol.helper.getUserHandleFromString
import app.vitriol.ui.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.objecthunter.exp4j.ExpressionBuilder
import java.io.IOException

// * MainViewModel is the primary ViewModel for Vitriol app state and user interactions.
internal data class AppDrawerUiState(
    val apps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val calculatorResult: String = "",
    val showCalculatorResult: Boolean = false,
)

internal class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val settingsRepository = SettingsRepository(appContext)
    private val appRepository = AppRepository(appContext, settingsRepository)

    // Events manager for UI events
    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val eventsFlow: SharedFlow<UiEvent> = _eventsFlow.asSharedFlow()

    private val _appDrawerState = MutableStateFlow(AppDrawerUiState())
    val appDrawerState: StateFlow<AppDrawerUiState> = _appDrawerState.asStateFlow()

    // App list state
    private val _appList = MutableStateFlow<List<AppModel>>(emptyList())
    val appList: StateFlow<List<AppModel>> = _appList.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenApps: StateFlow<List<AppModel>> = _hiddenApps.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Reset launcher state
    private val _launcherResetFailed = MutableStateFlow(false)
    val launcherResetFailed: StateFlow<Boolean> = _launcherResetFailed.asStateFlow()

    init {
        // Observe app list changes
        viewModelScope.launch {
            appRepository.appList.collect { apps ->
                _appList.value = apps
                updateAppDrawerState()
            }
        }

        // Observe hidden apps changes
        viewModelScope.launch {
            appRepository.hiddenApps.collect { apps -> _hiddenApps.value = apps }
        }
    }

    private fun updateAppDrawerState() {
        _appDrawerState.value = _appDrawerState.value.copy(apps = _appList.value, loading = false)
    }

    internal fun renameApp(
        app: AppModel,
        newName: String,
    ) {
        viewModelScope.launch {
            val appKey = app.getKey()
            if (newName.isBlank() || newName == app.appLabel) {
                settingsRepository.removeAppCustomName(appKey)
            } else {
                settingsRepository.setAppCustomName(appKey, newName)
            }
            // Reload apps to reflect changes
            loadApps()
        }
    }

    /** Handle first open of the app */
    internal fun firstOpen(value: Boolean) {
        viewModelScope.launch { settingsRepository.setFirstOpen(value) }
    }

    /** Load all apps and visible apps */
    internal fun loadApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(loading = true)
                appRepository.loadApps()
            } catch (e: IOException) {
                _errorMessage.value = "Failed to load apps: ${e.message}"
                _appDrawerState.value =
                    _appDrawerState.value.copy(loading = false, error = e.message)
            }
        }
    }

    /** Load hidden apps */
    fun getHiddenApps() {
        viewModelScope.launch {
            try {
                _appDrawerState.value = _appDrawerState.value.copy(loading = true)
                appRepository.loadHiddenApps()
                _appDrawerState.value = _appDrawerState.value.copy(loading = false)
            } catch (e: IOException) {
                _errorMessage.value = "Failed to load hidden apps: ${e.message}"
                _appDrawerState.value =
                    _appDrawerState.value.copy(loading = false, error = e.message)
            }
        }
    }

    /** Toggle app hidden state */
    fun toggleAppHidden(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.toggleAppHidden(app)
            } catch (e: IOException) {
                _errorMessage.value = "Failed to toggle app visibility: ${e.message}"
            }
        }
    }

    /** Launch an app */
    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.launchApp(app)
            } catch (e: ActivityNotFoundException) {
                _errorMessage.value = "Failed to launch app: ${e.message}"
            }
        }
    }

    /** Handle app selection for various functions */
    fun selectedApp(
        appModel: AppModel,
        flag: Int,
    ) {
        when (flag) {
            Constants.FLAG_LAUNCH_APP, Constants.FLAG_HIDDEN_APPS -> {
                launchApp(appModel)
            }
            Constants.FLAG_SET_SWIPE_LEFT_APP -> {
                setSwipeLeftApp(appModel)
            }
            Constants.FLAG_SET_SWIPE_RIGHT_APP -> {
                setSwipeRightApp(appModel)
            }
            Constants.FLAG_SET_ONE_TAP_APP -> {
                setOneTapApp(appModel)
            }
            Constants.FLAG_SET_DOUBLE_TAP_APP -> {
                setDoubleTapApp(appModel)
            }
            Constants.FLAG_SET_SWIPE_UP_APP -> {
                setSwipeUpApp(appModel)
            }
            Constants.FLAG_SET_SWIPE_DOWN_APP -> {
                setSwipeDownApp(appModel)
            }
            Constants.FLAG_SET_TWOFINGER_SWIPE_UP_APP -> {
                setTwoFingerSwipeUpApp(appModel)
            }
            Constants.FLAG_SET_TWOFINGER_SWIPE_DOWN_APP -> {
                setTwoFingerSwipeDownApp(appModel)
            }
            Constants.FLAG_SET_TWOFINGER_SWIPE_LEFT_APP -> {
                setTwoFingerSwipeLeftApp(appModel)
            }
            Constants.FLAG_SET_TWOFINGER_SWIPE_RIGHT_APP -> {
                setTwoFingerSwipeRightApp(appModel)
            }
            Constants.FLAG_SET_PINCH_IN_APP -> {
                setPinchInApp(appModel)
            }
            Constants.FLAG_SET_PINCH_OUT_APP -> {
                setPinchOutApp(appModel)
            }
        }
    }

    private fun AppModel.toPreference() =
        AppPreference(
            label = appLabel,
            packageName = appPackage,
            activityClassName = activityClassName,
            userString = user.toString(),
        )

    private fun launchAppFromPreference(pref: AppPreference?) {
        pref?.takeIf { it.packageName.isNotEmpty() }?.let {
            val app =
                AppModel(
                    appLabel = pref.label,
                    key = null,
                    appPackage = pref.packageName,
                    activityClassName = pref.activityClassName,
                    user = getUserHandleFromString(appContext, pref.userString),
                )
            launchApp(app)
        }
    }

    private fun setGestureApp(
        app: AppModel,
        save: suspend (AppPreference) -> Unit,
    ) {
        viewModelScope.launch { save(app.toPreference()) }
    }

    private fun setSwipeLeftApp(app: AppModel) = setGestureApp(app, settingsRepository::setSwipeLeftApp)

    private fun setSwipeRightApp(app: AppModel) = setGestureApp(app, settingsRepository::setSwipeRightApp)

    private fun setSwipeUpApp(app: AppModel) = setGestureApp(app, settingsRepository::setSwipeUpApp)

    private fun setSwipeDownApp(app: AppModel) = setGestureApp(app, settingsRepository::setSwipeDownApp)

    private fun setTwoFingerSwipeLeftApp(app: AppModel) =
        setGestureApp(app, settingsRepository::setTwoFingerSwipeLeftApp)

    private fun setTwoFingerSwipeRightApp(app: AppModel) =
        setGestureApp(app, settingsRepository::setTwoFingerSwipeRightApp)

    private fun setTwoFingerSwipeUpApp(app: AppModel) = setGestureApp(app, settingsRepository::setTwoFingerSwipeUpApp)

    private fun setTwoFingerSwipeDownApp(app: AppModel) =
        setGestureApp(app, settingsRepository::setTwoFingerSwipeDownApp)

    private fun setOneTapApp(app: AppModel) = setGestureApp(app, settingsRepository::setOneTapApp)

    private fun setDoubleTapApp(app: AppModel) = setGestureApp(app, settingsRepository::setDoubleTapApp)

    private fun setPinchInApp(app: AppModel) = setGestureApp(app, settingsRepository::setPinchInApp)

    private fun setPinchOutApp(app: AppModel) = setGestureApp(app, settingsRepository::setPinchOutApp)

    fun launchAppFromSettings(getPref: (AppSettings) -> AppPreference?) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            launchAppFromPreference(getPref(settings))
        }
    }

    fun launchSwipeUpApp() = launchAppFromSettings { it.swipeUpApp }

    fun launchSwipeDownApp() = launchAppFromSettings { it.swipeDownApp }

    fun launchSwipeRightApp() = launchAppFromSettings { it.swipeRightApp }

    fun launchSwipeLeftApp() = launchAppFromSettings { it.swipeLeftApp }

    fun launchTwoFingerSwipeUpApp() = launchAppFromSettings { it.twoFingerSwipeUpApp }

    fun launchTwoFingerSwipeDownApp() = launchAppFromSettings { it.twoFingerSwipeDownApp }

    fun launchTwoFingerSwipeRightApp() = launchAppFromSettings { it.twoFingerSwipeRightApp }

    fun launchTwoFingerSwipeLeftApp() = launchAppFromSettings { it.twoFingerSwipeLeftApp }

    fun launchOneTapApp() = launchAppFromSettings { it.oneTapApp }

    fun launchDoubleTapApp() = launchAppFromSettings { it.doubleTapApp }

    fun launchPinchInApp() = launchAppFromSettings { it.pinchInApp }

    fun launchPinchOutApp() = launchAppFromSettings { it.pinchOutApp }

    fun lockScreen() {
        viewModelScope.launch {
            // Use accessibility service to lock screen
            val intent = Intent(appContext, MyAccessibilityService::class.java)
            intent.action = "LOCK_SCREEN"
            appContext.startService(intent)
        }
    }

    /** Search apps by query or Math evaluation */
    private object MathEvaluator {
        private const val TAG = "MathEvaluator"

        fun evaluate(expression: String): String? =
            try {
                val cleanExpr =
                    expression
                        .replace("\\s".toRegex(), "")
                        .replace("×", "*")
                        .replace("÷", "/")
                        .replace("√", "sqrt")

                val result = ExpressionBuilder(cleanExpr).build().evaluate()

                if (result % 1 == 0.0) {
                    result.toInt().toString()
                } else {
                    "%.6f".format(result).replace(Regex("\\.?0+$"), "")
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid expression: $expression", e)
                null
            }
    }

    private fun evaluateMathExpression(expression: String): String? = MathEvaluator.evaluate(expression)

    private fun isMathExpression(input: String): Boolean {
        if (input.isEmpty()) return false

        val mathRegex = Regex("^[\\d\\s+\\-*/^().√!]+$")
        return input.matches(mathRegex) &&
            input.any { it.isDigit() } &&
            // Must contain at least one digit
            input.count { it.isDigit() } >=
            input.count { it in "+-*/^" } // More numbers than operators
    }

    fun searchApps(
        query: String,
        isEnterPressed: Boolean = false,
    ) {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.settings.first()

                // Clear previous calculator result
                _appDrawerState.update { it.copy(showCalculatorResult = false) }

                // Handle calculator only when Enter is pressed on a math expression
                if (isEnterPressed && isMathExpression(query)) {
                    val result = evaluateMathExpression(query)
                    if (result != null) {
                        _appDrawerState.update {
                            it.copy(
                                calculatorResult = result,
                                showCalculatorResult = true,
                                filteredApps = emptyList(),
                            )
                        }
                        return@launch
                    }
                }

                val filteredApps =
                    appList.value.filter {
                        it.appLabel.startsWith(query, ignoreCase = true)
                    }

                _appDrawerState.update { it.copy(filteredApps = filteredApps, loading = false) }

                if (filteredApps.size == 1) {
                    launchApp(filteredApps[0])
                }
            } catch (e: IllegalArgumentException) {
                _errorMessage.value = "Search failed: ${e.message}"
            }
        }
    }

    /** Clear error message */
    fun clearError() {
        _errorMessage.value = null
        _appDrawerState.value = _appDrawerState.value.copy(error = null)
    }

    /** Emit UI event */
    fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _eventsFlow.emit(event) }
    }
}
