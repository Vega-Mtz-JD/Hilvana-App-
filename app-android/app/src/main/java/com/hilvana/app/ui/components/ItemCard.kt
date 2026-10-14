package com.hilvana.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.ui.formatDue
import com.hilvana.app.ui.isActionable

/**
 * Tarjeta de un elemento. Tareas y recordatorios muestran una casilla para
 * marcarlos como hechos sin abrir el editor.
 */
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDone: (() -> Unit)? = null,
    folder: Folder? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val showCheckbox = item.type.isActionable && onToggleDone != null
    val link = remember(item.text) { LINK.find(item.text)?.value }
    val uriHandler = LocalUriHandler.current
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCheckbox) {
                Checkbox(checked = item.done, onCheckedChange = { onToggleDone?.invoke() })
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.priority > 0) {
                        Text(
                            starsText(item.priority) + " ",
                            style = MaterialTheme.typography.labelSmall,
                            color = StarColor,
                        )
                    }
                    Text(
                        text = buildString {
                            append(folder?.label ?: "${item.type.emoji} ${item.type.label}")
                            item.dueAt?.let { append(" · ⏰ ").append(formatDue(it)) }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item.imagePath?.let { path ->
                ItemImage(
                    path = path,
                    maxSide = 160,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(56.dp),
                )
            }
            if (link != null) {
                // Enlaces guardados (videos, artículos…): se abren con un toque.
                TextButton(onClick = {
                    val url = if (link.startsWith("www.")) "https://$link" else link
                    runCatching { uriHandler.openUri(url) }
                }) { Text("Abrir 🔗") }
            }
            if (trailing != null) trailing() else Spacer(Modifier.width(16.dp))
        }
    }
}

private val LINK = Regex("(?:https?://|www\\.)\\S+")

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Mensaje amable en lugar de una pantalla en blanco. */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
