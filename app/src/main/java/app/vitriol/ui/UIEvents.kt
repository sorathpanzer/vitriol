package app.vitriol.ui

import android.content.Intent
import app.vitriol.data.Constants

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

internal enum class AppSelectionType(
    val flag: Int,
    val title: String,
) {
    SWIPE_UP_APP(Constants.FLAG_SET_SWIPE_UP_APP, "Select Swipe Up Action App"),
    SWIPE_DOWN_APP(Constants.FLAG_SET_SWIPE_DOWN_APP, "Select Swipe Down Action App"),
    SWIPE_LEFT_APP(Constants.FLAG_SET_SWIPE_LEFT_APP, "Select Swipe Left App"),
    SWIPE_RIGHT_APP(Constants.FLAG_SET_SWIPE_RIGHT_APP, "Select Swipe Right App"),
    TWOFINGER_SWIPE_UP_APP(Constants.FLAG_SET_TWOFINGER_SWIPE_UP_APP, "Select 2 fingers Swipe Up Action App"),
    TWOFINGER_SWIPE_DOWN_APP(Constants.FLAG_SET_TWOFINGER_SWIPE_DOWN_APP, "Select 2 fingers Swipe Down Action App"),
    TWOFINGER_SWIPE_LEFT_APP(Constants.FLAG_SET_TWOFINGER_SWIPE_LEFT_APP, "Select 2 fingers Swipe Left App"),
    TWOFINGER_SWIPE_RIGHT_APP(Constants.FLAG_SET_TWOFINGER_SWIPE_RIGHT_APP, "Select 2 fingers Swipe Right App"),
    ONE_TAP_APP(Constants.FLAG_SET_ONE_TAP_APP, "Select One Tap App"),
    DOUBLE_TAP_APP(Constants.FLAG_SET_DOUBLE_TAP_APP, "Select Double Tap App"),
    PINCH_IN_APP(Constants.FLAG_SET_PINCH_IN_APP, "Select Pinch In App"),
    PINCH_OUT_APP(Constants.FLAG_SET_PINCH_OUT_APP, "Select Pinch Out App"),
}
