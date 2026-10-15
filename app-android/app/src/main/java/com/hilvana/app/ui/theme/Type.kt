package com.hilvana.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.hilvana.app.R

/**
 * Nunito (Google Fonts, licencia OFL): redondeada y amigable. Es una fuente
 * variable, así que un solo archivo da todos los grosores.
 */
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight) = Font(
    R.font.nunito,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Nunito = FontFamily(
    nunito(FontWeight.Light),
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
    nunito(FontWeight.Bold),
    nunito(FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.nunito(weight: FontWeight) = copy(fontFamily = Nunito, fontWeight = weight)

/** Tamaños de Material 3 con Nunito: títulos con más peso para que se lean de un vistazo. */
val Typography = Typography(
    displayLarge = base.displayLarge.nunito(FontWeight.Bold),
    displayMedium = base.displayMedium.nunito(FontWeight.Bold),
    displaySmall = base.displaySmall.nunito(FontWeight.Bold),
    headlineLarge = base.headlineLarge.nunito(FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.nunito(FontWeight.Bold),
    headlineSmall = base.headlineSmall.nunito(FontWeight.Bold),
    titleLarge = base.titleLarge.nunito(FontWeight.Bold),
    titleMedium = base.titleMedium.nunito(FontWeight.Bold),
    titleSmall = base.titleSmall.nunito(FontWeight.Bold),
    bodyLarge = base.bodyLarge.nunito(FontWeight.Normal),
    bodyMedium = base.bodyMedium.nunito(FontWeight.Normal),
    bodySmall = base.bodySmall.nunito(FontWeight.Normal),
    labelLarge = base.labelLarge.nunito(FontWeight.SemiBold),
    labelMedium = base.labelMedium.nunito(FontWeight.SemiBold),
    labelSmall = base.labelSmall.nunito(FontWeight.SemiBold),
)
