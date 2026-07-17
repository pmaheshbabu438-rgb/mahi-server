package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MinimalBluePrimary,
    secondary = MinimalBluePrimary,
    tertiary = MinimalBlueContainer,
    background = DarkBgMinimal,
    surface = DarkSurfaceMinimal,
    surfaceVariant = DarkSurfaceVariantMinimal,
    onPrimary = DarkSurfaceMinimal,
    onSecondary = DarkSurfaceMinimal,
    onBackground = DarkTextMinimal,
    onSurface = DarkTextMinimal,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutlineMinimal,
    primaryContainer = MinimalBlueContainer,
    onPrimaryContainer = MinimalBlueTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalBluePrimary,
    secondary = MinimalBluePrimary,
    tertiary = MinimalBluePrimary,
    background = MinimalBgSlate,
    surface = MinimalSurfaceWhite,
    surfaceVariant = MinimalSurfaceVariant,
    onPrimary = MinimalSurfaceWhite,
    onSecondary = MinimalSurfaceWhite,
    onBackground = MinimalTextCharcoal,
    onSurface = MinimalTextCharcoal,
    onSurfaceVariant = MinimalTextMedium,
    outline = MinimalOutline,
    primaryContainer = MinimalBlueContainer,
    onPrimaryContainer = MinimalBlueTextDark
)

@Composable
fun SignalConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled default Dynamic Color to lock in our gorgeous custom branding
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
