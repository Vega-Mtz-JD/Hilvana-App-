package com.hilvana.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.BuildConfig
import com.hilvana.app.data.update.AppRelease

/** Abre la descarga del APK (o la página de la versión) en el navegador. */
@Composable
private fun rememberOpenRelease(): (AppRelease) -> Unit {
    val uriHandler = LocalUriHandler.current
    return { release -> runCatching { uriHandler.openUri(release.apkUrl ?: release.pageUrl) } }
}

/** Aviso en Inicio: hay una versión de prueba nueva. */
@Composable
fun UpdateBanner(release: AppRelease, onLater: () -> Unit) {
    val openRelease = rememberOpenRelease()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconText(Icons.Outlined.SystemUpdate, "Hay una versión nueva: ${release.version}")
            if (release.notes.isNotBlank()) {
                Text(
                    release.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Tus notas no se pierden: la nueva versión se instala encima.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { openRelease(release) }) { Text("Descargar") }
                TextButton(onClick = onLater) { Text("Después") }
            }
        }
    }
}

/** Ajustes: qué versión está instalada, buscar actualizaciones y el aviso automático. */
@Composable
fun VersionCard(
    updateChecks: Boolean,
    onUpdateChecksChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
    available: AppRelease?,
) {
    val openRelease = rememberOpenRelease()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconText(Icons.Outlined.SystemUpdate, "Versión ${BuildConfig.VERSION_NAME} (prueba)")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Avisarme de versiones nuevas", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Revisa en GitHub dos veces al día. No envía tus notas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = updateChecks, onCheckedChange = onUpdateChecksChange)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheckNow) { Text("Buscar actualizaciones") }
                if (available != null) {
                    Button(onClick = { openRelease(available) }) { Text("Descargar ${available.version}") }
                }
            }
        }
    }
}
