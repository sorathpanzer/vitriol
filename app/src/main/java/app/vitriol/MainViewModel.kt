package app.vitriol

import android.app.Application
import android.content.Intent
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AppDrawerUiState(
    val apps: List<AppModel> = emptyList(),
    val filteredApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

internal class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val settingsRepository = SettingsRepository(appContext)
    private val appRepository = AppRepository(appContext, settingsRepository)

    private val _eventsFlow = MutableSharedFlow<UiEvent>()
    val eventsFlow = _eventsFlow.asSharedFlow()

    private val _appDrawerState = MutableStateFlow(AppDrawerUiState())
    val appDrawerState = _appDrawerState.asStateFlow()

    val hiddenApps: StateFlow<List<AppModel>> =
        appRepository.hiddenApps
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val errorMessage: StateFlow<String?> =
        _appDrawerState
            .map { it.error }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _launcherResetFailed = MutableStateFlow(false)
    val launcherResetFailed = _launcherResetFailed.asStateFlow()

    init {
        viewModelScope.launch {
            appRepository.appList.collect { apps ->
                _appDrawerState.update { it.copy(apps = apps, loading = false) }
            }
        }
    }

    fun onActivityCreated() {
        handleFirstOpen()
    }

    private fun handleFirstOpen() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.firstOpen) {
                settingsRepository.setFirstOpen(false)
                settingsRepository.updateSetting {
                    it.copy(firstOpenTime = System.currentTimeMillis())
                }
            }
        }
    }

    fun loadApps() {
        // Only load if we don't have apps yet or if we are not already loading
        if (_appDrawerState.value.apps.isNotEmpty() || _appDrawerState.value.loading) {
            return 
        }
    
        viewModelScope.launch {
            _appDrawerState.update { it.copy(loading = true) }
            try {
                appRepository.loadApps()
            } catch (e: Exception) {
                _appDrawerState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    fun forceLoadApps() {
        viewModelScope.launch {
            _appDrawerState.update { it.copy(loading = true) }
            try {
                appRepository.loadApps()
            } catch (e: Exception) {
                _appDrawerState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch { appRepository.loadHiddenApps() }
    }

    fun toggleAppHidden(app: AppModel) {
        viewModelScope.launch { appRepository.toggleAppHidden(app) }
    }

    fun renameApp(
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
            forceLoadApps()
        }
    }

    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            try {
                appRepository.launchApp(app)
            } catch (e: Exception) {
                _appDrawerState.update { it.copy(error = "Launch failed") }
            }
        }
    }

    fun clearError() {
        _appDrawerState.update { it.copy(error = null) }
    }

    fun selectedApp(
        app: AppModel,
        flag: Int,
    ) {
        val saveAction: (suspend (AppPreference) -> Unit)? =
            when (flag) {
                Constants.FLAG_SET_SWIPE_LEFT_APP -> settingsRepository::setSwipeLeftApp
                Constants.FLAG_SET_SWIPE_RIGHT_APP -> settingsRepository::setSwipeRightApp
                Constants.FLAG_SET_ONE_TAP_APP -> settingsRepository::setOneTapApp
                Constants.FLAG_SET_DOUBLE_TAP_APP -> settingsRepository::setDoubleTapApp
                Constants.FLAG_SET_SWIPE_UP_APP -> settingsRepository::setSwipeUpApp
                Constants.FLAG_SET_SWIPE_DOWN_APP -> settingsRepository::setSwipeDownApp
                Constants.FLAG_SET_TWOFINGER_SWIPE_UP_APP -> settingsRepository::setTwoFingerSwipeUpApp
                Constants.FLAG_SET_TWOFINGER_SWIPE_DOWN_APP -> settingsRepository::setTwoFingerSwipeDownApp
                Constants.FLAG_SET_TWOFINGER_SWIPE_LEFT_APP -> settingsRepository::setTwoFingerSwipeLeftApp
                Constants.FLAG_SET_TWOFINGER_SWIPE_RIGHT_APP -> settingsRepository::setTwoFingerSwipeRightApp
                Constants.FLAG_SET_PINCH_IN_APP -> settingsRepository::setPinchInApp
                Constants.FLAG_SET_PINCH_OUT_APP -> settingsRepository::setPinchOutApp
                else -> null
            }

        if (saveAction != null) {
            setGestureApp(app, saveAction)
        } else {
            launchApp(app)
        }
    }

    private fun launchGesture(getPref: (AppSettings) -> AppPreference?) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val pref = getPref(settings)
            if (pref != null && pref.packageName.isNotEmpty()) {
                launchApp(
                    AppModel(
                        appLabel = pref.label,
                        key = null,
                        appPackage = pref.packageName,
                        activityClassName = pref.activityClassName,
                        user = getUserHandleFromString(appContext, pref.userString),
                    ),
                )
            }
        }
    }

    fun launchSwipeUpApp() = launchGesture { it.swipeUpApp }
    fun launchSwipeDownApp() = launchGesture { it.swipeDownApp }
    fun launchSwipeLeftApp() = launchGesture { it.swipeLeftApp }
    fun launchSwipeRightApp() = launchGesture { it.swipeRightApp }
    fun launchTwoFingerSwipeUpApp() = launchGesture { it.twoFingerSwipeUpApp }
    fun launchTwoFingerSwipeDownApp() = launchGesture { it.twoFingerSwipeDownApp }
    fun launchTwoFingerSwipeLeftApp() = launchGesture { it.twoFingerSwipeLeftApp }
    fun launchTwoFingerSwipeRightApp() = launchGesture { it.twoFingerSwipeRightApp }
    fun launchOneTapApp() = launchGesture { it.oneTapApp }
    fun launchDoubleTapApp() = launchGesture { it.doubleTapApp }
    fun launchPinchInApp() = launchGesture { it.pinchInApp }
    fun launchPinchOutApp() = launchGesture { it.pinchOutApp }

    private fun setGestureApp(
        app: AppModel,
        save: suspend (AppPreference) -> Unit,
    ) {
        viewModelScope.launch { save(app.toPreference()) }
    }

    fun searchApps(query: String) {
        viewModelScope.launch {
            val apps = _appDrawerState.value.apps
            val filtered =
                apps.filter {
                    it.appLabel.startsWith(query, ignoreCase = true)
                }

            _appDrawerState.update {
                it.copy(
                    filteredApps = filtered,
                    searchQuery = query,
                    loading = false,
                )
            }

            if (filtered.size == 1) launchApp(filtered[0])
        }
    }

    fun emitEvent(event: UiEvent) = viewModelScope.launch { _eventsFlow.emit(event) }

    fun lockScreen() {
        val intent = Intent(appContext, MyAccessibilityService::class.java).apply { action = "LOCK_SCREEN" }
        appContext.startService(intent)
    }

    private fun AppModel.toPreference() =
        AppPreference(
            label = appLabel,
            packageName = appPackage,
            activityClassName = activityClassName,
            userString = user.toString(),
        )
}
