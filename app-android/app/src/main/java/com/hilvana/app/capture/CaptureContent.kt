package com.hilvana.app.capture

import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.ImageStore
import com.hilvana.app.data.ItemType
import com.hilvana.app.parser.SpanishDateParser
import com.hilvana.app.reminder.QuickTime
import androidx.compose.foundation.BorderStroke
import com.hilvana.app.ui.components.ItemImage
import com.hilvana.app.ui.components.StarRating
import com.hilvana.app.ui.components.TypeSelector
import com.hilvana.app.ui.components.starsText
import com.hilvana.app.ui.components.color
import com.hilvana.app.ui.components.prompt
import com.hilvana.app.ui.formatDue
import kotlinx.coroutines.launch

/**
 * Mini-panel de captura. Lo usan la burbuja (ventana superpuesta) y CaptureActivity
 * (Compartir, mosaico de Ajustes rápidos, acceso directo y widget).
 *
 * La imagen la administra quien muestra el panel ([imagePath] / [onImageChange]),
 * porque es quien sabe si se guardó o se canceló (y si hay que borrar el archivo).
 *
 * @param onPickImage abre el selector de fotos con el texto escrito hasta ahora.
 */
@Composable
fun CaptureContent(
    folders: List<Folder>,
    imagePath: String?,
    onImageChange: (String?) -> Unit,
    onPickImage: (currentText: String) -> Unit,
    onSave: (text: String, type: ItemType, dueAt: Long?, folderId: Long?, priority: Int) -> Unit,
    onDismiss: () -> Unit,
    initialText: String = "",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var text by rememberSaveable { mutableStateOf(initialText) }
    var selectedType by remember { mutableStateOf<ItemType?>(null) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var quickTime by remember { mutableStateOf<QuickTime?>(null) }
    var customDue by remember { mutableStateOf<Long?>(null) }
    var manualPriority by remember { mutableStateOf<Int?>(null) }
    var ignoreDetection by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val clipKind = remember { ClipboardReader.peekKind(context) }
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxPanelHeight = with(LocalDensity.current) { (windowHeight * 0.85f).toDp() }

    // Captura inteligente: "mañana a las 5 llamar a mamá" → Recordatorio mañana 17:00,
    // y "links: https://…" → Nota en la carpeta Links (si existe).
    val parsed = remember(text) { SpanishDateParser.parse(text) }
    val prefixFolder = if (ignoreDetection) null else parsed.prefix?.let { p -> folders.firstOrNull { it.matches(p) } }
    val detected = parsed.takeIf {
        !ignoreDetection && (it.type != null || prefixFolder != null || it.priority != null)
    }
    val detectedDue = detected?.dueAtMillis()
    val detectedPriority = detected?.priority
    // Lo que el usuario elige a mano siempre gana sobre lo detectado.
    val effectiveType = selectedType ?: detected?.type ?: when {
        prefixFolder != null -> ItemType.NOTE
        detectedPriority != null -> ItemType.TASK // "pagar luz urgente" suena a tarea
        else -> null
    }
    val isReminder = effectiveType == ItemType.REMINDER
    val isNote = (effectiveType ?: ItemType.NOTE) == ItemType.NOTE
    val effectiveFolder = if (!isNote) null
    else selectedFolderId?.let { id -> folders.firstOrNull { it.id == id } } ?: prefixFolder
    val usesPrefix = isNote && prefixFolder != null && effectiveFolder == prefixFolder
    val manualTime = quickTime != null || customDue != null
    val usesDetection = detected?.type != null && !manualTime &&
        (selectedType == null || selectedType == detected.type)
    val reminderDue = if (isReminder) quickTime?.toMillis() ?: customDue ?: detectedDue else null
    val shownType = effectiveType ?: ItemType.NOTE
    val canHavePriority = shownType != ItemType.NOTE
    val priority = if (canHavePriority) manualPriority ?: detectedPriority ?: 0 else 0
    // Si se usó la importancia escrita ("urgente", "!!!"), esas palabras no quedan en el texto.
    val usesPriorityWords = canHavePriority && manualPriority == null && detectedPriority != null

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun paste() {
        when (val clip = ClipboardReader.read(context)) {
            null -> Toast.makeText(context, "No hay nada copiado", Toast.LENGTH_SHORT).show()
            is ClipboardReader.Clip.Text ->
                text = if (text.isBlank()) clip.text else text.trimEnd() + "\n" + clip.text
            is ClipboardReader.Clip.Image -> scope.launch {
                val imported = ImageStore.import(context, clip.uri)
                if (imported != null) onImageChange(imported)
                else Toast.makeText(context, "No se pudo pegar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            // El borde toma el color del tipo elegido: se ve de un vistazo qué se guarda.
            border = BorderStroke(2.dp, shownType.color),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                .fillMaxWidth()
                // Con el calendario abierto el panel crece: que no se salga de la pantalla.
                .heightIn(max = maxPanelHeight)
                // Evita que un toque dentro del panel llegue al fondo y lo cierre.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Primero: qué se va a guardar. Separado del resto para no confundir.
                TypeSelector(selected = shownType, onSelect = { selectedType = it })

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(shownType.prompt) },
                    placeholder = { Text("Escribe, pega un link o agrega una imagen…") },
                    minLines = 2,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                // Pegar lo copiado o adjuntar una imagen: guardar links, videos y capturas al toque.
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = ::paste,
                        label = { Text("📋 Pegar" + (clipKind?.let { " ${it.label}" } ?: "")) },
                    )
                    AssistChip(
                        onClick = { onPickImage(text) },
                        label = { Text(if (imagePath == null) "🖼️ Imagen" else "🖼️ Cambiar imagen") },
                    )
                }

                if (imagePath != null) {
                    Box {
                        ItemImage(
                            path = imagePath,
                            maxSide = 720,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                        )
                        TextButton(
                            onClick = { onImageChange(null) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) { Text("Quitar ✕") }
                    }
                }

                // Lo que Hilvana entendió; tocarlo lo descarta y se guarda el texto tal cual.
                if (detected != null) {
                    InputChip(
                        selected = true,
                        onClick = { ignoreDetection = true },
                        label = {
                            Text(
                                text = buildString {
                                    append("✨ ")
                                    if (detectedPriority != null) append(starsText(detectedPriority)).append(" ")
                                    if (detectedDue != null) {
                                        append("⏰ ").append(formatDue(detectedDue))
                                        if (detected.text.isNotBlank()) append(" · ").append(detected.text)
                                    } else if (prefixFolder != null) {
                                        append(prefixFolder.label)
                                        if (detected.textAfterPrefix.isNotBlank()) {
                                            append(" · ").append(detected.textAfterPrefix)
                                        }
                                    } else if (detected.text.isNotBlank()) {
                                        append(detected.text)
                                    }
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

                // Lo propio de cada tipo: carpeta (Nota), aviso (Tarea) o cuándo (Recordatorio).
                if (isNote && folders.isNotEmpty()) {
                    Text("Carpeta", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        folders.forEach { folder ->
                            FilterChip(
                                selected = effectiveFolder?.id == folder.id,
                                onClick = {
                                    selectedFolderId = if (effectiveFolder?.id == folder.id) null else folder.id
                                },
                                label = { Text(folder.label) },
                            )
                        }
                    }
                }

                if (canHavePriority) {
                    Text("Importancia", style = MaterialTheme.typography.labelLarge)
                    StarRating(priority = priority, onChange = { manualPriority = it })
                }

                if (shownType == ItemType.TASK) {
                    Text(
                        "Aparecerá en Tareas para hacerla cuando puedas. Si necesitas que suene a una " +
                            "hora, elige ⏰ Recordatorio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isReminder) {
                    ReminderPicker(
                        dueAt = reminderDue,
                        selectedQuick = quickTime,
                        onQuick = {
                            quickTime = it
                            customDue = null
                        },
                        onCustom = {
                            customDue = it
                            quickTime = null
                        },
                        // Esconde el teclado para que se vea el calendario.
                        onOpenCalendar = { focusManager.clearFocus() },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val finalText = when {
                                usesPrefix -> parsed.textAfterPrefix
                                usesDetection || usesPriorityWords -> parsed.text
                                else -> text.trim()
                            }.ifBlank { text.trim() }.ifBlank { "📷 Imagen" }
                            onSave(finalText, shownType, reminderDue, effectiveFolder?.id, priority)
                        },
                        // Con imagen se puede guardar sin texto; un recordatorio necesita hora.
                        enabled = (text.isNotBlank() || imagePath != null) && (!isReminder || reminderDue != null),
                    ) { Text("Guardar") }
                }
            }
        }
    }
}
