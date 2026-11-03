package com.hilvana.app.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.ChipIcon
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.components.icon
import com.hilvana.app.ui.components.iconVector
import com.hilvana.app.ui.components.SortMenu

/** Buscar en todo, con filtros rápidos y las palabras resaltadas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    items: List<Item>,
    folders: List<Folder>,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<ItemType?>(null) }
    var includeDone by rememberSaveable { mutableStateOf(false) }
    var withLink by rememberSaveable { mutableStateOf(false) }
    var withImage by rememberSaveable { mutableStateOf(false) }
    var important by rememberSaveable { mutableStateOf(false) }
    var folderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortOrder.RELEVANCE) }
    val filters = SearchFilters(type, includeDone, withLink, withImage, important, folderId, category)
    val results = remember(items, folders, query, filters, sort) {
        SearchEngine.search(items, folders, query, filters, sort)
    }
    val categories = remember(folders) { folders.mapNotNull { it.category }.distinct().sortedBy { it.lowercase() } }
    val foldersById = remember(folders) { folders.associateBy { it.id } }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar en notas, tareas y recordatorios…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ItemType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = if (type == option) null else option },
                            label = { Text(option.label) },
                            leadingIcon = { ChipIcon(option.icon) },
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = withLink, onClick = { withLink = !withLink }, label = { Text("Con enlace") }, leadingIcon = { ChipIcon(HilvanaIcons.Link) })
                    FilterChip(selected = withImage, onClick = { withImage = !withImage }, label = { Text("Con imagen") }, leadingIcon = { ChipIcon(HilvanaIcons.Image) })
                    FilterChip(selected = important, onClick = { important = !important }, label = { Text("Importantes") }, leadingIcon = { ChipIcon(HilvanaIcons.Star) })
                    FilterChip(selected = includeDone, onClick = { includeDone = !includeDone }, label = { Text("Incluir hechas") }, leadingIcon = { ChipIcon(HilvanaIcons.Task) })
                }
            }
            // Dónde buscar: una categoría (todas sus carpetas) o una carpeta.
            if (categories.isNotEmpty() || folders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = {
                                    category = if (category == option) null else option
                                    folderId = null
                                },
                                label = { Text(option) },
                                leadingIcon = { ChipIcon(HilvanaIcons.Category) },
                            )
                        }
                        folders.forEach { folder ->
                            FilterChip(
                                selected = folderId == folder.id,
                                onClick = {
                                    folderId = if (folderId == folder.id) null else folder.id
                                    category = null
                                },
                                label = { Text(folder.name) },
                                leadingIcon = { ChipIcon(folder.iconVector) },
                            )
                        }
                    }
                }
            }

            when {
                query.isBlank() && filters.isEmpty -> item {
                    EmptyState(
                        "Escribe algo o toca un filtro.\n" +
                            "Busca en títulos, textos, links, pasos, carpetas y categorías."
                    )
                }
                results.isEmpty() -> item {
                    EmptyState("Nada encontrado" + if (!includeDone) ".\nPrueba con \"Incluir hechas\"." else ".")
                }
                else -> item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (results.size == 1) "1 resultado" else "${results.size} resultados",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        SortMenu(selected = sort, onSelect = { sort = it })
                    }
                }
            }
            items(results, key = { it.id }) { item ->
                ItemCard(
                    item = item,
                    onClick = { onOpen(item) },
                    onToggleDone = { onToggleDone(item) },
                    folder = item.folderId?.let(foldersById::get),
                    highlight = query,
                )
            }
        }
    }
}
