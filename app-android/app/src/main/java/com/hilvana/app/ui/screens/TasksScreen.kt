package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.SectionHeader
import com.hilvana.app.data.ItemType

/**
 * Tareas: cosas por hacer sin importar cuándo (los recordatorios van al Calendario).
 * Pendientes arriba (más estrellas primero), completadas abajo.
 */
@Composable
fun TasksScreen(
    items: List<Item>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCompleted by rememberSaveable { mutableStateOf(false) }
    val tasks = items.filter { it.type == ItemType.TASK }
    // Lo más importante primero; a igual importancia, lo que vence antes.
    val pending = tasks.filter { !it.done }
        .sortedWith(compareByDescending<Item> { it.priority }.thenBy(nullsLast()) { it.dueAt })
    // Las tareas en foco van primero, en su propia sección.
    val (focus, others) = pending.partition { it.pinned }
    val completed = tasks.filter { it.done }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pending.isEmpty()) {
            item { EmptyState("Sin tareas pendientes 🎉") }
        }
        if (focus.isNotEmpty()) {
            item(key = "focus-header") { SectionHeader("🎯 En foco · también en tus notificaciones") }
            items(focus, key = { it.id }) { item ->
                ItemCard(item = item, onClick = { onOpen(item) }, onToggleDone = { onToggleDone(item) })
            }
            if (others.isNotEmpty()) item(key = "others-header") { SectionHeader("Pendientes") }
        }
        items(others, key = { it.id }) { item ->
            ItemCard(item = item, onClick = { onOpen(item) }, onToggleDone = { onToggleDone(item) })
        }

        if (completed.isNotEmpty()) {
            item(key = "completed-header") {
                TextButton(onClick = { showCompleted = !showCompleted }) {
                    SectionHeader((if (showCompleted) "▾ " else "▸ ") + "Completadas (${completed.size})")
                }
            }
            if (showCompleted) {
                items(completed, key = { it.id }) { item ->
                    ItemCard(item = item, onClick = { onOpen(item) }, onToggleDone = { onToggleDone(item) })
                }
            }
        }
    }
}
