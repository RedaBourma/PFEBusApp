package com.example.pfebusapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkCoralRed,
    onPrimary = White,
    primaryContainer = DeepRed,
    onPrimaryContainer = LightCoral,
    secondary = DarkOrange,
    onSecondary = White,
    secondaryContainer = DarkOrange.copy(alpha = 0.8f),
    onSecondaryContainer = White,
    tertiary = DarkBlue,
    onTertiary = White,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = MediumDarkGray,
    onSurfaceVariant = Color(0xFFDADADA),
    error = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary = CoralRed,
    onPrimary = White,
    primaryContainer = LightCoral,
    onPrimaryContainer = BurntOrange,
    secondary = DeepOrange,
    onSecondary = White,
    secondaryContainer = DeepOrange.copy(alpha = 0.15f),
    onSecondaryContainer = BurntOrange,
    tertiary = CoolBlue,
    onTertiary = White,
    background = White,
    onBackground = Color(0xFF1A1A1A),
    surface = OffWhite,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = LightGray,
    onSurfaceVariant = CoolGray,
    error = Color(0xFFD32F2F)
)

@Composable
fun PFEBusAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled by default to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Set status bar color using the new approach
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Update status bar color using WindowCompat
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            
            // Control the appearance of the status bar
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}