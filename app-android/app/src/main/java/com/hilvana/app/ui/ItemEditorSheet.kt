package com.hilvana.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.reminder.QuickTime
import java.util.Calendar
import java.util.TimeZone

/**
 * Editor de un elemento (nuevo o existente): texto, tipo, fecha/hora, hecho y borrar.
 * Al pasar a Nota/Idea se quita la fecha (en el plan, esos tipos no la tienen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    item: Item,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
) {
    val isNew = item.id == 0L
    var text by rememberSaveable { mutableStateOf(item.text) }
    var type by rememberSaveable { mutableStateOf(item.type) }
    var dueAt by rememberSaveable { mutableStateOf(item.dueAt) }
    var done by rememberSaveable { mutableStateOf(item.done) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Fecha elegida en el calendario, esperando que se elija la hora.
    var pickedDateUtc by remember { mutableStateOf<Long?>(null) }

    val canHaveDate = type.isActionable
    val canSave = text.isNotBlank() && (type != ItemType.REMINDER || dueAt != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isNew) "Nuevo" else "Editar", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("¿Qué no quieres olvidar?") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ItemType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label) },
                    )
                }
            }

            if (canHaveDate) {
                Text("Fecha y hora", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = dueAt?.let { "⏰ " + formatDue(it) }
                        ?: if (type == ItemType.REMINDER) "Elige cuándo avisarte" else "Sin fecha",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickTime.entries.forEach { option ->
                        AssistChip(onClick = { dueAt = option.toMillis() }, label = { Text(option.label) })
                    }
                    AssistChip(onClick = { showDatePicker = true }, label = { Text("📅 Elegir…") })
                    if (dueAt != null && type == ItemType.TASK) {
                        AssistChip(onClick = { dueAt = null }, label = { Text("Quitar fecha") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = done, onCheckedChange = { done = it })
                    Text("Hecho")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isNew) {
                    TextButton(
                        onClick = { onDelete(item) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Borrar")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(
                    onClick = {
                        onSave(
                            item.copy(
                                text = text.trim(),
                                type = type,
                                dueAt = if (canHaveDate) dueAt else null,
                                done = canHaveDate && done,
                            )
                        )
                    },
                    enabled = canSave,
                ) { Text("Guardar") }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = localDateAsUtc(dueAt ?: System.currentTimeMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateUtc = dateState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    pickedDateUtc?.let { dateUtc ->
        val context = LocalContext.current
        val initial = Calendar.getInstance().apply { dueAt?.let { timeInMillis = it } }
        val timeState = rememberTimePickerState(
            initialHour = initial.get(Calendar.HOUR_OF_DAY),
            initialMinute = initial.get(Calendar.MINUTE),
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickedDateUtc = null },
            title = { Text("¿A qué hora?") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    dueAt = combineDateAndTime(dateUtc, timeState.hour, timeState.minute)
                    pickedDateUtc = null
                }) { Text("Listo") }
            },
            dismissButton = {
                TextButton(onClick = { pickedDateUtc = null }) { Text("Cancelar") }
            },
        )
    }
}

/** El DatePicker trabaja con fechas a medianoche UTC; convertimos desde/hacia hora local. */
private fun localDateAsUtc(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun combineDateAndTime(dateUtc: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtc }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), hour, minute)
    }.timeInMillis
}
