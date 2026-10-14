package com.hilvana.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RepeatRuleTest {

    private fun at(month: Int, day: Int, hour: Int = 8, year: Int = 2026) = LocalDateTime.of(year, month, day, hour, 0)

    @Test fun diario() = assertEquals(at(10, 14), RepeatRule.DAILY.nextAfter(at(10, 13)))

    @Test fun deLunesAViernesSaltaElFinDeSemana() {
        // Viernes 16 → lunes 19
        assertEquals(at(10, 19), RepeatRule.WEEKDAYS.nextAfter(at(10, 16)))
    }

    @Test fun mensualDel31AFebrero() =
        assertEquals(at(2, 28, year = 2027), RepeatRule.MONTHLY.nextAfter(at(1, 31, year = 2027)))

    @Test fun siSeAtrasoNoDejaFechasPasadas() {
        // Diario a las 8 que venció hace 3 días; ahora son las 10 del 13 → mañana 14 a las 8.
        assertEquals(at(10, 14), RepeatRule.DAILY.nextFuture(at(10, 10), now = at(10, 13, hour = 10)))
    }

    @Test fun ocurrenciasParaElCalendario() {
        val days = RepeatRule.WEEKLY.occurrences(at(10, 12), until = LocalDate.of(2026, 11, 2))
        assertEquals(
            listOf(12, 19, 26).map { LocalDate.of(2026, 10, it) } + LocalDate.of(2026, 11, 2),
            days,
        )
    }
}
