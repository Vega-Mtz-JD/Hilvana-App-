package com.hilvana.app.reminder

import java.util.Calendar

/** Atajos para recordatorios que se usan de verdad en el día a día. */
enum class QuickTime(val label: String) {
    IN_15_MIN("En 15 min"),
    IN_30_MIN("En 30 min"),
    IN_1_HOUR("En 1 hora"),
    IN_3_HOURS("En 3 horas"),
    TONIGHT("Esta noche 20:00"),
    TOMORROW_9("Mañana 9:00"),
    NEXT_WEEK("En una semana");

    fun toMillis(now: Long = System.currentTimeMillis()): Long = when (this) {
        IN_15_MIN -> now + 15 * MINUTE
        IN_30_MIN -> now + 30 * MINUTE
        IN_1_HOUR -> now + 60 * MINUTE
        IN_3_HOURS -> now + 3 * 60 * MINUTE
        TONIGHT -> atTime(now, daysAhead = 0, hour = 20)
        TOMORROW_9 -> atTime(now, daysAhead = 1, hour = 9)
        NEXT_WEEK -> atTime(now, daysAhead = 7, hour = 9)
    }

    companion object {
        private const val MINUTE = 60_000L

        /** "Esta noche" solo tiene sentido antes de las 19:00. */
        fun available(now: Long = System.currentTimeMillis()): List<QuickTime> {
            val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            return entries.filter { it != TONIGHT || hour < 19 }
        }

        private fun atTime(now: Long, daysAhead: Int, hour: Int): Long = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, daysAhead)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
