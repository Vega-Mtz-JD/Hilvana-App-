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
) {
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
                buttonText = "Abrir información de la app",
                onClick = onOpenAppSettings,
            )
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
