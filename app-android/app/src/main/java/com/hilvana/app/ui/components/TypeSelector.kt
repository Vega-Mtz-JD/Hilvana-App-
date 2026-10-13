package com.hilvana.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.ItemType

/** Ícono de cada tipo, igual en toda la app. */
val ItemType.emoji: String
    get() = when (this) {
        ItemType.NOTE -> "📝"
        ItemType.TASK -> "✅"
        ItemType.REMINDER -> "⏰"
    }

/** Color de cada tipo (tonos medios: se leen en modo claro y oscuro). */
val ItemType.color: Color
    get() = when (this) {
        ItemType.NOTE -> Color(0xFFF9A825)
        ItemType.TASK -> Color(0xFF43A047)
        ItemType.REMINDER -> Color(0xFF1E88E5)
    }

/** Pregunta del campo de texto según lo que se va a guardar. */
val ItemType.prompt: String
    get() = when (this) {
        ItemType.NOTE -> "¿Qué no quieres olvidar?"
        ItemType.TASK -> "¿Qué tienes que hacer?"
        ItemType.REMINDER -> "¿Qué te recuerdo?"
    }

/**
 * Selector Nota / Tarea / Recordatorio: tres botones grandes con ícono y color.
 * El elegido se rellena con su color para que se note de un vistazo.
 */
@Composable
fun TypeSelector(
    selected: ItemType,
    onSelect: (ItemType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ItemType.entries.forEach { type ->
            val isSelected = type == selected
            val shape = RoundedCornerShape(14.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (isSelected) type.color.copy(alpha = 0.22f) else Color.Transparent)
                    .border(
                        BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) type.color else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        shape,
                    )
                    .clickable { onSelect(type) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(type.emoji, style = MaterialTheme.typography.titleLarge)
                Text(
                    type.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
