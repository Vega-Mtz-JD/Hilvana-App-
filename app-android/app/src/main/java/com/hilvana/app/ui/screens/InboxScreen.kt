package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard

/** Todo lo capturado sin revisar. Al marcarlo sale de la Bandeja (sigue en Notas/Tareas). */
@Composable
fun InboxScreen(
    items: List<Item>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    onReviewed: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lo último capturado primero.
    val inbox = items.filter { it.inInbox }.sortedByDescending { it.createdAt }
    var confirmAll by remember { mutableStateOf(false) }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("¿Marcar todo como revisado?") },
            text = { Text("Las ${inbox.size} cosas salen de la Bandeja; siguen en Notas, Tareas y el Calendario.") },
            confirmButton = {
                TextButton(onClick = {
                    inbox.forEach(onReviewed)
                    confirmAll = false
                }) { Text("Sí, todo") }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancelar") } },
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (inbox.isEmpty()) {
            item { EmptyState("Bandeja vacía\nTodo lo que captures con la burbuja llega aquí.") }
        } else {
            item {
                Text(
                    "Revisa lo capturado: tócalo para editarlo o cambiarle el tipo, y márcalo cuando esté listo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (inbox.size > 1) {
                item {
                    OutlinedButton(onClick = { confirmAll = true }) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null)
                        Text("Marcar todo como revisado", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        items(inbox, key = { it.id }) { item ->
            ItemCard(
                item = item,
                onClick = { onOpen(item) },
                onToggleDone = { onToggleDone(item) },
                trailing = {
                    IconButton(onClick = { onReviewed(item) }) {
                        Icon(Icons.Filled.Check, contentDescription = "Marcar como revisado")
                    }
                },
            )
        }
    }
}
