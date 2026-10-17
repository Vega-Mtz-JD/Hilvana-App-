package com.hilvana.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hilvana.app.R
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.screens.PermissionsState

private enum class Step { WELCOME, NAME, BUBBLE, REMINDERS, BATTERY, DONE }

/**
 * Bienvenida de la primera vez (plan §7.1): explica POR QUÉ se pide cada permiso
 * y detecta cuando el usuario regresa de Ajustes con el permiso dado. Todo se
 * puede saltar: sin burbuja la app sigue funcionando (plan B, §7.5).
 */
@Composable
fun OnboardingScreen(
    permissions: PermissionsState,
    initialName: String,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onOpenAutostart: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onFinish: (name: String, showBubble: Boolean) -> Unit,
) {
    val steps = remember { Step.entries.filter { it != Step.BATTERY || BatteryHelp.isAggressiveManufacturer } }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(initialName) }
    val step = steps[index]
    val next = { if (index < steps.lastIndex) index++ }

    BackHandler(enabled = index > 0) { index-- }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
        ) {
            StepDots(count = steps.size, current = index)
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding",
            ) { current ->
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (current) {
                        Step.WELCOME -> WelcomeStep()
                        Step.NAME -> NameStep(name = name, onNameChange = { name = it.take(30) }, onDone = next)
                        Step.BUBBLE -> PermissionStep(
                            icon = Icons.Outlined.BubbleChart,
                            title = "La burbuja flotante",
                            body = "Es el corazón de Hilvana: una burbuja que flota encima de cualquier app " +
                                "(WhatsApp, YouTube, el navegador…) para anotar algo en 2 segundos sin salir " +
                                "de lo que haces.\n\nAndroid pide que actives tú mismo \"Mostrar sobre otras " +
                                "apps\". Te llevamos directo al ajuste: activa Hilvana y regresa.",
                            items = listOf(
                                PermissionItem("Mostrar sobre otras apps", permissions.overlay, onRequestOverlay),
                            ),
                        )
                        Step.REMINDERS -> PermissionStep(
                            icon = HilvanaIcons.Reminder,
                            title = "Recordatorios que sí suenan",
                            body = "Para avisarte a la hora exacta, aunque Hilvana esté cerrada o el " +
                                "teléfono bloqueado, necesitamos poder mostrar notificaciones y programar " +
                                "alarmas.",
                            items = listOf(
                                PermissionItem("Notificaciones", permissions.notifications, onRequestNotifications),
                                PermissionItem("Alarmas y recordatorios", permissions.exactAlarms, onRequestExactAlarms),
                            ),
                        )
                        Step.BATTERY -> BatteryStep(onOpenAutostart = onOpenAutostart, onOpenAppDetails = onOpenAppDetails)
                        Step.DONE -> DoneStep(name = name, canShowBubble = permissions.overlay)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (index > 0 && step != Step.DONE) {
                    TextButton(onClick = { index-- }) { Text("Atrás") }
                }
                Spacer(Modifier.weight(1f))
                when (step) {
                    Step.DONE -> Column(horizontalAlignment = Alignment.End) {
                        if (permissions.overlay) {
                            Button(onClick = { onFinish(name, true) }) { Text("Empezar con la burbuja") }
                            TextButton(onClick = { onFinish(name, false) }) { Text("Empezar sin burbuja") }
                        } else {
                            Button(onClick = { onFinish(name, false) }) { Text("Empezar") }
                        }
                    }
                    Step.BUBBLE -> if (permissions.overlay) {
                        Button(onClick = next) { Text("Siguiente") }
                    } else {
                        TextButton(onClick = next) { Text("Ahora no") }
                    }
                    Step.REMINDERS -> if (permissions.notifications && permissions.exactAlarms) {
                        Button(onClick = next) { Text("Siguiente") }
                    } else {
                        TextButton(onClick = next) { Text("Ahora no") }
                    }
                    Step.WELCOME -> Button(onClick = next) { Text("Comenzar") }
                    else -> Button(onClick = next) { Text("Siguiente") }
                }
            }
        }
    }
}

@Composable
private fun StepDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
private fun BigIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun WelcomeStep() {
    BigIcon(ImageVector.vectorResource(R.drawable.ic_stat_hilvana))
    Text("Bienvenido a Hilvana", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Que nada se te olvide: lo anotas en 2 segundos desde cualquier app, y Hilvana lo ordena por ti.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Feature(Icons.Outlined.BubbleChart, "Burbuja flotante", "Anota sin salir de lo que estás haciendo.")
    Feature(HilvanaIcons.Smart, "Captura inteligente", "Escribe \"mañana a las 5 llamar a mamá\" y se crea el recordatorio.")
    Feature(HilvanaIcons.Folder, "Notas en carpetas", "Ideas, links, capturas… cada cosa en su lugar.")
    Feature(HilvanaIcons.Calendar, "Calendario", "Tus recordatorios y tareas, día por día.")
    Feature(HilvanaIcons.Lock, "Privado", "Todo se guarda en tu teléfono y funciona sin internet.")
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(26.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    BigIcon(Icons.Outlined.WavingHand)
    Text("¿Cómo te llamas?", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Es solo para saludarte; se queda en tu teléfono. Puedes dejarlo vacío.",
        style = MaterialTheme.typography.bodyLarge,
    )
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Tu nombre") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "Más adelante podrás conectar tu cuenta de Google para respaldar tus notas en la nube.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class PermissionItem(val label: String, val granted: Boolean, val onRequest: () -> Unit)

@Composable
private fun PermissionStep(icon: ImageVector, title: String, body: String, items: List<PermissionItem>) {
    BigIcon(icon)
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Text(body, style = MaterialTheme.typography.bodyLarge)
    items.forEach { item ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (item.granted) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.granted) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "Activado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp),
                    )
                }
                Text(
                    item.label,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (!item.granted) {
                    OutlinedButton(onClick = item.onRequest) { Text("Activar") }
                }
            }
        }
    }
}

@Composable
private fun BatteryStep(onOpenAutostart: () -> Unit, onOpenAppDetails: () -> Unit) {
    BigIcon(Icons.Outlined.BatteryChargingFull)
    Text("Que tu teléfono no la cierre", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Tu teléfono cierra apps en segundo plano para ahorrar batería, y eso puede apagar la " +
            "burbuja o retrasar recordatorios. Dos ajustes lo evitan:",
        style = MaterialTheme.typography.bodyLarge,
    )
    if (BatteryHelp.isXiaomi) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Inicio automático", style = MaterialTheme.typography.titleSmall)
                Text("Busca Hilvana en la lista y actívalo.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onOpenAutostart) { Text("Abrir Inicio automático") }
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                (if (BatteryHelp.isXiaomi) "2. " else "") + "Ahorro de batería: Sin restricciones",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "En la información de la app entra a \"Ahorro de batería\" (o \"Batería\") y elige " +
                    "\"Sin restricciones\".",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenAppDetails) { Text("Abrir información de la app") }
        }
    }
    Text(
        "Puedes volver a esto cuando quieras desde Ajustes.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun DoneStep(name: String, canShowBubble: Boolean) {
    BigIcon(HilvanaIcons.Celebrate)
    Text(
        if (name.isBlank()) "¡Todo listo!" else "¡Todo listo, ${name.trim()}!",
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        if (canShowBubble) {
            "Toca la burbuja cuando se te ocurra algo. Prueba escribir:\n\"mañana a las 5 llamar a mamá\""
        } else {
            "Sin burbuja también puedes anotar rápido: Compartir → Hilvana, el mosaico \"Capturar\" de " +
                "Ajustes rápidos o mantener presionado el ícono de la app."
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}
