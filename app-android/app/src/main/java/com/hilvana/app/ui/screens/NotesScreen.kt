package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard

private enum class NotesFilter(val label: String, val types: Set<ItemType>) {
    ALL("Todas", setOf(ItemType.NOTE, ItemType.IDEA)),
    NOTES("Notas", setOf(ItemType.NOTE)),
    IDEAS("Ideas", setOf(ItemType.IDEA)),
}

@Composable
fun NotesScreen(
    items: List<Item>,
    onOpen: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(NotesFilter.ALL) }
    val notes = items.filter { it.type in filter.types }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NotesFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        if (notes.isEmpty()) {
            item { EmptyState("Aún no hay ${filter.label.lowercase()}.\nToca + para escribir una.") }
        }
        items(notes, key = { it.id }) { item ->
            ItemCard(item = item, onClick = { onOpen(item) })
        }
    }
}
