package com.hilvana.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** El tipo solo cambia cómo se muestra un elemento; se puede convertir con un toque. */
enum class ItemType(val label: String) {
    NOTE("Nota"),
    TASK("Tarea"),
    IDEA("Idea"),
    REMINDER("Recordatorio"),
}

/**
 * Todo en Hilvana es un "Elemento" con campos opcionales (ver docs/plan-app.md §4).
 * Lo capturado desde la burbuja cae primero en la Bandeja (`inInbox = true`).
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val type: ItemType = ItemType.NOTE,
    val inInbox: Boolean = true,
    val done: Boolean = false,
    /** Fecha/hora del recordatorio en milisegundos, si la tiene. */
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
