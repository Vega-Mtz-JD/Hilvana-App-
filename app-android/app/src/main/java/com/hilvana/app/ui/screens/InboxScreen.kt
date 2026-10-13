package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard

/** Todo lo capturado sin revisar. Al tocar ✓ sale de la Bandeja (sigue en Notas/Tareas). */
@Composable
fun InboxScreen(
    items: List<Item>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    onReviewed: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inbox = items.filter { it.inInbox }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (inbox.isEmpty()) {
            item { EmptyState("Bandeja vacía ✨\nTodo lo que captures con la burbuja llega aquí.") }
        } else {
            item {
                Text(
                    "Revisa lo capturado: tócalo para editarlo o cambiarle el tipo, y ✓ cuando esté listo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
