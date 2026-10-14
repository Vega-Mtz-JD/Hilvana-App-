package com.hilvana.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * - Nota: guardar algo para no olvidarlo (se organiza en carpetas).
 * - Tarea: algo que hay que hacer, sin importar cuándo.
 * - Recordatorio: algo que hay que hacer en un día y hora concretos (suena).
 */
enum class ItemType(val label: String) {
    NOTE("Nota"),
    TASK("Tarea"),
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
    /** Carpeta de la nota (solo para Notas); null = sin carpeta. */
    val folderId: Long? = null,
    /** Imagen adjunta guardada por [ImageStore] (foto o captura), si tiene. */
    val imagePath: String? = null,
    /** Importancia de 0 (sin marcar) a 5 estrellas. */
    @ColumnInfo(defaultValue = "0") val priority: Int = 0,
    /** Cuándo se marcó como hecho (para las estadísticas); null si está pendiente. */
    val completedAt: Long? = null,
)

const val MAX_PRIORITY = 5
