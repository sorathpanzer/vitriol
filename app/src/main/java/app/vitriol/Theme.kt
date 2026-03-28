package app.vitriol

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private object ColorValues {
    const val BLACK = 0xFF000000
    const val DARK_GRAY1 = 0xFF1D1D1D
    const val LIGHT_GRAY = 0xFFE0E0E0
    const val LIGHT_BLUE = 0xFF90CAF9
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(ColorValues.LIGHT_BLUE),
    onPrimary = Color(ColorValues.BLACK),
    onPrimaryContainer = Color(ColorValues.LIGHT_GRAY),
    surface = Color(ColorValues.DARK_GRAY1),
)

@Composable
internal fun VitriolTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat
                    .getInsetsController(window, view)
                    .isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
