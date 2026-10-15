package com.hilvana.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Paleta de marca de Hilvana: verde azulado (teal) de principal y ámbar de acento.
 * Cada modo tiene sus propios tonos (no es un simple "invertir").
 */
object HilvanaColors {
    val Teal = Color(0xFF0F766E)
    val TealLight = Color(0xFF5FD4C4)
    val Amber = Color(0xFFF59E0B)
    val AmberDeep = Color(0xFFA16207)
    val Night = Color(0xFF0F1716)
    val Mist = Color(0xFFF5F8F7)
}

val HilvanaLightColors = lightColorScheme(
    primary = HilvanaColors.Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EEE6),
    onPrimaryContainer = Color(0xFF00201D),
    inversePrimary = HilvanaColors.TealLight,
    secondary = HilvanaColors.AmberDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7B3),
    onSecondaryContainer = Color(0xFF2B1B00),
    tertiary = Color(0xFF305F7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDE5F5),
    onTertiaryContainer = Color(0xFF001E2E),
    background = HilvanaColors.Mist,
    onBackground = Color(0xFF161D1C),
    surface = HilvanaColors.Mist,
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFF4F2),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3E9E7),
    surfaceContainerHighest = Color(0xFFDEE4E2),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val HilvanaDarkColors = darkColorScheme(
    primary = HilvanaColors.TealLight,
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFA6F2E7),
    inversePrimary = HilvanaColors.Teal,
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3F2A00),
    secondaryContainer = Color(0xFF5C4200),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFF9BCBE6),
    onTertiary = Color(0xFF00344A),
    tertiaryContainer = Color(0xFF164C63),
    onTertiaryContainer = Color(0xFFCDE5F5),
    background = HilvanaColors.Night,
    onBackground = Color(0xFFDDE4E2),
    surface = HilvanaColors.Night,
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFF0A1110),
    surfaceContainerLow = Color(0xFF172120),
    surfaceContainer = Color(0xFF1B2524),
    surfaceContainerHigh = Color(0xFF252F2E),
    surfaceContainerHighest = Color(0xFF303A39),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4947),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
