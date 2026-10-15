package com.hilvana.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
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
import com.hilvana.app.data.progressText
import com.hilvana.app.ui.search.SearchEngine
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.hilvana.app.ui.formatDue
import com.hilvana.app.ui.isActionable

/**
 * Tarjeta de un elemento. Arriba: casilla (tareas y recordatorios), texto e
 * información; debajo y a lo ancho: imagen, vista previa del enlace y audio.
 */
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDone: (() -> Unit)? = null,
    folder: Folder? = null,
    /** Palabras a resaltar en el texto (resultados de búsqueda). */
    highlight: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val link = remember(item.text) { LINK.find(item.text)?.value }
    val hasPreview = item.linkTitle != null || item.linkImage != null
    // Con vista previa, la URL larga no aporta: se muestra solo el resto del texto.
    val shownText = remember(item.text, hasPreview) {
        if (hasPreview && link != null) item.text.replace(link, "").trim() else item.text
    }
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val displayText = remember(shownText, highlight) {
        buildAnnotatedString {
            append(shownText)
            if (!highlight.isNullOrBlank()) {
                SearchEngine.highlightRanges(shownText, highlight).forEach { range ->
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, background = highlightColor),
                        range.first,
                        range.last + 1,
                    )
                }
            }
        }
    }
    val showCheckbox = item.type.isActionable && onToggleDone != null
    val uriHandler = LocalUriHandler.current
    val openLink = {
        link?.let { runCatching { uriHandler.openUri(if (it.startsWith("www.")) "https://$it" else it) } }
        Unit
    }

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
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
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (shownText.isNotBlank()) {
                    Text(
                        text = displayText,
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
                }
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
                            if (item.pinned && !item.done) append("🎯 ")
                            append(folder?.label ?: "${item.type.emoji} ${item.type.label}")
                            if (item.checklist.isNotEmpty()) append(" · ☑ ").append(item.checklist.progressText)
                            item.dueAt?.let { append(" · ").append(formatDue(it)) }
                            item.repeatRule?.let { append(" · 🔁 ").append(it.label) }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) trailing() else Spacer(Modifier.width(16.dp))
        }

        // Adjuntos a lo ancho de la tarjeta.
        val hasAttachments = item.imagePath != null || hasPreview || link != null || item.audioPath != null
        if (hasAttachments) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.imagePath?.let { path ->
                    ItemImage(
                        path = path,
                        maxSide = 720,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                    )
                }
                when {
                    hasPreview -> LinkPreviewRow(item, onOpen = openLink)
                    link != null -> TextButton(onClick = openLink) { Text("🔗 Abrir enlace") }
                }
                item.audioPath?.let { path ->
                    AudioPlayer(path = path, durationMs = item.audioDurationMs)
                }
            }
        }
    }
}

private val LINK = Regex("(?:https?://|www\\.)\\S+")

/** Vista previa del enlace (como en WhatsApp): miniatura, título y sitio. Tocarla abre el enlace. */
@Composable
private fun LinkPreviewRow(item: Item, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item.linkImage?.let { path ->
            ItemImage(path = path, maxSide = 240, modifier = Modifier.size(76.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            item.linkTitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "🔗 " + (item.linkSite ?: "Abrir enlace") + "  ↗",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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
