package app.vitriol.ui

import android.content.Intent

// * UI Events for navigation and actions
internal sealed class UiEvent {
    // Navigation
    object NavigateToAppDrawer : UiEvent()

    object NavigateToSettings : UiEvent()

    object NavigateToHiddenApps : UiEvent()

    object NavigateBack : UiEvent()

    object NavigateToWidgetPicker : UiEvent()

    // Dialogs & feedback
    data class ShowDialog(
        val dialogType: String,
    ) : UiEvent()

    data class ShowToast(
        val message: String,
    ) : UiEvent()

    data class ShowError(
        val message: String,
    ) : UiEvent()

    // Widget config
    data class LaunchWidgetBindIntent(
        val intent: Intent,
    ) : UiEvent()

    data class StartActivityForResult(
        val intent: Intent,
        val requestCode: Int,
    ) : UiEvent()

    // App selection
    data class NavigateToAppSelection(
        val selectionType: AppSelectionType,
    ) : UiEvent()

    // System
    object ResetLauncher : UiEvent()
}

internal enum class AppSelectionType {
    SWIPE_LEFT_APP,
    SWIPE_RIGHT_APP,
    ONE_TAP_APP,
    DOUBLE_TAP_APP,
    SWIPE_UP_APP,
    SWIPE_DOWN_APP,
    TWOFINGER_SWIPE_UP_APP,
    TWOFINGER_SWIPE_DOWN_APP,
    TWOFINGER_SWIPE_LEFT_APP,
    TWOFINGER_SWIPE_RIGHT_APP,
    PINCH_IN_APP,
    PINCH_OUT_APP,
}
