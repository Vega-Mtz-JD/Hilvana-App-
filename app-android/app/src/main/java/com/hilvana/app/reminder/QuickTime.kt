package com.hilvana.app.reminder

import java.util.Calendar

/** Horas rápidas para recordatorios; el MVP entenderá texto como "mañana a las 5". */
enum class QuickTime(val label: String) {
    IN_1_MIN("En 1 min"),
    IN_10_MIN("En 10 min"),
    IN_1_HOUR("En 1 hora"),
    TOMORROW_9("Mañana 9:00");

    fun toMillis(now: Long = System.currentTimeMillis()): Long = when (this) {
        IN_1_MIN -> now + 60_000L
        IN_10_MIN -> now + 10 * 60_000L
        IN_1_HOUR -> now + 60 * 60_000L
        TOMORROW_9 -> Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
