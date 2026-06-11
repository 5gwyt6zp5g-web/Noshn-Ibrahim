package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class ThemeController(
    val isDark: Boolean,
    val toggleTheme: () -> Unit
)

val LocalThemeHelper = staticCompositionLocalOf<ThemeController> {
    ThemeController(isDark = false, toggleTheme = {})
}

// EduHub Premium Slate-Indigo Theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),      // Indigo 500
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81), // Indigo 900
    secondary = Color(0xFF06B6D4),    // Cyan 500
    secondaryContainer = Color(0xFF164E63), // Cyan 900
    tertiary = Color(0xFFF43F5E),     // Rose 500
    background = Color(0xFF0F172A),   // Slate 900
    onBackground = Color(0xFFF1F5F9), // Slate 100
    surface = Color(0xFF1E293B),      // Slate 800
    onSurface = Color(0xFFF1F5F9),    // Slate 100
    surfaceVariant = Color(0xFF334155), // Slate 700
    onSurfaceVariant = Color(0xFFCBD5E1), // Slate 300
    outline = Color(0xFF475569)       // Slate 600
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),      // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Indigo 100
    secondary = Color(0xFF0891B2),    // Cyan 600
    secondaryContainer = Color(0xFFECFEFF), // Cyan 50
    tertiary = Color(0xFFE11D48),     // Rose 600
    background = Color(0xFFF8FAFC),   // Slate 50
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = Color.White,
    onSurface = Color(0xFF0F172A),    // Slate 900
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF475569), // Slate 600
    outline = Color(0xFFCBD5E1)       // Slate 300
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (disabling by default to preserve custom theme branding)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
