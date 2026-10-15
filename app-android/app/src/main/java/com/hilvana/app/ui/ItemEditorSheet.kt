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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import com.hilvana.app.data.Folder
import com.hilvana.app.data.ImageStore
import com.hilvana.app.data.Item
import com.hilvana.app.ui.components.ImageViewer
import com.hilvana.app.ui.components.ItemImage
import androidx.compose.runtime.mutableIntStateOf
import com.hilvana.app.data.MAX_PINNED
import com.hilvana.app.ui.components.AudioPlayer
import com.hilvana.app.ui.components.ChecklistEditor
import com.hilvana.app.ui.components.FocusSection
import com.hilvana.app.ui.components.RepeatSelector
import com.hilvana.app.ui.components.StarRating
import com.hilvana.app.ui.components.TypeSelector
import com.hilvana.app.ui.components.prompt
import kotlinx.coroutines.launch
import com.hilvana.app.data.ItemType
import com.hilvana.app.reminder.QuickTime
import java.util.Calendar
import java.util.TimeZone

/**
 * Editor de un elemento (nuevo o existente): texto, tipo, fecha/hora, hecho y borrar.
 * Al pasar a Nota se quita la fecha (las notas no la tienen) y se puede elegir carpeta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    item: Item,
    folders: List<Folder>,
    /** Cuántas OTRAS tareas están en foco (para el máximo). */
    otherPinnedCount: Int,
    nudgeHours: Int,
    onNudgeHoursChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
) {
    val isNew = item.id == 0L
    var text by rememberSaveable { mutableStateOf(item.text) }
    var type by rememberSaveable { mutableStateOf(item.type) }
    var dueAt by rememberSaveable { mutableStateOf(item.dueAt) }
    var done by rememberSaveable { mutableStateOf(item.done) }
    var folderId by rememberSaveable { mutableStateOf(item.folderId) }
    var priority by rememberSaveable { mutableIntStateOf(item.priority) }
    var repeat by rememberSaveable { mutableStateOf(item.repeatRule) }
    var checklist by remember { mutableStateOf(item.checklist) }
    var audioPath by rememberSaveable { mutableStateOf(item.audioPath) }
    var pinned by rememberSaveable { mutableStateOf(item.pinned) }
    var imagePath by rememberSaveable { mutableStateOf(item.imagePath) }
    var showImage by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Fecha elegida en el calendario, esperando que se elija la hora.
    var pickedDateUtc by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val imported = ImageStore.import(context, uri)
                if (imported == null) {
                    Toast.makeText(context, "No se pudo abrir esa imagen", Toast.LENGTH_SHORT).show()
                } else {
                    // La imagen elegida antes en este editor (sin guardar) ya no sirve.
                    if (imagePath != item.imagePath) ImageStore.delete(imagePath)
                    imagePath = imported
                }
            }
        }
    }

    /** Al salir sin guardar, borra la imagen que se agregó en este editor. */
    fun discardNewImage() {
        if (imagePath != item.imagePath) ImageStore.delete(imagePath)
    }

    val canHaveDate = type.isActionable
    val canSave = (text.isNotBlank() || imagePath != null || audioPath != null) &&
        (type != ItemType.REMINDER || dueAt != null)

    ModalBottomSheet(
        onDismissRequest = {
            discardNewImage()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isNew) "Nuevo" else "Editar", style = MaterialTheme.typography.titleLarge)

            TypeSelector(selected = type, onSelect = { type = it })

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(type.prompt) },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            audioPath?.let { path ->
                AudioPlayer(
                    path = path,
                    durationMs = item.audioDurationMs,
                    trailing = { TextButton(onClick = { audioPath = null }) { Text("Quitar") } },
                )
            }

            val currentImage = imagePath
            if (currentImage != null) {
                ItemImage(
                    path = currentImage,
                    maxSide = 1024,
                    onClick = { showImage = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickImage.launch(PickVisualMediaRequest(ImageOnly)) }) {
                        Text("Cambiar imagen")
                    }
                    TextButton(onClick = {
                        discardNewImage()
                        imagePath = null
                    }) { Text("Quitar") }
                }
            } else {
                OutlinedButton(onClick = { pickImage.launch(PickVisualMediaRequest(ImageOnly)) }) {
                    Text("📷 Agregar imagen")
                }
            }


            if (type == ItemType.NOTE && folders.isNotEmpty()) {
                Text("Carpeta", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = folderId == null,
                        onClick = { folderId = null },
                        label = { Text("Sin carpeta") },
                    )
                    folders.forEach { folder ->
                        FilterChip(
                            selected = folderId == folder.id,
                            onClick = { folderId = folder.id },
                            label = { Text(folder.label) },
                        )
                    }
                }
            }

            if (type == ItemType.TASK) {
                ChecklistEditor(steps = checklist, onChange = { checklist = it })
                FocusSection(
                    pinned = pinned,
                    canPin = otherPinnedCount < MAX_PINNED,
                    nudgeHours = nudgeHours,
                    onPinnedChange = { pinned = it },
                    onNudgeHoursChange = onNudgeHoursChange,
                )
            }

            if (canHaveDate) {
                Text("Importancia", style = MaterialTheme.typography.labelLarge)
                StarRating(priority = priority, onChange = { priority = it })

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
                    QuickTime.available().forEach { option ->
                        AssistChip(onClick = { dueAt = option.toMillis() }, label = { Text(option.label) })
                    }
                    AssistChip(onClick = { showDatePicker = true }, label = { Text("📅 Elegir…") })
                    if (dueAt != null && type == ItemType.TASK) {
                        AssistChip(onClick = { dueAt = null }, label = { Text("Quitar fecha") })
                    }
                }
                // Repetir necesita una fecha de inicio.
                if (dueAt != null) {
                    RepeatSelector(repeat = repeat, onChange = { repeat = it })
                } else {
                    Text(
                        "🔁 Para que se repita, primero elige una fecha.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = done, onCheckedChange = { done = it })
                    Text(if (repeat != null && dueAt != null) "Hecho (pasa a la próxima fecha)" else "Hecho")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isNew) {
                    TextButton(
                        onClick = {
                            discardNewImage()
                            onDelete(item)
                        },
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
                TextButton(onClick = {
                    discardNewImage()
                    onDismiss()
                }) { Text("Cancelar") }
                Button(
                    onClick = {
                        onSave(
                            item.copy(
                                // Una imagen sola también es una nota válida.
                                text = text.trim().ifBlank { if (imagePath != null) "📷 Imagen" else "🎙️ Nota de voz" },
                                imagePath = imagePath,
                                type = type,
                                dueAt = if (canHaveDate) dueAt else null,
                                done = canHaveDate && done,
                                folderId = if (type == ItemType.NOTE) folderId else null,
                                priority = if (canHaveDate) priority else 0,
                                repeatRule = if (canHaveDate && dueAt != null) repeat else null,
                                checklist = checklist,
                                audioPath = audioPath,
                                audioDurationMs = if (audioPath != null) item.audioDurationMs else null,
                                pinned = type == ItemType.TASK && pinned,
                            )
                        )
                    },
                    enabled = canSave,
                ) { Text("Guardar") }
            }
        }
    }

    val viewerImage = imagePath
    if (showImage && viewerImage != null) {
        ImageViewer(path = viewerImage, onDismiss = { showImage = false })
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
