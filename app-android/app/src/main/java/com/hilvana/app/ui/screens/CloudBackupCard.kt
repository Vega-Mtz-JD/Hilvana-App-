package com.hilvana.app.ui.screens

import android.app.PendingIntent
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.ui.ItemsViewModel.CloudState
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.components.IconText
import java.text.DateFormat
import java.util.Date

/**
 * Respaldo en Google Drive: conectar la cuenta, respaldo diario automático,
 * respaldar o restaurar a mano y desconectar.
 */
@Composable
fun CloudBackupCard(
    state: CloudState,
    authIntent: PendingIntent?,
    onAuthResult: (Intent?) -> Unit,
    onConnect: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
) {
    // La pantalla de Google (elegir cuenta y dar permiso) cuando el ViewModel la pide.
    val googleScreen = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onAuthResult(it.data)
    }
    LaunchedEffect(authIntent) {
        authIntent?.let { googleScreen.launch(IntentSenderRequest.Builder(it).build()) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.connected) {
                IconText(HilvanaIcons.Cloud, "Respaldo en Google Drive")
                Text(
                    "Conecta tu cuenta de Google y Hilvana guardará cada día una copia de todo (notas, " +
                        "imágenes y audios) en una carpeta oculta de TU Drive. Si cambias de celular, " +
                        "solo conectas la cuenta y restauras.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onConnect, enabled = !state.busy) { Text("Conectar con Google") }
                Text(
                    "Hilvana solo puede ver su propia carpeta: no tus archivos ni tu contraseña.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                IconText(HilvanaIcons.Cloud, "Conectado: ${state.email}")
                Text(
                    if (state.lastBackupAt == 0L) "Aún no hay respaldo en Drive."
                    else "Último respaldo en Drive: " +
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(state.lastBackupAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Respaldo automático diario", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Solo con internet y batería suficiente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.autoBackup, onCheckedChange = onAutoBackupChange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBackupNow, enabled = !state.busy) { Text("Respaldar ahora") }
                    OutlinedButton(onClick = onRestore, enabled = !state.busy) { Text("Restaurar") }
                }
                Text(
                    "Restaurar suma lo de Drive a lo que ya tienes; no borra nada ni duplica.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onDisconnect, enabled = !state.busy) { Text("Desconectar cuenta") }
            }
            if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
