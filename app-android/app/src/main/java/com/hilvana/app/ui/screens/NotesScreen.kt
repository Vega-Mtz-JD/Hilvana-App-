package com.hilvana.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.components.IconText
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.SectionHeader
import com.hilvana.app.ui.components.iconVector

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
    /** Renombrar una categoría en todas sus carpetas (null = quitarla). */
    onRenameCategory: (String, String?) -> Unit,
    /** false: las carpetas protegidas dicen "Protegida" en lugar de cuántas notas tienen. */
    unlocked: Boolean,
    canLockFolders: Boolean,
    modifier: Modifier = Modifier,
) {
    val notes = items.filter { it.type == ItemType.NOTE }
    val foldersById = folders.associateBy { it.id }
    val openFolder = openFolderId?.let { foldersById[it] }
    // Diálogo de carpeta: null = cerrado; Folder con id 0 = carpeta nueva.
    var editingFolder by remember { mutableStateOf<Folder?>(null) }
    val categories = folders.mapNotNull { it.category }.distinct().sortedBy { it.lowercase() }
    var collapsedCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    var renamingCategory by remember { mutableStateOf<String?>(null) }

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
            val folderRows: (String, List<Folder?>) -> Unit = { keyPrefix, tiles ->
                tiles.chunked(FOLDER_COLUMNS).forEach { row ->
                    item(key = "$keyPrefix-${row.firstOrNull()?.id ?: "new"}") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { folder ->
                                if (folder != null) {
                                    FolderTile(
                                        folder = folder,
                                        count = notes.count { it.folderId == folder.id },
                                        hidden = folder.locked && !unlocked,
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
            }

            if (categories.isEmpty()) {
                item { SectionHeader("Carpetas") }
                folderRows("folders", folders + null) // null = "Nueva carpeta"
            } else {
                categories.forEach { category ->
                    val inCategory = folders.filter { it.category == category }
                    val collapsed = category in collapsedCategories
                    item(key = "category-$category") {
                        CategoryHeader(
                            name = category,
                            folderCount = inCategory.size,
                            collapsed = collapsed,
                            onToggle = {
                                collapsedCategories =
                                    if (collapsed) collapsedCategories - category else collapsedCategories + category
                            },
                            onRename = { renamingCategory = category },
                        )
                    }
                    if (!collapsed) folderRows("cat-$category", inCategory)
                }
                val uncategorized = folders.filter { it.category == null }
                item(key = "category-none") {
                    SectionHeader(if (uncategorized.isEmpty()) "Nueva carpeta" else "Sin categoría")
                }
                folderRows("uncategorized", uncategorized + null)
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
                    IconText(
                        openFolder.iconVector,
                        openFolder.name,
                        style = MaterialTheme.typography.titleLarge,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconSize = 24.dp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (openFolder.locked) {
                        Icon(
                            HilvanaIcons.Lock,
                            contentDescription = "Protegida",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
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
            existingCategories = categories,
            canLock = canLockFolders,
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

    renamingCategory?.let { category ->
        RenameCategoryDialog(
            name = category,
            onDismiss = { renamingCategory = null },
            onRename = { newName ->
                onRenameCategory(category, newName)
                collapsedCategories = collapsedCategories - category
                renamingCategory = null
            },
        )
    }
}

/** Encabezado de una categoría: tocarlo la pliega/despliega; el lápiz la renombra. */
@Composable
private fun CategoryHeader(
    name: String,
    folderCount: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (collapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (collapsed) "Mostrar carpetas" else "Ocultar carpetas",
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        Text(
            if (folderCount == 1) "1 carpeta" else "$folderCount carpetas",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onRename) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Renombrar categoría",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Renombrar una categoría (cambia en todas sus carpetas) o quitarla. */
@Composable
private fun RenameCategoryDialog(name: String, onDismiss: () -> Unit, onRename: (String?) -> Unit) {
    var text by rememberSaveable { mutableStateOf(name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(24) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onRename(null) }) {
                    Text("Quitar categoría (las carpetas se quedan)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onRename(text) }, enabled = text.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun FolderTile(
    folder: Folder,
    count: Int,
    hidden: Boolean,
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
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                folder.iconVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(30.dp),
            )
            Text(
                folder.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                when {
                    hidden -> "Protegida"
                    count == 1 -> "1 nota"
                    else -> "$count notas"
                },
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
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(30.dp),
            )
            Text("Nueva", style = MaterialTheme.typography.titleSmall)
            Text(
                "carpeta",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
