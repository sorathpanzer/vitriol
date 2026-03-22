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
        "darkGray0" to Color(0xFF121212),
        "darkGray1" to Color(0xFF1D1D1D),
        "darkGray2" to Color(0xFF1E1E1E),
        "darkGray3" to Color(0xFF2E2E2E),
        "darkGray4" to Color(0xFF2D2D2D),
        "lightGray" to Color(0xFFE0E0E0),
        "mediumGray" to Color(0xFFCCCCCC),
        "lightGreen" to Color(0xFF81C784),
        "orange" to Color(0xFFFFB74D),
        "lightBlue" to Color(0xFF90CAF9),
        "pinkRed" to Color(0xFFCF6679),
    )
}

// ----------------------
// Função pura que gera o esquema de cores
// ----------------------
private fun buildDarkColorScheme(colors: Map<String, Color>): ColorScheme =
    darkColorScheme(
        primary = colors.getValue("lightBlue"),
        onPrimary = colors.getValue("black"),
        primaryContainer = colors.getValue("darkGray2"),
        onPrimaryContainer = colors.getValue("lightGray"),
        secondary = colors.getValue("lightGreen"),
        onSecondary = colors.getValue("black"),
        secondaryContainer = colors.getValue("darkGray3"),
        onSecondaryContainer = colors.getValue("lightGray"),
        tertiary = colors.getValue("orange"),
        onTertiary = colors.getValue("black"),
        background = colors.getValue("darkGray0"),
        onBackground = colors.getValue("lightGray"),
        surface = colors.getValue("darkGray1"),
        onSurface = colors.getValue("lightGray"),
        surfaceVariant = colors.getValue("darkGray4"),
        onSurfaceVariant = colors.getValue("mediumGray"),
        error = colors.getValue("pinkRed"),
        onError = colors.getValue("black"),
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
