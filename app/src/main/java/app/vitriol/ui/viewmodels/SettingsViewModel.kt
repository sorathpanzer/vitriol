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

    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    val loading = mutableStateOf(true)

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked

    private val _temporarilyUnlocked = MutableStateFlow(false)
    val temporarilyUnlocked: StateFlow<Boolean> = _temporarilyUnlocked

    val effectiveLockState: StateFlow<Boolean> =
        combine(_locked, _temporarilyUnlocked) { locked, tempUnlocked ->
            locked && !tempUnlocked
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _settingsState.value = settings
                loading.value = false
                if (!_temporarilyUnlocked.value) {
                    _locked.value = settings.lockSettings
                } else {
                    _locked.value = settings.lockSettings
                }
            }
        }
    }

    internal suspend fun updateSetting(
        propertyName: String,
        value: Any,
    ) {
        settingsRepository.updateSetting(propertyName, value)
    }

    internal fun emitEvent(event: UiEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    internal fun setUnlocked(isUnlocked: Boolean) {
        _temporarilyUnlocked.value = isUnlocked
    }

    internal fun toggleLockSettings(locked: Boolean) {
        viewModelScope.launch {
            if (locked) {
                _temporarilyUnlocked.value = true
            } else {
                _temporarilyUnlocked.value = false
            }
            settingsRepository.setSettingsLock(locked)
        }
    }

    internal fun resetUnlockState() {
        _temporarilyUnlocked.value = false
    }
}
