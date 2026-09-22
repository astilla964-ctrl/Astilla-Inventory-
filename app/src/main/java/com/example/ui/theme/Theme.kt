package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = SlateDark,
    primaryContainer = TealDark,
    onPrimaryContainer = TealContainer,
    secondary = SlateLight,
    onSecondary = Color.White,
    tertiary = AccentAmber,
    background = SurfaceDark,
    surface = CardSurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = AlertRed,
    errorContainer = RedContainer,
    onErrorContainer = OnRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = SlateMedium,
    onSecondary = Color.White,
    tertiary = AccentAmber,
    background = SurfaceLight,
    surface = CardSurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = AlertRed,
    errorContainer = RedContainer,
    onErrorContainer = OnRedContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
