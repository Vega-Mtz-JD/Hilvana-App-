package com.hilvana.app.ui.stats

import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.data.MAX_PRIORITY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayActivity(val date: LocalDate, val created: Int, val completed: Int)

data class Stats(
    val total: Int,
    val pending: Int,
    val overdue: Int,
    val completedLast7Days: Int,
    val completedToday: Int,
    /** Meta diaria usada para la racha (al menos 1). */
    val dailyGoal: Int,
    /** Días seguidos (hasta hoy, o hasta ayer si hoy aún no) cumpliendo la meta diaria. */
    val streak: Int,
    val bestStreak: Int,
    val activity: List<DayActivity>,
    val byType: Map<ItemType, Int>,
    /** Tareas y recordatorios pendientes por estrellas (índice 0 = sin estrellas … 5). */
    val pendingByPriority: List<Int>,
    /** Notas por carpeta (null = sin carpeta), de más a menos. */
    val notesByFolder: List<Pair<Folder?, Int>>,
)

/** Cálculo de las estadísticas (sin Android, para poder probarlo en la laptop). */
object StatsCalculator {

    const val ACTIVITY_DAYS = 14

    fun compute(
        items: List<Item>,
        folders: List<Folder>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        /** Meta diaria (0 = apagada: entonces la racha cuenta días con al menos 1). */
        dailyGoal: Int = 0,
    ): Stats {
        fun Long.day(): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
        val today = now.day()

        val actionable = items.filter { it.type != ItemType.NOTE }
        val pending = actionable.filter { !it.done }
        val completionDays = items.mapNotNull { it.completedAt?.day() }

        val createdPerDay = items.groupingBy { it.createdAt.day() }.eachCount()
        val completedPerDay = completionDays.groupingBy { it }.eachCount()
        val activity = (ACTIVITY_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DayActivity(date, createdPerDay[date] ?: 0, completedPerDay[date] ?: 0)
        }

        val foldersById = folders.associateBy { it.id }
        val notesByFolder = items.filter { it.type == ItemType.NOTE }
            .groupingBy { it.folderId?.let(foldersById::get) }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        // Días que cuentan para la racha: los que llegaron a la meta.
        val goal = dailyGoal.coerceAtLeast(1)
        val goalDays = completedPerDay.filterValues { it >= goal }.keys

        return Stats(
            total = items.size,
            pending = pending.size,
            overdue = pending.count { it.dueAt != null && it.dueAt < now },
            completedLast7Days = completionDays.count { !it.isBefore(today.minusDays(6)) },
            completedToday = completedPerDay[today] ?: 0,
            dailyGoal = goal,
            streak = currentStreak(goalDays, today),
            bestStreak = bestStreak(goalDays),
            activity = activity,
            byType = ItemType.entries.associateWith { type -> items.count { it.type == type } },
            pendingByPriority = (0..MAX_PRIORITY).map { stars -> pending.count { it.priority == stars } },
            notesByFolder = notesByFolder,
        )
    }

    /** Racha actual: si hoy todavía no completaste nada, cuenta hasta ayer (no se rompe a las 8 a. m.). */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun bestStreak(days: Set<LocalDate>): Int {
        var best = 0
        days.forEach { start ->
            if (start.minusDays(1) in days) return@forEach // no es el inicio de una racha
            var length = 0
            var day = start
            while (day in days) {
                length++
                day = day.plusDays(1)
            }
            best = maxOf(best, length)
        }
        return best
    }
}
