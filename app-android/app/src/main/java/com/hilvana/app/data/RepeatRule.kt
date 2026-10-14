package com.hilvana.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Próxima fecha (en milisegundos, hora local) posterior a ahora para algo que vence en [dueAt]. */
fun nextOccurrenceMillis(rule: RepeatRule, dueAt: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val from = LocalDateTime.ofInstant(Instant.ofEpochMilli(dueAt), zone)
    return rule.nextFuture(from, LocalDateTime.now(zone)).atZone(zone).toInstant().toEpochMilli()
}

/**
 * Cada cuánto se repite una tarea o recordatorio. Al marcarlo como hecho salta a
 * la siguiente fecha (ver ItemRepository).
 */
enum class RepeatRule(val label: String) {
    DAILY("Cada día"),
    WEEKDAYS("Lun a vie"),
    WEEKLY("Cada semana"),
    MONTHLY("Cada mes"),
    YEARLY("Cada año");

    /** La siguiente fecha después de [from] (un solo paso). */
    fun nextAfter(from: LocalDateTime): LocalDateTime = when (this) {
        DAILY -> from.plusDays(1)
        WEEKDAYS -> {
            var next = from.plusDays(1)
            while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                next = next.plusDays(1)
            }
            next
        }
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1) // el 31 pasa al último día de meses más cortos
        YEARLY -> from.plusYears(1)
    }

    /** La primera fecha posterior a [now] (si se atrasó varios ciclos, no deja fechas pasadas). */
    fun nextFuture(from: LocalDateTime, now: LocalDateTime): LocalDateTime {
        var next = nextAfter(from)
        while (!next.isAfter(now)) next = nextAfter(next)
        return next
    }

    /** Días en que ocurre desde [start] (incluido) hasta [until], para el calendario. */
    fun occurrences(start: LocalDateTime, until: LocalDate, max: Int = 120): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var current = start
        while (!current.toLocalDate().isAfter(until) && days.size < max) {
            days += current.toLocalDate()
            current = nextAfter(current)
        }
        return days
    }
}
