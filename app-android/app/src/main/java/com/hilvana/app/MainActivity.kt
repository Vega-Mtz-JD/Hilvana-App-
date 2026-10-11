package com.hilvana.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hilvana.app.bubble.BubbleService
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.theme.HilvanaTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {

    // Se vuelven a leer en onResume: el usuario puede regresar de Ajustes con el permiso dado.
    private var canDrawOverlays by mutableStateOf(false)
    private var canPostNotifications by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HilvanaTheme {
                val bubbleRunning by BubbleService.isRunning.collectAsState()
                val inboxFlow = remember { database.itemDao().observeInbox() }
                val inbox by inboxFlow.collectAsState(initial = emptyList())
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> canPostNotifications = granted }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BubbleSetupScreen(
                        canDrawOverlays = canDrawOverlays,
                        canPostNotifications = canPostNotifications,
                        bubbleRunning = bubbleRunning,
                        inbox = inbox,
                        onRequestOverlay = ::openOverlaySettings,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onToggleBubble = {
                            if (bubbleRunning) BubbleService.stop(this) else BubbleService.start(this)
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        canDrawOverlays = Settings.canDrawOverlays(this)
        canPostNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Lleva directo al ajuste "Mostrar sobre otras apps" de Hilvana. */
    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }
}

@Composable
fun BubbleSetupScreen(
    canDrawOverlays: Boolean,
    canPostNotifications: Boolean,
    bubbleRunning: Boolean,
    inbox: List<Item>,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onToggleBubble: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Hilvana", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Prototipo de la burbuja flotante. Captura ideas desde cualquier app.",
            style = MaterialTheme.typography.bodyLarge,
        )

        PermissionCard(
            title = "Mostrar sobre otras apps",
            explanation = "Necesario para que la burbuja aparezca encima de WhatsApp, YouTube, etc.",
            granted = canDrawOverlays,
            buttonText = "Dar permiso",
            onRequest = onRequestOverlay,
        )
        PermissionCard(
            title = "Notificaciones",
            explanation = "Para mostrar el aviso de la burbuja y, más adelante, tus recordatorios.",
            granted = canPostNotifications,
            buttonText = "Permitir",
            onRequest = onRequestNotifications,
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onToggleBubble,
            enabled = canDrawOverlays,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (bubbleRunning) "Ocultar burbuja" else "Mostrar burbuja")
        }
        if (!canDrawOverlays) {
            Text(
                "Primero da el permiso \"Mostrar sobre otras apps\".",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text("Bandeja (${inbox.size})", style = MaterialTheme.typography.titleLarge)
        if (inbox.isEmpty()) {
            Text(
                "Aún no hay nada. Toca la burbuja y escribe algo.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(inbox, key = { it.id }) { item -> InboxRow(item) }
        }
    }
}

@Composable
private fun InboxRow(item: Item) {
    val time = remember(item.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.createdAt))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(item.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${item.type.label} · $time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    explanation: String,
    granted: Boolean,
    buttonText: String,
    onRequest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                (if (granted) "✅ " else "⚠️ ") + title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
            if (!granted) {
                OutlinedButton(onClick = onRequest) { Text(buttonText) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BubbleSetupScreenPreview() {
    HilvanaTheme {
        BubbleSetupScreen(
            canDrawOverlays = false,
            canPostNotifications = true,
            bubbleRunning = false,
            inbox = listOf(Item(id = 1, text = "Llamar a mamá mañana a las 5", type = ItemType.REMINDER)),
            onRequestOverlay = {},
            onRequestNotifications = {},
            onToggleBubble = {},
        )
    }
}
