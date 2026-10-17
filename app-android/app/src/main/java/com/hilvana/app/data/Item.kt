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
    /** Cada cuánto se repite (necesita fecha); null = no se repite. */
    val repeatRule: RepeatRule? = null,
    /** Pasos de una tarea (checklist). */
    @ColumnInfo(defaultValue = "'[]'") val checklist: List<CheckItem> = emptyList(),
    /** En prioridad: se muestra fija en notificaciones, burbuja y widget sin abrir la app. */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    /** Vista previa del primer enlace del texto: para qué enlace es, título, sitio y miniatura. */
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val linkSite: String? = null,
    val linkImage: String? = null,
    /** Nota de voz guardada por [AudioStore] y su duración. */
    val audioPath: String? = null,
    val audioDurationMs: Long? = null,
)

/** Máximo de tareas en prioridad a la vez (para que siga siendo "foco"). */
const val MAX_PINNED = 3

const val MAX_PRIORITY = 5
