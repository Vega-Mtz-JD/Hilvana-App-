package com.hilvana.app.ui

import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Tareas y recordatorios tienen casilla de "hecho" y pueden tener fecha. */
val ItemType.isActionable: Boolean
    get() = this == ItemType.TASK || this == ItemType.REMINDER

data class TodayGroups(
    val overdue: List<Item>,
    val today: List<Item>,
    val upcoming: List<Item>,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && today.isEmpty() && upcoming.isEmpty()
}

/** Pantalla Hoy: lo atrasado, lo de hoy y lo de los próximos 7 días. */
fun groupForToday(items: List<Item>, now: Long = System.currentTimeMillis()): TodayGroups {
    val startOfToday = startOfDay(now)
    val startOfTomorrow = addDays(startOfToday, 1)
    val endOfWeek = addDays(startOfToday, 8)
    val dated = items.filter { it.dueAt != null }.sortedBy { it.dueAt }
    return TodayGroups(
        overdue = dated.filter { !it.done && it.dueAt!! < startOfToday },
        today = dated.filter { it.dueAt!! in startOfToday until startOfTomorrow },
        upcoming = dated.filter { !it.done && it.dueAt!! in startOfTomorrow until endOfWeek },
    )
}

/** "Hoy 17:30", "Mañana 9:00", "Ayer 8:15" o "vie 16 oct 10:00". */
fun formatDue(millis: Long, now: Long = System.currentTimeMillis()): String {
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
    val today = startOfDay(now)
    return when (startOfDay(millis)) {
        today -> "Hoy $time"
        addDays(today, 1) -> "Mañana $time"
        addDays(today, -1) -> "Ayer $time"
        else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(millis)) + " $time"
    }
}

fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis
