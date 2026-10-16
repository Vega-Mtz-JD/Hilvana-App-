package com.hilvana.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import android.os.Build
import com.hilvana.app.ui.theme.ThemeMode
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Estado de los permisos que Hilvana necesita (se relee al volver de Ajustes). */
data class PermissionsState(
    val overlay: Boolean = false,
    val notifications: Boolean = false,
    val exactAlarms: Boolean = false,
) {
    val allGranted: Boolean get() = overlay && notifications && exactAlarms
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    permissions: PermissionsState,
    bubbleRunning: Boolean,
    onBack: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onToggleBubble: () -> Unit,
    /** null si el teléfono no permite agregar el mosaico desde la app (Android < 13). */
    onAddCaptureTile: (() -> Unit)?,
    onShowWelcome: () -> Unit,
    appLockEnabled: Boolean,
    canUseAppLock: Boolean,
    onSetAppLock: (Boolean) -> Unit,
    lastBackupAt: Long,
    backupBusy: Boolean,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    /** Ejecuta la acción tras pedir huella si hay algo protegido (el respaldo lo incluye todo). */
    exportGate: (() -> Unit) -> Unit,
    linkPreviews: Boolean,
    onLinkPreviewsChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    cloudCard: @Composable () -> Unit,
) {
    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(onExportBackup) }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportBackup)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Burbuja flotante", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onToggleBubble,
                enabled = permissions.overlay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (bubbleRunning) "Ocultar burbuja" else "Mostrar burbuja")
            }

            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🌗 Tema", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("🎨 Colores del fondo de pantalla", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (dynamicColor) "La app toma los colores de tu fondo de pantalla."
                                    else "Usando los colores de Hilvana (verde azulado y ámbar).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                        }
                    }
                }
            }

            Text("Privacidad", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🔒 Bloquear Hilvana al abrir", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Con tu huella o el PIN de tu teléfono. Se vuelve a bloquear tras 1 minuto " +
                                    "fuera de la app. Capturar desde la burbuja no lo pide.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = onSetAppLock,
                            enabled = canUseAppLock || appLockEnabled,
                        )
                    }
                    Text(
                        if (canUseAppLock) {
                            "También puedes proteger solo una carpeta (p. ej. 🔑 Claves): ábrela, toca ✏️ y " +
                                "activa \"Proteger con huella o PIN\"."
                        } else {
                            "⚠️ Tu teléfono no tiene bloqueo de pantalla. Configúralo en Ajustes del teléfono " +
                                "para usar esta opción."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🔗 Vista previa de enlaces", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Trae el título y la miniatura de los links que guardas (YouTube, TikTok, " +
                                "páginas…). Para eso Hilvana visita esa página; tus notas no se envían.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = linkPreviews, onCheckedChange = onLinkPreviewsChange)
                }
            }

            Text("Respaldo", style = MaterialTheme.typography.titleMedium)
            cloudCard()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Guarda todo (notas, tareas, recordatorios, carpetas e imágenes) en un archivo " +
                            "para cambiar de teléfono o tener una copia. Tú eliges dónde: Drive, Descargas, " +
                            "WhatsApp…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        if (lastBackupAt == 0L) {
                            "Aún no has hecho un respaldo."
                        } else {
                            "Último respaldo: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(lastBackupAt))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportGate { createBackup.launch("hilvana-respaldo-${LocalDate.now()}.zip") }
                            },
                            enabled = !backupBusy,
                        ) { Text("📤 Exportar") }
                        OutlinedButton(
                            onClick = { openBackup.launch(arrayOf("application/zip", "application/octet-stream")) },
                            enabled = !backupBusy,
                        ) { Text("📥 Importar") }
                    }
                    if (backupBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        "Importar suma lo del respaldo a lo que ya tienes; no borra nada ni duplica.\n" +
                            "⚠️ El archivo no está cifrado e incluye tus carpetas protegidas: guárdalo en un " +
                            "lugar seguro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Otras formas de capturar", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sin burbuja también puedes anotar rápido:", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "• Compartir → Hilvana desde cualquier app (textos y enlaces).\n" +
                            "• Mantén presionado el ícono de Hilvana → Capturar.\n" +
                            "• Mosaico \"Capturar\" en Ajustes rápidos (baja la barra de notificaciones).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (onAddCaptureTile != null) {
                        OutlinedButton(onClick = onAddCaptureTile) { Text("Agregar mosaico a Ajustes rápidos") }
                    } else {
                        Text(
                            "Para el mosaico: baja la barra, toca el lápiz ✏️ y arrastra \"Capturar\".",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Text("Permisos", style = MaterialTheme.typography.titleMedium)
            if (permissions.allGranted) {
                Text("✅ Todos los permisos listos", style = MaterialTheme.typography.bodyMedium)
            }
            if (!permissions.overlay) {
                SettingsCard(
                    title = "⚠️ Mostrar sobre otras apps",
                    explanation = "Necesario para que la burbuja aparezca encima de WhatsApp, YouTube, etc.",
                    buttonText = "Dar permiso",
                    onClick = onRequestOverlay,
                )
            }
            if (!permissions.notifications) {
                SettingsCard(
                    title = "⚠️ Notificaciones",
                    explanation = "Para mostrar el aviso de la burbuja y tus recordatorios.",
                    buttonText = "Permitir",
                    onClick = onRequestNotifications,
                )
            }
            if (!permissions.exactAlarms) {
                SettingsCard(
                    title = "⚠️ Alarmas y recordatorios",
                    explanation = "Para que tus recordatorios suenen a la hora exacta y no minutos después.",
                    buttonText = "Permitir",
                    onClick = onRequestExactAlarms,
                )
            }

            // Xiaomi, Samsung, Huawei… cierran apps en segundo plano (ver plan §7.2).
            SettingsCard(
                title = "¿La burbuja o los recordatorios se detienen?",
                explanation = "Algunos teléfonos (Xiaomi, Samsung, Huawei) cierran apps para ahorrar " +
                    "batería. En la información de la app activa \"Inicio automático\" y pon " +
                    "\"Ahorro de batería\" en \"Sin restricciones\".",
                buttonText = "Abrir ajustes de batería",
                onClick = onOpenAppSettings,
            )

            OutlinedButton(onClick = onShowWelcome, modifier = Modifier.fillMaxWidth()) {
                Text("Ver la bienvenida otra vez")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    explanation: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onClick) { Text(buttonText) }
        }
    }
}
