package com.hilvana.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.ItemType

/** Íconos de Hilvana (Material, contorno) en lugar de emojis: se ven igual en todos los teléfonos. */
object HilvanaIcons {
    val Note = Icons.AutoMirrored.Outlined.StickyNote2
    val Task = Icons.Outlined.TaskAlt
    val Reminder = Icons.Outlined.Alarm
    val Priority = Icons.Outlined.Flag
    val Repeat = Icons.Outlined.Repeat
    val Checklist = Icons.Outlined.Checklist
    val Link = Icons.Outlined.Link
    val Image = Icons.Outlined.Image
    val VoiceNote = Icons.Outlined.Mic
    val Dictate = Icons.Outlined.RecordVoiceOver
    val Paste = Icons.Outlined.ContentPaste
    val Calendar = Icons.Outlined.CalendarMonth
    val Lock = Icons.Outlined.Lock
    val Warning = Icons.Outlined.WarningAmber
    val Cloud = Icons.Outlined.Cloud
    val Smart = Icons.Outlined.AutoAwesome
    val Goal = Icons.Outlined.TrackChanges
    val Streak = Icons.Outlined.LocalFireDepartment
    val Celebrate = Icons.Outlined.Celebration
    val Star = Icons.Outlined.StarOutline
    val Folder = Icons.Outlined.Folder
}

val ItemType.icon: ImageVector
    get() = when (this) {
        ItemType.NOTE -> HilvanaIcons.Note
        ItemType.TASK -> HilvanaIcons.Task
        ItemType.REMINDER -> HilvanaIcons.Reminder
    }

/**
 * Íconos para carpetas. En la base se guarda la clave ("lightbulb"); las carpetas de
 * versiones anteriores guardaban un emoji y se traducen aquí, sin migrar nada.
 */
object FolderIcons {
    val options: List<Pair<String, ImageVector>> = listOf(
        Folder.DEFAULT_ICON to Icons.Outlined.Folder,
        "lightbulb" to Icons.Outlined.Lightbulb,
        "key" to Icons.Outlined.Key,
        "link" to Icons.Outlined.Link,
        "movie" to Icons.Outlined.Movie,
        "psychology" to Icons.Outlined.Psychology,
        "camera" to Icons.Outlined.PhotoCamera,
        "cart" to Icons.Outlined.ShoppingCart,
        "book" to Icons.AutoMirrored.Outlined.MenuBook,
        "school" to Icons.Outlined.School,
        "work" to Icons.Outlined.WorkOutline,
        "code" to Icons.Outlined.Code,
        "home" to Icons.Outlined.Home,
        "star" to Icons.Outlined.StarOutline,
        "favorite" to Icons.Outlined.FavoriteBorder,
        "flight" to Icons.Outlined.Flight,
        "music" to Icons.Outlined.MusicNote,
        "restaurant" to Icons.Outlined.Restaurant,
        "fitness" to Icons.Outlined.FitnessCenter,
        "savings" to Icons.Outlined.Savings,
        "palette" to Icons.Outlined.Palette,
        "pets" to Icons.Outlined.Pets,
    )
    private val byKey = options.toMap()

    private val legacyEmojis = mapOf(
        "📁" to Folder.DEFAULT_ICON, "💡" to "lightbulb", "🔑" to "key", "🔗" to "link",
        "🎬" to "movie", "🧠" to "psychology", "📷" to "camera", "🛒" to "cart", "📚" to "book",
        "⭐" to "star", "❤️" to "favorite", "💼" to "work", "🏠" to "home", "✈️" to "flight",
        "🎵" to "music", "🍳" to "restaurant",
    )

    /** Clave válida para lo guardado (clave nueva o emoji viejo). */
    fun key(stored: String): String =
        stored.takeIf { it in byKey } ?: legacyEmojis[stored] ?: Folder.DEFAULT_ICON

    fun icon(stored: String): ImageVector = byKey.getValue(key(stored))
}

val Folder.iconVector: ImageVector get() = FolderIcons.icon(icon)

/** Ícono + texto en una fila (títulos de secciones, avisos…). */
@Composable
fun IconText(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = Color.Unspecified,
    iconTint: Color = if (color != Color.Unspecified) color else LocalContentColor.current,
    iconSize: Dp = 18.dp,
    maxLines: Int = Int.MAX_VALUE,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
        Text(text, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Ícono pequeño para el `leadingIcon` de un chip. */
@Composable
fun ChipIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
}
