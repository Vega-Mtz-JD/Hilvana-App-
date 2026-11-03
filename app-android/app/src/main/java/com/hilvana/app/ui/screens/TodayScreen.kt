package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.SectionHeader
import com.hilvana.app.ui.groupForToday

private const val INBOX_PREVIEW = 3
private const val TASKS_PREVIEW = 3
private const val NOTES_PREVIEW = 5
private const val UPCOMING_PREVIEW = 5

/**
 * Inicio: el resumen del día en una sola pantalla. Accesos con contadores, meta diaria,
 * tareas en prioridad, lo atrasado / de hoy / próximo, y lo último de la Bandeja,
 * las tareas sin fecha y las notas, cada bloque con "Ver todo".
 */
@Composable
fun TodayScreen(
    items: List<Item>,
    folders: List<Folder>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    onOpenInbox: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    banner: (@Composable () -> Unit)? = null,
    goalCard: (@Composable () -> Unit)? = null,
) {
    val groups = groupForToday(items)
    val foldersById = folders.associateBy { it.id }
    val focus = items.filter { it.pinned && !it.done }
    val focusIds = focus.map { it.id }.toSet()
    val overdue = groups.overdue.filter { it.id !in focusIds }
    val today = groups.today.filter { it.id !in focusIds }
    val allUpcoming = groups.upcoming.filter { it.id !in focusIds }
    val upcoming = allUpcoming.take(UPCOMING_PREVIEW)
    // Lo que ya salió arriba no se repite en los bloques de abajo.
    val shown = (focus + overdue + today + upcoming).map { it.id }.toSet()
    val inbox = items.filter { it.inInbox }
    val inboxPreview = inbox.filter { it.id !in shown }.sortedByDescending { it.createdAt }.take(INBOX_PREVIEW)
    val undatedTasks = items.filter { it.type == ItemType.TASK && !it.done && it.dueAt == null && it.id !in shown }
    val tasksPreview = undatedTasks
        .sortedWith(compareByDescending<Item> { it.priority }.thenByDescending { it.createdAt })
        .take(TASKS_PREVIEW)
    val notes = items.filter { it.type == ItemType.NOTE }
    val recentNotes = notes.filter { it.id !in shown && it.id !in inboxPreview.map(Item::id) }
        .sortedByDescending { it.createdAt }
        .take(NOTES_PREVIEW)
    val pendingTasks = items.count { it.type == ItemType.TASK && !it.done }
    val pendingToday = groups.today.count { !it.done }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (banner != null) item { banner() }

        item(key = "shortcuts") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Shortcut(Icons.Outlined.Inbox, "Bandeja", inbox.size, onOpenInbox, Modifier.weight(1f))
                Shortcut(HilvanaIcons.Task, "Tareas", pendingTasks, onOpenTasks, Modifier.weight(1f))
                Shortcut(HilvanaIcons.Calendar, "Hoy", pendingToday, onOpenCalendar, Modifier.weight(1f))
            }
        }

        if (goalCard != null) item(key = "goal") { goalCard() }

        val nothing = focus.isEmpty() && groups.isEmpty && inbox.isEmpty() && undatedTasks.isEmpty() && notes.isEmpty()
        if (nothing) {
            item { EmptyState("Todo listo por ahora.\nToca + o la burbuja para anotar algo.") }
        }

        section("focus", "En prioridad", focus, onOpen, onToggleDone)
        section("overdue", "Atrasado", overdue, onOpen, onToggleDone)
        section("today", "Hoy", today, onOpen, onToggleDone)
        section(
            "upcoming", "Próximos 7 días", upcoming, onOpen, onToggleDone,
            seeAll = if (allUpcoming.size > upcoming.size) ("Ver calendario" to onOpenCalendar) else null,
        )
        section(
            "inbox", "En la bandeja (${inbox.size})", inboxPreview, onOpen, onToggleDone,
            seeAll = if (inbox.isNotEmpty()) ("Ver bandeja" to onOpenInbox) else null,
        )
        section(
            "tasks", "Tareas sin fecha", tasksPreview, onOpen, onToggleDone,
            seeAll = if (undatedTasks.size > tasksPreview.size) ("Ver todas" to onOpenTasks) else null,
        )
        section(
            "notes", "Notas recientes", recentNotes, onOpen, onToggleDone,
            folderOf = { it.folderId?.let(foldersById::get) },
            seeAll = if (notes.isNotEmpty()) ("Ver notas" to onOpenNotes) else null,
        )
    }
}

private fun LazyListScope.section(
    key: String,
    title: String,
    items: List<Item>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    folderOf: (Item) -> Folder? = { null },
    seeAll: Pair<String, () -> Unit>? = null,
) {
    if (items.isEmpty()) return
    item(key = "header-$key") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title, modifier = Modifier.weight(1f))
            seeAll?.let { (label, action) -> TextButton(onClick = action) { Text(label) } }
        }
    }
    items(items, key = { "$key-${it.id}" }) { item ->
        ItemCard(
            item = item,
            onClick = { onOpen(item) },
            onToggleDone = { onToggleDone(item) },
            folder = folderOf(item),
        )
    }
}

/** Acceso con contador: tocarlo abre esa pestaña. */
@Composable
private fun Shortcut(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text("$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
