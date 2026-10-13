package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.SectionHeader

private const val FOLDER_COLUMNS = 3

/**
 * Notas organizadas en carpetas personalizadas.
 * - Sin carpeta abierta: cuadrícula de carpetas + todas las notas.
 * - Con carpeta abierta ([openFolderId]): solo las notas de esa carpeta.
 */
@Composable
fun NotesScreen(
    items: List<Item>,
    folders: List<Folder>,
    openFolderId: Long?,
    onOpenFolder: (Long?) -> Unit,
    onOpen: (Item) -> Unit,
    onSaveFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notes = items.filter { it.type == ItemType.NOTE }
    val foldersById = folders.associateBy { it.id }
    val openFolder = openFolderId?.let { foldersById[it] }
    // Diálogo de carpeta: null = cerrado; Folder con id 0 = carpeta nueva.
    var editingFolder by remember { mutableStateOf<Folder?>(null) }

    // Si la carpeta abierta se borró, volver a la vista de carpetas.
    LaunchedEffect(openFolderId, folders) {
        if (openFolderId != null && folders.isNotEmpty() && openFolder == null) onOpenFolder(null)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (openFolder == null) {
            item { SectionHeader("Carpetas") }
            val tiles: List<Folder?> = folders + null // null = "Nueva carpeta"
            tiles.chunked(FOLDER_COLUMNS).forEach { row ->
                item(key = "folders-${row.firstOrNull()?.id ?: "new"}") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { folder ->
                            if (folder != null) {
                                FolderTile(
                                    folder = folder,
                                    count = notes.count { it.folderId == folder.id },
                                    onClick = { onOpenFolder(folder.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                NewFolderTile(
                                    onClick = { editingFolder = Folder(name = "") },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        repeat(FOLDER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            item { SectionHeader("Todas las notas (${notes.size})") }
            if (notes.isEmpty()) {
                item { EmptyState("Aún no hay notas.\nToca + o escribe en la burbuja \"ideas: …\".") }
            }
            items(notes, key = { it.id }) { note ->
                ItemCard(item = note, onClick = { onOpen(note) }, folder = note.folderId?.let(foldersById::get))
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenFolder(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a carpetas")
                    }
                    Text(
                        openFolder.label,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { editingFolder = openFolder }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar carpeta")
                    }
                }
            }
            val inFolder = notes.filter { it.folderId == openFolder.id }
            if (inFolder.isEmpty()) {
                item {
                    EmptyState(
                        "Carpeta vacía.\nToca + o escribe en la burbuja \"${openFolder.name.lowercase()}: …\"."
                    )
                }
            }
            items(inFolder, key = { it.id }) { note ->
                ItemCard(item = note, onClick = { onOpen(note) })
            }
        }
    }

    editingFolder?.let { folder ->
        FolderDialog(
            folder = folder,
            existingNames = folders.filter { it.id != folder.id }.map { it.name },
            onDismiss = { editingFolder = null },
            onSave = {
                onSaveFolder(it)
                editingFolder = null
            },
            onDelete = {
                onDeleteFolder(it)
                editingFolder = null
                onOpenFolder(null)
            },
        )
    }
}

@Composable
private fun FolderTile(folder: Folder, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(folder.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                folder.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                if (count == 1) "1 nota" else "$count notas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NewFolderTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("➕", style = MaterialTheme.typography.headlineMedium)
            Text("Nueva", style = MaterialTheme.typography.titleSmall)
            Text(
                "carpeta",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
