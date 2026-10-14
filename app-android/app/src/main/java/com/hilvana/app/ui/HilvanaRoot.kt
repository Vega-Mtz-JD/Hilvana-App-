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
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.LaunchedEffect
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
import com.hilvana.app.ui.screens.CalendarScreen
import com.hilvana.app.ui.stats.StatsScreen
import com.hilvana.app.ui.screens.InboxScreen
import java.time.LocalDate
import java.time.ZoneId
import com.hilvana.app.ui.screens.NotesScreen
import com.hilvana.app.ui.screens.PermissionsState
import com.hilvana.app.ui.screens.SettingsScreen
import com.hilvana.app.ui.screens.TasksScreen
import com.hilvana.app.ui.screens.TodayScreen

private enum class Tab(val label: String, val icon: ImageVector, val newItemType: ItemType) {
    TODAY("Hoy", Icons.Filled.Home, ItemType.TASK),
    INBOX("Bandeja", Icons.Filled.Email, ItemType.NOTE),
    NOTES("Notas", Icons.Filled.Edit, ItemType.NOTE),
    TASKS("Tareas", Icons.Filled.CheckCircle, ItemType.TASK),
    CALENDAR("Calendario", Icons.Filled.DateRange, ItemType.REMINDER),
}

/** Nuevo recordatorio desde el calendario: ese día a las 9:00 (o en 1 hora si ya pasó). */
private fun reminderTimeFor(date: LocalDate): Long {
    val zone = ZoneId.systemDefault()
    val atNine = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
    val now = System.currentTimeMillis()
    return if (atNine > now) atNine else now + 60 * 60_000L
}

/** Pantalla raíz: barra superior, navegación inferior, botón + y editor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HilvanaRoot(
    permissions: PermissionsState,
    bubbleRunning: Boolean,
    userName: String,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onToggleBubble: () -> Unit,
    onAddCaptureTile: (() -> Unit)?,
    onShowWelcome: () -> Unit,
    unlocked: Boolean,
    onRequestUnlock: (title: String, onSuccess: () -> Unit) -> Unit,
    appLockEnabled: Boolean,
    canUseAppLock: Boolean,
    onSetAppLock: (Boolean) -> Unit,
    viewModel: ItemsViewModel = viewModel(),
) {
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
    val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
    // Mientras no se desbloquee, las notas de carpetas protegidas no se muestran en ningún lado.
    val lockedFolderIds = folders.filter { it.locked }.map { it.id }.toSet()
    val items = if (unlocked || lockedFolderIds.isEmpty()) allItems
    else allItems.filter { it.folderId == null || it.folderId !in lockedFolderIds }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showStats by rememberSaveable { mutableStateOf(false) }
    var openFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var editing by remember { mutableStateOf<Item?>(null) }
    val inboxCount = items.count { it.inInbox }

    // Si se vuelve a bloquear con una carpeta protegida abierta, se cierra.
    LaunchedEffect(unlocked, openFolderId, lockedFolderIds) {
        if (!unlocked && openFolderId in lockedFolderIds) openFolderId = null
    }
    val folderOpen = tab == Tab.NOTES && openFolderId != null

    // "Atrás": cierra Ajustes, luego la carpeta abierta, luego regresa a Hoy antes de salir.
    BackHandler(enabled = showSettings || showStats || folderOpen || tab != Tab.TODAY) {
        when {
            showSettings -> showSettings = false
            showStats -> showStats = false
            folderOpen -> openFolderId = null
            else -> tab = Tab.TODAY
        }
    }

    if (showStats) {
        // Con la app bloqueada no cuenta lo de carpetas protegidas (usa la lista ya filtrada).
        StatsScreen(items = items, folders = folders, onBack = { showStats = false })
        return
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
            onAddCaptureTile = onAddCaptureTile,
            onShowWelcome = onShowWelcome,
            appLockEnabled = appLockEnabled,
            canUseAppLock = canUseAppLock,
            onSetAppLock = onSetAppLock,
            lastBackupAt = lastBackupAt,
            backupBusy = backupBusy,
            onExportBackup = viewModel::exportBackup,
            onImportBackup = viewModel::importBackup,
            exportGate = { action ->
                // El respaldo incluye las carpetas protegidas: si hay algo protegido, pedir huella.
                if (appLockEnabled || folders.any { it.locked }) onRequestUnlock("Exportar respaldo", action)
                else action()
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            tab != Tab.TODAY -> tab.label
                            userName.isNotBlank() -> "Hola, $userName"
                            else -> "Hilvana"
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { showStats = true }) {
                        Text("📊", style = MaterialTheme.typography.titleLarge)
                    }
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
                editing = Item(
                    text = "",
                    type = tab.newItemType,
                    inInbox = false,
                    folderId = if (tab == Tab.NOTES) openFolderId else null,
                    dueAt = if (tab == Tab.CALENDAR) reminderTimeFor(selectedDate) else null,
                )
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
            Tab.NOTES -> NotesScreen(
                items = items,
                folders = folders,
                openFolderId = openFolderId,
                onOpenFolder = { id ->
                    val folder = folders.firstOrNull { it.id == id }
                    if (folder?.locked == true) {
                        onRequestUnlock("Abrir ${folder.name}") { openFolderId = id }
                    } else {
                        openFolderId = id
                    }
                },
                unlocked = unlocked,
                canLockFolders = canUseAppLock,
                onOpen = open,
                onSaveFolder = viewModel::saveFolder,
                onDeleteFolder = viewModel::deleteFolder,
                modifier = contentModifier,
            )
            Tab.TASKS -> TasksScreen(
                items = items,
                onOpen = open,
                onToggleDone = viewModel::toggleDone,
                modifier = contentModifier,
            )
            Tab.CALENDAR -> CalendarScreen(
                items = items,
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it },
                onOpen = open,
                onToggleDone = viewModel::toggleDone,
                modifier = contentModifier,
            )
        }
    }

    editing?.let { item ->
        ItemEditorSheet(
            item = item,
            folders = folders,
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
