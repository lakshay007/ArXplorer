package com.lakshay.arxplorer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// Modern minimal color palette
private val md_theme_light_primary = Color(0xFF4A148C)          // Deep Purple
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_secondary = Color(0xFF6A1B9A)        // Dark Purple
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_surface = Color(0xFFF3E5F5)         // Light Purple
private val md_theme_light_surfaceVariant = Color(0xFFE1BEE7)  // Medium Purple
private val md_theme_light_onSurface = Color(0xFF4A148C)       // Deep Purple for text
private val md_theme_light_onSurfaceVariant = Color(0xFF6A1B9A) // Dark Purple for secondary text
private val md_theme_light_background = Color(0xFFF3E5F5)       // Light Purple
private val md_theme_light_cardBackground = Color(0xFFFFFFFF)   // White

// Dark theme colors with bolder accents
private val md_theme_dark_primary = Color(0xFFB4C6E0)          // Brighter slate blue
private val md_theme_dark_onPrimary = Color(0xFF1A1A1A)
private val md_theme_dark_secondary = Color(0xFF8BA3C7)        // Brighter slate
private val md_theme_dark_onSecondary = Color(0xFF1A1A1A)
private val md_theme_dark_surface = Color(0xFF121212)          // Slightly lighter black
private val md_theme_dark_surfaceVariant = Color(0xFF1E1E1E)   // Even lighter black
private val md_theme_dark_onSurface = Color(0xFFF8FAFC)        // Brighter white
private val md_theme_dark_onSurfaceVariant = Color(0xFFE2E8F0)  // Brighter muted white
private val md_theme_dark_background = Color(0xFF121212)        // Slightly lighter black
private val md_theme_dark_cardBackground = Color(0xFF1E1E1E)    // Even lighter black

object ThemeManager {
    private var _isDarkMode by mutableStateOf(false)
    val isDarkMode: Boolean get() = _isDarkMode

    fun toggleDarkMode() {
        _isDarkMode = !_isDarkMode
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode = dark
    }

    // Light theme colors
    val lightColors = AppColors(
        primary = md_theme_light_primary,
        secondary = md_theme_light_secondary,
        surface = md_theme_light_surface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurface = md_theme_light_onSurface,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        background = md_theme_light_background,
        cardBackground = md_theme_light_cardBackground,
        textPrimary = md_theme_light_onSurface,
        textSecondary = md_theme_light_onSurfaceVariant
    )

    // Dark theme colors
    val darkColors = AppColors(
        primary = md_theme_dark_primary,
        secondary = md_theme_dark_secondary,
        surface = md_theme_dark_surface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurface = md_theme_dark_onSurface,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        background = md_theme_dark_background,
        cardBackground = md_theme_dark_cardBackground,
        textPrimary = md_theme_dark_onSurface,
        textSecondary = md_theme_dark_onSurfaceVariant
    )

    val colors: AppColors
        get() = if (_isDarkMode) darkColors else lightColors
}

data class AppColors(
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val background: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalAppColors = staticCompositionLocalOf { ThemeManager.lightColors }

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    background = md_theme_light_background,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    background = md_theme_dark_background,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant
)

@Composable
fun ArXplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ThemeManager.setDarkMode(darkTheme)
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAppColors provides ThemeManager.colors) {
        MaterialTheme(
            colorScheme = colors,
            content = content
        )
    }
} 