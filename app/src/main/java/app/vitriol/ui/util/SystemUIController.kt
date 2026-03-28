package app.vitriol.ui.util

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
internal fun SystemUIController(immersiveMode: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    DisposableEffect(immersiveMode, window, view) {
        if (window != null) {
            try {
                val controller = WindowInsetsControllerCompat(window, view)

                WindowCompat.setDecorFitsSystemWindows(window, false)

                if (immersiveMode) {
                    controller.hide(
                        WindowInsetsCompat.Type.statusBars() or
                            WindowInsetsCompat.Type.navigationBars(),
                    )
                } else {
                    controller.show(
                        WindowInsetsCompat.Type.statusBars() or
                            WindowInsetsCompat.Type.navigationBars(),
                    )
                }

                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } catch (e: SecurityException) {
                Log.e("SystemUIController", "Failed to update system UI", e)
            }
        }

        onDispose { }
    }
}
