package com.hilvana.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.UserPrefs

/** Esquinas redondeadas y suaves, a juego con Nunito. */
val HilvanaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Tema elegido en Ajustes → Apariencia. */
enum class ThemeMode(val label: String) { SYSTEM("Como el sistema"), LIGHT("Claro"), DARK("Oscuro") }

/**
 * Tema de Hilvana. Por defecto lee lo elegido en Ajustes (modo y colores); la
 * pantalla principal lo pasa explícito para que el cambio se vea al instante.
 */
@Composable
fun HilvanaTheme(
    themeMode: ThemeMode? = null,
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val prefs = UserPrefs(context)
    val mode = themeMode ?: prefs.themeMode
    val useDynamic = (dynamicColor ?: prefs.dynamicColor) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        dark -> HilvanaDarkColors
        else -> HilvanaLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = HilvanaShapes,
        content = content,
    )
}
