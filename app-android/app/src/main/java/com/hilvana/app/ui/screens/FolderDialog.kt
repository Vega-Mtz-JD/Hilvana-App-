package com.hilvana.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder

/** Ideas de carpetas para empezar (los ejemplos de uso del plan). */
private val SUGGESTIONS = listOf(
    "💡" to "Ideas",
    "🔑" to "Claves",
    "🔗" to "Links",
    "🎬" to "Videos",
    "🧠" to "Descubrimientos",
    "📷" to "Capturas",
    "🛒" to "Compras",
    "📚" to "Aprender",
)

private val EMOJIS = listOf(
    "📁", "💡", "🔑", "🔗", "🎬", "🧠", "📷", "🛒", "📚", "⭐", "❤️", "💼", "🏠", "✈️", "🎵", "🍳",
)

/** Crear o editar una carpeta: nombre, emoji y (si ya existe) borrarla. */
@Composable
fun FolderDialog(
    folder: Folder,
    existingNames: List<String>,
    /** false si el teléfono no tiene bloqueo de pantalla (no se podría desbloquear). */
    canLock: Boolean,
    onDismiss: () -> Unit,
    onSave: (Folder) -> Unit,
    onDelete: (Folder) -> Unit,
) {
    val isNew = folder.id == 0L
    var name by rememberSaveable { mutableStateOf(folder.name) }
    var emoji by rememberSaveable { mutableStateOf(folder.emoji) }
    var locked by rememberSaveable { mutableStateOf(folder.locked) }
    var confirmDelete by remember { mutableStateOf(false) }

    val trimmed = name.trim()
    val duplicated = existingNames.any { Folder(name = it).matches(trimmed) }
    val canSave = trimmed.isNotEmpty() && !duplicated

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nueva carpeta" else "Editar carpeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isNew) {
                    Text("Sugerencias", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SUGGESTIONS
                            .filter { (_, suggested) -> existingNames.none { Folder(name = it).matches(suggested) } }
                            .forEach { (suggestedEmoji, suggestedName) ->
                                AssistChip(
                                    onClick = {
                                        name = suggestedName
                                        emoji = suggestedEmoji
                                        // Las claves se protegen por defecto.
                                        if (suggestedName == "Claves" && canLock) locked = true
                                    },
                                    label = { Text("$suggestedEmoji $suggestedName") },
                                )
                            }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = duplicated,
                    supportingText = if (duplicated) {
                        { Text("Ya tienes una carpeta con ese nombre") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Ícono", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EMOJIS.forEach { option ->
                        FilterChip(
                            selected = emoji == option,
                            onClick = { emoji = option },
                            label = { Text(option) },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🔒 Proteger con huella o PIN", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (canLock) {
                                "Sus notas se ocultan hasta que desbloquees."
                            } else {
                                "Configura un bloqueo de pantalla en tu teléfono para usarlo."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = locked,
                        onCheckedChange = { locked = it },
                        // Se puede quitar siempre; activarlo solo si hay con qué desbloquear.
                        enabled = canLock || locked,
                    )
                }

                if (!isNew) {
                    if (!confirmDelete) {
                        TextButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) { Text("Borrar carpeta") }
                    } else {
                        Text(
                            "¿Borrar \"${folder.name}\"? Sus notas NO se borran: quedan sin carpeta.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { confirmDelete = false }) { Text("No") }
                            TextButton(
                                onClick = { onDelete(folder) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) { Text("Sí, borrar") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(folder.copy(name = trimmed, emoji = emoji, locked = locked)) },
                enabled = canSave,
            ) { Text(if (isNew) "Crear" else "Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
