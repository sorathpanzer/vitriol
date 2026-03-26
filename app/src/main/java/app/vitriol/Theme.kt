package app.vitriol

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ----------------------
// Declarar todas as cores num mapa imutável
// ----------------------
private val AppColors: Map<String, Color> by lazy {
    mapOf(
        "black" to Color(0xFF000000),
        "darkGray1" to Color(0xFF1D1D1D),
        "lightGray" to Color(0xFFE0E0E0),
        "lightBlue" to Color(0xFF90CAF9),
    )
}

// ----------------------
// Função pura que gera o esquema de cores
// ----------------------
private fun buildDarkColorScheme(colors: Map<String, Color>): ColorScheme =
    darkColorScheme(
        primary = colors.getValue("lightBlue"),
        onPrimary = colors.getValue("black"),
        onPrimaryContainer = colors.getValue("lightGray"),
        surface = colors.getValue("darkGray1"),
    )

// ----------------------
// ColorScheme lazy (só construído quando usado)
// ----------------------
private val DarkColorScheme: ColorScheme by lazy {
    buildDarkColorScheme(AppColors)
}

// ----------------------
// Composable Theme
// ----------------------
@Composable
internal fun VitriolTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
