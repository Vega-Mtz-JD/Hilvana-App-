package com.hilvana.app.ui.stats

import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatsCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 13)
    private val now = today.atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun at(date: LocalDate, hour: Int = 10) = date.atTime(hour, 0).toInstant(zone).toEpochMilli()
    private fun daysAgo(n: Long) = today.minusDays(n)

    @Test fun rachaCuentaHastaAyerSiHoyNoHayNada() {
        val days = setOf(daysAgo(1), daysAgo(2), daysAgo(3), daysAgo(5))
        assertEquals(3, StatsCalculator.currentStreak(days, today))
    }

    @Test fun rachaIncluyeHoy() {
        assertEquals(2, StatsCalculator.currentStreak(setOf(today, daysAgo(1)), today))
    }

    @Test fun rachaRotaEsCero() {
        assertEquals(0, StatsCalculator.currentStreak(setOf(daysAgo(2), daysAgo(3)), today))
    }

    @Test fun mejorRacha() {
        val days = setOf(daysAgo(10), daysAgo(9), daysAgo(8), daysAgo(7), daysAgo(1), today)
        assertEquals(4, StatsCalculator.bestStreak(days))
    }

    @Test fun rachaConMetaDiaria() {
        // Ayer y anteayer se completaron 3 cosas; hoy 1. Con meta 3: racha 2 (hoy aún no).
        fun done(day: LocalDate, n: Int) = (1..n).map {
            Item(text = "x", type = ItemType.TASK, createdAt = at(day), done = true, completedAt = at(day, 9 + it))
        }
        val items = done(daysAgo(2), 3) + done(daysAgo(1), 3) + done(today, 1)
        val stats = StatsCalculator.compute(items, emptyList(), now, zone, dailyGoal = 3)
        assertEquals(1, stats.completedToday)
        assertEquals(3, stats.dailyGoal)
        assertEquals(2, stats.streak)
        // Sin meta (cuenta con 1 al día): 3 días seguidos incluyendo hoy.
        assertEquals(3, StatsCalculator.compute(items, emptyList(), now, zone).streak)
    }

    @Test fun resumenGeneral() {
        val ideas = Folder(id = 1, name = "Ideas")
        val items = listOf(
            Item(id = 1, text = "nota", type = ItemType.NOTE, createdAt = at(today), folderId = 1),
            Item(id = 2, text = "nota", type = ItemType.NOTE, createdAt = at(daysAgo(1))),
            Item(id = 3, text = "tarea", type = ItemType.TASK, createdAt = at(daysAgo(2)), priority = 5),
            Item(
                id = 4, text = "hecha", type = ItemType.TASK, createdAt = at(daysAgo(3)),
                done = true, completedAt = at(today), priority = 3,
            ),
            Item(
                id = 5, text = "atrasado", type = ItemType.REMINDER, createdAt = at(daysAgo(4)),
                dueAt = at(daysAgo(1)),
            ),
        )
        val stats = StatsCalculator.compute(items, listOf(ideas), now, zone)

        assertEquals(5, stats.total)
        assertEquals(2, stats.pending) // tarea 3 y recordatorio 5
        assertEquals(1, stats.overdue)
        assertEquals(1, stats.completedLast7Days)
        assertEquals(1, stats.streak)
        assertEquals(mapOf(ItemType.NOTE to 2, ItemType.TASK to 2, ItemType.REMINDER to 1), stats.byType)
        assertEquals(listOf(1, 0, 0, 0, 0, 1), stats.pendingByPriority) // sin estrellas: 1, ★5: 1
        assertEquals(StatsCalculator.ACTIVITY_DAYS, stats.activity.size)
        assertEquals(DayActivity(today, created = 1, completed = 1), stats.activity.last())
        assertEquals(listOf(ideas to 1, null to 1).toSet(), stats.notesByFolder.toSet())
    }
}
