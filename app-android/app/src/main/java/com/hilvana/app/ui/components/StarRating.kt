package com.hilvana.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.MAX_PRIORITY

/** Dorado de las estrellas marcadas. */
val StarColor = Color(0xFFFFB300)

private val PRIORITY_NAMES = listOf("Sin importancia", "Baja", "Normal", "Media", "Alta", "Urgente")

fun priorityName(priority: Int): String = PRIORITY_NAMES[priority.coerceIn(0, MAX_PRIORITY)]

/**
 * Importancia de 1 a 5 estrellas. Tocar la estrella ya marcada la quita
 * (vuelve a "sin importancia").
 */
@Composable
fun StarRating(
    priority: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        (1..MAX_PRIORITY).forEach { star ->
            Text(
                text = if (star <= priority) "★" else "☆",
                style = MaterialTheme.typography.headlineSmall,
                color = if (star <= priority) StarColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clickable { onChange(if (star == priority) 0 else star) }
                    .padding(horizontal = 2.dp)
                    .semantics { contentDescription = "$star estrellas" },
            )
        }
        Text(
            priorityName(priority),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Estrellas compactas para las tarjetas ("★★★"). Vacío si no tiene importancia. */
fun starsText(priority: Int): String = "★".repeat(priority.coerceIn(0, MAX_PRIORITY))
