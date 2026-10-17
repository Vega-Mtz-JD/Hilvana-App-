package com.hilvana.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.data.nextStep
import com.hilvana.app.data.progressText
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.components.IconText

/**
 * Franja "En prioridad" arriba del panel de captura: al abrir la burbuja ves tus tareas
 * en prioridad y avanzas un paso o la marcas hecha, sin salir de lo que estás haciendo.
 */
@Composable
fun FocusStrip(
    tasks: List<Item>,
    hideText: Boolean,
    onStep: (Item) -> Unit,
    onDone: (Item) -> Unit,
) {
    if (tasks.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            IconText(HilvanaIcons.Priority, "En prioridad", style = MaterialTheme.typography.labelLarge)
            if (hideText) {
                // Con la app bloqueada no se muestra el texto encima de otras apps.
                Text(
                    "${tasks.size} ${if (tasks.size == 1) "tarea" else "tareas"} · ábrelas en Hilvana",
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }
            tasks.forEach { task ->
                val next = task.checklist.nextStep
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (task.checklist.isNotEmpty()) {
                            Text(
                                task.checklist.progressText + (next?.let { " · Sigue: ${it.text}" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        if (next != null) TextButton(onClick = { onStep(task) }) { Text("Paso") }
                        TextButton(onClick = { onDone(task) }) { Text("Hecho") }
                    }
                }
            }
        }
    }
}
