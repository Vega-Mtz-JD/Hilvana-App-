package com.hilvana.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 🎯 Meta de hoy: cuántas cosas completaste de tu meta diaria, con barra de avance
 * y la racha. Tocarla abre el selector de meta.
 */
@Composable
fun DailyGoalCard(
    goal: Int,
    completedToday: Int,
    streak: Int,
    onGoalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosing by remember { mutableStateOf(false) }
    val reached = goal > 0 && completedToday >= goal

    Card(
        onClick = { choosing = true },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reached) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (goal == 0) {
                Text("🎯 Sin meta diaria", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Hoy completaste $completedToday. Toca para ponerte una meta.",
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (reached) "🎉 ¡Meta cumplida!" else "🎯 Meta de hoy",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text("$completedToday / $goal", style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { (completedToday.toFloat() / goal).coerceIn(0f, 1f) },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Text(
                "🔥 Racha: $streak ${if (streak == 1) "día" else "días"} cumpliendo la meta",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (choosing) {
        GoalPickerDialog(
            current = goal,
            onPick = {
                onGoalChange(it)
                choosing = false
            },
            onDismiss = { choosing = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalPickerDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meta diaria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "¿Cuántas tareas o recordatorios quieres completar cada día? La racha cuenta los días que la cumples.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = current == 0, onClick = { onPick(0) }, label = { Text("Sin meta") })
                    (1..10).forEach { n ->
                        FilterChip(selected = current == n, onClick = { onPick(n) }, label = { Text("$n") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
    )
}
