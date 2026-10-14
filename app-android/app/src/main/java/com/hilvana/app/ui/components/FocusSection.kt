package com.hilvana.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.MAX_PINNED

private val NUDGE_OPTIONS = listOf(0 to "No", 1 to "Cada 1 h", 2 to "Cada 2 h", 4 to "Cada 4 h")

/**
 * 🎯 En foco: la tarea se muestra fija en notificaciones (y pantalla de bloqueo),
 * en la burbuja y en el widget, para avanzar sin abrir la app.
 */
@Composable
fun FocusSection(
    pinned: Boolean,
    canPin: Boolean,
    nudgeHours: Int,
    onPinnedChange: (Boolean) -> Unit,
    onNudgeHoursChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🎯 En foco", style = MaterialTheme.typography.labelLarge)
                Text(
                    when {
                        pinned || canPin ->
                            "Fija en tus notificaciones, la burbuja y el widget: la ves y avanzas sin abrir la app."
                        else -> "Ya tienes $MAX_PINNED tareas en foco. Termina o quita una para fijar esta."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = pinned, onCheckedChange = onPinnedChange, enabled = pinned || canPin)
        }
        if (pinned) {
            Text("🔔 Empujoncitos (para todas tus tareas en foco, nunca de 22:00 a 8:00)", style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NUDGE_OPTIONS.forEach { (hours, label) ->
                    FilterChip(
                        selected = nudgeHours == hours,
                        onClick = { onNudgeHoursChange(hours) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
