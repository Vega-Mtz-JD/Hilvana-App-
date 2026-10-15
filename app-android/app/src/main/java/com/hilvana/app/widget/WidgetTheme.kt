package com.hilvana.app.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.hilvana.app.ui.theme.HilvanaDarkColors
import com.hilvana.app.ui.theme.HilvanaLightColors

private val HilvanaWidgetColors = ColorProviders(light = HilvanaLightColors, dark = HilvanaDarkColors)

/**
 * Tema de los widgets: los colores de marca de Hilvana (claro/oscuro según el
 * teléfono), o los del fondo de pantalla si así se eligió en Ajustes → Apariencia.
 */
@Composable
fun HilvanaWidgetTheme(dynamicColor: Boolean, content: @Composable () -> Unit) {
    if (dynamicColor) GlanceTheme(content = content)
    else GlanceTheme(colors = HilvanaWidgetColors, content = content)
}
