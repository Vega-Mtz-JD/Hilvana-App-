package com.hilvana.app.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.ItemType
import com.hilvana.app.parser.SpanishDateParser
import com.hilvana.app.reminder.QuickTime
import com.hilvana.app.ui.formatDue

/**
 * Mini-panel de captura. Lo usan la burbuja (ventana superpuesta) y CaptureActivity
 * (Compartir, mosaico de Ajustes rápidos y acceso directo).
 */
@Composable
fun CaptureContent(
    onSave: (text: String, type: ItemType, dueAt: Long?) -> Unit,
    onDismiss: () -> Unit,
    initialText: String = "",
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var selectedType by remember { mutableStateOf<ItemType?>(null) }
    var quickTime by remember { mutableStateOf<QuickTime?>(null) }
    var ignoreDetection by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Captura inteligente: "mañana a las 5 llamar a mamá" → Recordatorio mañana 17:00.
    val parsed = remember(text) { SpanishDateParser.parse(text) }
    val detected = parsed.takeIf { !ignoreDetection && it.type != null }
    val detectedDue = detected?.dueAtMillis()
    // Lo que el usuario elige a mano siempre gana sobre lo detectado.
    val effectiveType = selectedType ?: detected?.type
    val isReminder = effectiveType == ItemType.REMINDER
    val usesDetection = detected != null && quickTime == null &&
        (selectedType == null || selectedType == detected.type)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Fondo oscurecido: tocar fuera del panel lo cierra.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 56.dp)
                .fillMaxWidth()
                // Evita que un toque dentro del panel llegue al fondo y lo cierre.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("¿Qué no quieres olvidar?", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Escribe aquí…") },
                    minLines = 2,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                // Lo que Hilvana entendió; tocarlo lo descarta y se guarda el texto tal cual.
                if (detected != null) {
                    InputChip(
                        selected = true,
                        onClick = { ignoreDetection = true },
                        label = {
                            Text(
                                text = buildString {
                                    append("✨ ")
                                    append(detectedDue?.let { "⏰ " + formatDue(it) } ?: detected.type?.label)
                                    if (detected.text.isNotBlank()) append(" · ").append(detected.text)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = "Descartar lo detectado")
                        },
                    )
                }

                // Elegir tipo es opcional: si no se elige, se guarda como Nota en la Bandeja.
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ItemType.entries.forEach { type ->
                        FilterChip(
                            selected = effectiveType == type,
                            onClick = { selectedType = if (selectedType == type) null else type },
                            label = { Text(type.label) },
                        )
                    }
                }

                if (isReminder && detectedDue == null) {
                    Text("¿Cuándo te aviso?", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickTime.entries.forEach { option ->
                            FilterChip(
                                selected = quickTime == option,
                                onClick = { quickTime = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val dueAt = if (isReminder) quickTime?.toMillis() ?: detectedDue else null
                            val finalText = if (usesDetection) {
                                parsed.text.ifBlank { text.trim() }
                            } else {
                                text.trim()
                            }
                            onSave(finalText, effectiveType ?: ItemType.NOTE, dueAt)
                        },
                        // Un recordatorio necesita hora.
                        enabled = text.isNotBlank() &&
                            (!isReminder || quickTime != null || detectedDue != null),
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
