package com.hilvana.app.parser

import com.hilvana.app.data.ItemType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Resultado de interpretar lo que escribió el usuario.
 * @param text el texto sin las palabras de fecha/hora ("Llamar a mamá").
 * @param dueAt la fecha/hora detectada, si había alguna.
 * @param type Recordatorio si se detectó fecha/hora; null si no.
 * @param prefix lo escrito antes de ":" al inicio ("links: …" → "Links"), que puede
 *   ser el nombre de una carpeta; quien llama decide si coincide con alguna.
 * @param textAfterPrefix [text] sin ese prefijo.
 */
data class ParseResult(
    val text: String,
    val dueAt: LocalDateTime?,
    val type: ItemType?,
    val prefix: String? = null,
    val textAfterPrefix: String = text,
) {
    fun dueAtMillis(zone: ZoneId = ZoneId.systemDefault()): Long? =
        dueAt?.atZone(zone)?.toInstant()?.toEpochMilli()
}

/**
 * Entiende fechas y horas escritas en español natural:
 * "mañana a las 5", "el viernes 8 de la noche", "en 10 minutos", "15/11",
 * "pasado mañana", "esta noche", "a las 18:30", "5pm", "el 15 de octubre"…
 *
 * Es Kotlin puro (sin Android) para poder probarlo en la laptop y, en el futuro,
 * reutilizarlo en iPhone con Kotlin Multiplatform (ver plan §6).
 */
object SpanishDateParser {

    // Límites de palabra que funcionan igual en Android y en la JVM con letras acentuadas.
    private const val B = "(?<![\\p{L}\\d])"
    private const val E = "(?![\\p{L}\\d])"
    private val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

    private enum class Period(val defaultTime: LocalTime) {
        MORNING(LocalTime.of(9, 0)),
        AFTERNOON(LocalTime.of(15, 0)),
        NIGHT(LocalTime.of(20, 0)),
    }

    private val NUMBER_WORDS = mapOf(
        "un" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "diez" to 10, "quince" to 15, "veinte" to 20, "treinta" to 30,
        "cuarenta" to 40,
    )

    private val MONTHS = mapOf(
        "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4, "mayo" to 5, "junio" to 6,
        "julio" to 7, "agosto" to 8, "septiembre" to 9, "setiembre" to 9, "octubre" to 10,
        "noviembre" to 11, "diciembre" to 12,
    )

    private val WEEKDAYS = mapOf(
        "lunes" to DayOfWeek.MONDAY, "martes" to DayOfWeek.TUESDAY,
        "miercoles" to DayOfWeek.WEDNESDAY, "miércoles" to DayOfWeek.WEDNESDAY,
        "jueves" to DayOfWeek.THURSDAY, "viernes" to DayOfWeek.FRIDAY,
        "sabado" to DayOfWeek.SATURDAY, "sábado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY,
    )

    private val RELATIVE = Regex(
        "$B(?:en|dentro\\s+de)\\s+(\\d+|${NUMBER_WORDS.keys.joinToString("|")}|media)\\s+" +
            "(minutos?|min|horas?|hrs?|d[ií]as?|semanas?)$E"
    )
    private val TONIGHT = Regex("${B}esta\\s+noche$E")
    private val PERIOD = Regex("$B(?:de|en|por)\\s+la\\s+(ma[nñ]ana|tarde|noche)$E")
    private val NOON = Regex("${B}al\\s+mediod[ií]a$E")
    private val TIME_MERIDIEM = Regex(
        "$B(?:a\\s+las?\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(a\\.?\\s?m\\.?|p\\.?\\s?m\\.?)(?!\\p{L})"
    )
    private val TIME_A_LAS = Regex("${B}a\\s+las?\\s+(\\d{1,2})(?::(\\d{2}))?(?:\\s*(?:hrs?|h|horas))?$E")
    private val TIME_CLOCK = Regex("$B(\\d{1,2}):(\\d{2})$E")
    private val DAY_AFTER_TOMORROW = Regex("${B}pasado\\s+ma[nñ]ana$E")
    private val TOMORROW = Regex("${B}ma[nñ]ana$E")
    private val TODAY = Regex("${B}hoy$E")
    private val WEEKDAY = Regex(
        "$B(?:el\\s+)?(?:(pr[oó]ximo|este)\\s+)?(${WEEKDAYS.keys.joinToString("|")})" +
            "(?:\\s+(?:que\\s+viene|pr[oó]ximo))?$E"
    )
    private val DAY_OF_MONTH_NAME = Regex(
        "$B(?:el\\s+)?(\\d{1,2})\\s+de\\s+(${MONTHS.keys.joinToString("|")})(?:\\s+(?:de\\s+)?(\\d{4}))?$E"
    )
    private val DAY_SLASH_MONTH = Regex("$B(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?$E")
    private val EL_DAY = Regex("${B}el\\s+(\\d{1,2})$E")
    private val URL = Regex("(?:https?://|www\\.)\\S+")

    /** "links: …", "mis ideas: …" (letras y espacios, hasta 20, antes de ":"). */
    private val PREFIX = Regex("^(\\p{L}[\\p{L} ]{0,19}?)\\s*:\\s+")

    fun parse(input: String, now: LocalDateTime = LocalDateTime.now()): ParseResult {
        val lower = input.lowercase()
        val taken = mutableListOf<IntRange>()
        // Los enlaces nunca se interpretan: ".../video/12/10" no es una fecha.
        val links = URL.findAll(lower).map { it.range }.toList()

        /** Primera coincidencia que no se cruce con algo ya interpretado ni con un enlace. */
        fun Regex.findFree(): MatchResult? =
            findAll(lower).firstOrNull { m -> (taken + links).none { it.overlaps(m.range) } }
                ?.also { taken += it.range }

        // 1) "en 10 minutos", "dentro de 2 horas": se calcula desde ahora y listo.
        RELATIVE.findFree()?.let { m ->
            val dueAt = relative(now, m.groupValues[1], m.groupValues[2])
            if (dueAt != null) return result(input, taken, dueAt)
            taken.remove(m.range)
        }

        var date: LocalDate? = null
        var period: Period? = null
        var hour: Int? = null
        var minute = 0
        var explicitMeridiem = false

        // 2) Partes del día ("esta noche", "en la tarde") antes que "mañana" (día).
        TONIGHT.findFree()?.let {
            date = now.toLocalDate()
            period = Period.NIGHT
        }
        if (period == null) {
            PERIOD.findFree()?.let { m ->
                period = when (m.groupValues[1]) {
                    "tarde" -> Period.AFTERNOON
                    "noche" -> Period.NIGHT
                    else -> Period.MORNING
                }
            }
        }

        // 3) Hora: "al mediodía", "5pm", "a las 5:30", "18:30".
        NOON.findFree()?.let { hour = 12 }
        if (hour == null) {
            TIME_MERIDIEM.findFree()?.let { m ->
                val h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toIntOrNull() ?: 0
                if (h in 1..12 && min in 0..59) {
                    val pm = m.groupValues[3].startsWith("p")
                    hour = when {
                        pm && h < 12 -> h + 12
                        !pm && h == 12 -> 0
                        else -> h
                    }
                    minute = min
                    explicitMeridiem = true
                } else {
                    taken.remove(m.range)
                }
            }
        }
        if (hour == null) {
            (TIME_A_LAS.findFree() ?: TIME_CLOCK.findFree())?.let { m ->
                val h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toIntOrNull() ?: 0
                if (h in 0..23 && min in 0..59) {
                    hour = h
                    minute = min
                } else {
                    taken.remove(m.range)
                }
            }
        }

        // 4) Día.
        val today = now.toLocalDate()
        if (date == null) DAY_AFTER_TOMORROW.findFree()?.let { date = today.plusDays(2) }
        if (date == null) TOMORROW.findFree()?.let { date = today.plusDays(1) }
        if (date == null) TODAY.findFree()?.let { date = today }
        if (date == null) {
            WEEKDAY.findFree()?.let { m ->
                val day = WEEKDAYS.getValue(m.groupValues[2])
                val thisWeek = m.groupValues[1] == "este"
                date = if (thisWeek && today.dayOfWeek == day) today
                else today.with(TemporalAdjusters.next(day))
            }
        }
        if (date == null) {
            DAY_OF_MONTH_NAME.findFree()?.let { m ->
                date = dateOf(
                    today,
                    day = m.groupValues[1].toInt(),
                    month = MONTHS.getValue(m.groupValues[2]),
                    year = m.groupValues[3].toIntOrNull(),
                ) ?: run { taken.remove(m.range); null }
            }
        }
        if (date == null) {
            DAY_SLASH_MONTH.findFree()?.let { m ->
                val year = m.groupValues[3].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
                date = dateOf(today, m.groupValues[1].toInt(), m.groupValues[2].toInt(), year)
                    ?: run { taken.remove(m.range); null }
            }
        }
        if (date == null) {
            EL_DAY.findFree()?.let { m ->
                val day = m.groupValues[1].toInt()
                val thisMonth = runCatching { today.withDayOfMonth(day) }.getOrNull()
                date = when {
                    thisMonth != null && !thisMonth.isBefore(today) -> thisMonth
                    else -> runCatching { today.plusMonths(1).withDayOfMonth(day) }.getOrNull()
                } ?: run { taken.remove(m.range); null }
            }
        }

        if (date == null && hour == null && period == null) {
            return result(input, taken, null)
        }

        // 5) Combinar. "a las 5" sin am/pm: de 1 a 7 se entiende de la tarde.
        val time = hour?.let { h ->
            val adjusted = when {
                explicitMeridiem -> h
                period == Period.AFTERNOON || period == Period.NIGHT -> if (h < 12) h + 12 else h
                period == null && h in 1..7 -> h + 12
                else -> h
            }
            LocalTime.of(adjusted, minute)
        } ?: period?.defaultTime ?: DEFAULT_TIME

        var dueAt = LocalDateTime.of(date ?: today, time)
        // Solo hora y ya pasó: se entiende mañana.
        if (date == null && !dueAt.isAfter(now)) dueAt = dueAt.plusDays(1)

        return result(input, taken, dueAt)
    }

    private fun relative(now: LocalDateTime, amountText: String, unit: String): LocalDateTime? {
        if (amountText == "media") {
            return if (unit.startsWith("hora") || unit.startsWith("hr")) now.plusMinutes(30) else null
        }
        val amount = (amountText.toLongOrNull() ?: NUMBER_WORDS[amountText]?.toLong()) ?: return null
        return when {
            unit.startsWith("min") -> now.plusMinutes(amount)
            unit.startsWith("hora") || unit.startsWith("hr") -> now.plusHours(amount)
            unit.startsWith("d") -> now.plusDays(amount)
            unit.startsWith("semana") -> now.plusWeeks(amount)
            else -> null
        }
    }

    /** Fecha sin año: si ya pasó este año, se entiende el próximo. */
    private fun dateOf(today: LocalDate, day: Int, month: Int, year: Int?): LocalDate? {
        val candidate = runCatching { LocalDate.of(year ?: today.year, month, day) }.getOrNull()
            ?: return null
        return if (year == null && candidate.isBefore(today)) candidate.plusYears(1) else candidate
    }

    private fun result(input: String, taken: List<IntRange>, dueAt: LocalDateTime?): ParseResult {
        val builder = StringBuilder(input)
        taken.sortedByDescending { it.first }.forEach { builder.delete(it.first, it.last + 1) }
        val text = tidy(builder.toString())
        val prefixMatch = PREFIX.find(text)
        return ParseResult(
            text = text,
            dueAt = dueAt,
            type = if (dueAt != null) ItemType.REMINDER else null,
            prefix = prefixMatch?.groupValues?.get(1)?.trim(),
            textAfterPrefix = prefixMatch?.let { tidy(text.substring(it.range.last + 1)) } ?: text,
        )
    }

    private fun tidy(text: String): String = text
        .replace(Regex("\\s+"), " ")
        .trim(' ', ',', '.', ':', ';', '-')
        // Mayúscula inicial, salvo que empiece con un enlace ("Https://" no abriría).
        .let { if (URL.find(it)?.range?.first == 0) it else it.replaceFirstChar { c -> c.uppercase() } }

    private fun IntRange.overlaps(other: IntRange): Boolean =
        first <= other.last && other.first <= last
}
