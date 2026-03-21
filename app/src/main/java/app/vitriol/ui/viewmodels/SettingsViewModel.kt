package app.vitriol.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vitriol.data.repository.SettingsRepository
import app.vitriol.data.settings.AppSettings
import app.vitriol.ui.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository(application.applicationContext)

    // UI state for settings
    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    // Loading state
    val loading = mutableStateOf(true)

    // Events manager for UI events
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked

    private val _showLockDialog = MutableStateFlow(false)
    val showLockDialog: StateFlow<Boolean> = _showLockDialog

    private val _settingPin = MutableStateFlow(false)
    val settingPin: StateFlow<Boolean> = _settingPin

    private val _temporarilyUnlocked = MutableStateFlow(false)
    val temporarilyUnlocked: StateFlow<Boolean> = _temporarilyUnlocked

    val effectiveLockState: StateFlow<Boolean> =
        combine(_locked, _temporarilyUnlocked) { locked, tempUnlocked ->
            locked && !tempUnlocked
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // Load settings from repository
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
                loading.value = false
                _locked.value = settings.lockSettings
            }
        }
    }

    // * Update a setting by property name
    internal suspend fun updateSetting(
        propertyName: String,
        value: Any,
    ) {
        settingsRepository.updateSetting(propertyName, value)
    }

    // * Emit UI event
    internal fun emitEvent(event: UiEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    internal fun setShowLockDialog(
        show: Boolean,
        settingPin: Boolean = false,
    ) {
        _showLockDialog.value = show
        _settingPin.value = settingPin
    }

    internal fun validatePin(pin: String): Boolean {
        var isValid = false
        viewModelScope.launch {
            isValid = settingsRepository.validateSettingsPin(pin)
            if (isValid) {
                _temporarilyUnlocked.value = true
                _showLockDialog.value = false
            }
        }
        return isValid
    }

    internal fun setPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setSettingsLockPin(pin)
        }
    }

    internal fun toggleLockSettings(locked: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSettingsLock(locked)
            if (!locked) {
                // When unlocking, reset the PIN to empty
                settingsRepository.setSettingsLockPin("")
            }
        }
    }

    internal fun resetUnlockState() {
        _temporarilyUnlocked.value = false
    }
}
