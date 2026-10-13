package com.hilvana.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.screens.InboxScreen
import com.hilvana.app.ui.screens.NotesScreen
import com.hilvana.app.ui.screens.PermissionsState
import com.hilvana.app.ui.screens.SettingsScreen
import com.hilvana.app.ui.screens.TasksScreen
import com.hilvana.app.ui.screens.TodayScreen

private enum class Tab(val label: String, val icon: ImageVector, val newItemType: ItemType) {
    TODAY("Hoy", Icons.Filled.DateRange, ItemType.TASK),
    INBOX("Bandeja", Icons.Filled.Email, ItemType.NOTE),
    NOTES("Notas", Icons.Filled.Edit, ItemType.NOTE),
    TASKS("Tareas", Icons.Filled.CheckCircle, ItemType.TASK),
}

/** Pantalla raíz: barra superior, navegación inferior, botón + y editor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HilvanaRoot(
    permissions: PermissionsState,
    bubbleRunning: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onToggleBubble: () -> Unit,
    viewModel: ItemsViewModel = viewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }
    val inboxCount = items.count { it.inInbox }

    // "Atrás": cierra Ajustes o regresa a Hoy antes de salir de la app.
    BackHandler(enabled = showSettings || tab != Tab.TODAY) {
        if (showSettings) showSettings = false else tab = Tab.TODAY
    }

    if (showSettings) {
        SettingsScreen(
            permissions = permissions,
            bubbleRunning = bubbleRunning,
            onBack = { showSettings = false },
            onRequestOverlay = onRequestOverlay,
            onRequestNotifications = onRequestNotifications,
            onRequestExactAlarms = onRequestExactAlarms,
            onOpenAppSettings = onOpenAppSettings,
            onToggleBubble = onToggleBubble,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == Tab.TODAY) "Hilvana" else tab.label) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        if (permissions.allGranted) {
                            Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                        } else {
                            BadgedBox(badge = { Badge() }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ajustes (faltan permisos)")
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = tab == destination,
                        onClick = { tab = destination },
                        icon = {
                            if (destination == Tab.INBOX && inboxCount > 0) {
                                BadgedBox(badge = { Badge { Text("$inboxCount") } }) {
                                    Icon(destination.icon, contentDescription = null)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = null)
                            }
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Lo creado desde la app ya está ordenado: no pasa por la Bandeja.
                editing = Item(text = "", type = tab.newItemType, inInbox = false)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo")
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        val open: (Item) -> Unit = { editing = it }

        when (tab) {
            Tab.TODAY -> TodayScreen(
                items = items,
                onOpen = open,
                onToggleDone = viewModel::toggleDone,
                modifier = contentModifier,
                banner = if (permissions.allGranted) null else {
                    { SetupBanner(onOpenSettings = { showSettings = true }) }
                },
            )
            Tab.INBOX -> InboxScreen(
                items = items,
                onOpen = open,
                onToggleDone = viewModel::toggleDone,
                onReviewed = viewModel::markReviewed,
                modifier = contentModifier,
            )
            Tab.NOTES -> NotesScreen(items = items, onOpen = open, modifier = contentModifier)
            Tab.TASKS -> TasksScreen(
                items = items,
                onOpen = open,
                onToggleDone = viewModel::toggleDone,
                modifier = contentModifier,
            )
        }
    }

    editing?.let { item ->
        ItemEditorSheet(
            item = item,
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(it)
                editing = null
            },
        )
    }
}

@Composable
private fun SetupBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Termina de configurar Hilvana", style = MaterialTheme.typography.titleSmall)
            Text(
                "Faltan permisos para la burbuja o los recordatorios.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenSettings) { Text("Ir a Ajustes") }
        }
    }
}
