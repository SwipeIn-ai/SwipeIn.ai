package com.swipeapply.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Color.White,
    
    secondary = ChipTextAccent,
    onSecondary = Color.White,
    secondaryContainer = ChipBackgroundAccent,
    onSecondaryContainer = ChipTextAccent,
    
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreenLight,
    onTertiaryContainer = AccentGreen,
    
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRedLight,
    onErrorContainer = AccentRed,
    
    background = BackgroundLight,
    onBackground = TextPrimary,
    
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,
    
    outline = DividerColor,
    outlineVariant = DividerColor
)

@Composable
fun SwipeApplyTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
