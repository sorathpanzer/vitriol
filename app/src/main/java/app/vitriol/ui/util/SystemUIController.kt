package app.vitriol.ui.util

import android.app.Activity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private class AppLoadingException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)

// * Controls system Immersive Mode
@Composable
internal fun systemUIController(immersiveMode: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = remember { (context as? Activity)?.window }

    DisposableEffect(immersiveMode) {
        if (window != null) {
            if (immersiveMode) {
                enableImmersiveMode(window, view)
            } else {
                disableImmersiveMode(window, view)
            }
        }

        onDispose { }
    }
}

private fun disableImmersiveMode(
    window: android.view.Window,
    view: View,
) {
    try {
        val controller = WindowInsetsControllerCompat(window, view)

        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        WindowCompat.setDecorFitsSystemWindows(window, false)

        controller.show(WindowInsetsCompat.Type.navigationBars())
    } catch (e: IllegalStateException) {
        throw AppLoadingException("Failed to disable Immersive Mode", e)
    }
}

private fun enableImmersiveMode(
    window: android.view.Window,
    view: View,
) {
    try {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, view)

        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } catch (e: IllegalStateException) {
        throw AppLoadingException("Failed to enable Immersive Mode", e)
    }
}
