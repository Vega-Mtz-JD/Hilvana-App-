package com.hilvana.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.components.EmptyState
import com.hilvana.app.ui.components.ItemCard
import com.hilvana.app.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SPANISH = Locale.forLanguageTag("es")
private val WEEKDAY_INITIALS = listOf("L", "M", "X", "J", "V", "S", "D")

/** Día (local) en que vence un elemento, o null si no tiene fecha. */
fun Item.dueDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

/**
 * Elementos por día. Lo que se repite aparece también en sus próximas fechas
 * (hasta un año), aunque en la base de datos solo se guarda la siguiente.
 */
private fun itemsByDate(items: List<Item>, zone: ZoneId = ZoneId.systemDefault()): Map<LocalDate, List<Item>> {
    val horizon = LocalDate.now(zone).plusYears(1)
    val map = mutableMapOf<LocalDate, MutableList<Item>>()
    items.filter { it.dueAt != null && it.type != ItemType.NOTE }.forEach { item ->
        val rule = item.repeatRule
        val days = if (rule != null && !item.done) {
            val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(item.dueAt!!), zone)
            rule.occurrences(start, until = horizon, max = 400)
        } else {
            listOf(item.dueDate(zone)!!)
        }
        days.forEach { map.getOrPut(it) { mutableListOf() } += item }
    }
    return map
}

/**
 * Calendario del mes: los días con recordatorios o tareas se colorean y, al tocar
 * un día, debajo se ve lo que hay en él.
 */
@Composable
fun CalendarScreen(
    items: List<Item>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onOpen: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = LocalDate.now()
    val byDate = remember(items) { itemsByDate(items) }
    val dayItems = byDate[selectedDate].orEmpty().sortedBy { it.dueAt }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            MonthHeader(
                month = month,
                onPrevious = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                onToday = {
                    month = YearMonth.from(today)
                    onSelectDate(today)
                },
            )
        }
        item {
            MonthGrid(
                month = month,
                byDate = byDate,
                selectedDate = selectedDate,
                today = today,
                onSelectDate = onSelectDate,
            )
        }
        item { Legend() }

        item {
            SectionHeader(
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH))
                    .replaceFirstChar { it.uppercase() }
            )
        }
        if (dayItems.isEmpty()) {
            item { EmptyState("Nada este día.\nToca + para agregar un recordatorio.") }
        }
        items(dayItems, key = { it.id }) { item ->
            ItemCard(item = item, onClick = { onOpen(item) }, onToggleDone = { onToggleDone(item) })
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(
            text = month.month.getDisplayName(TextStyle.FULL_STANDALONE, SPANISH)
                .replaceFirstChar { it.uppercase() } + " ${month.year}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
        TextButton(onClick = onToday) { Text("Hoy") }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    byDate: Map<LocalDate, List<Item>>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    // La semana empieza en lunes; los huecos antes del día 1 quedan vacíos.
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            WEEKDAY_INITIALS.forEach { initial ->
                Text(
                    initial,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { date ->
                    Box(Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                items = byDate[date].orEmpty(),
                                selected = date == selectedDate,
                                isToday = date == today,
                                onClick = { onSelectDate(date) },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, items: List<Item>, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hasReminder = items.any { it.type == ItemType.REMINDER }
    val hasTask = items.any { it.type == ItemType.TASK }
    // Color del día: recordatorio > tarea > nada. El día elegido siempre resalta más.
    val background = when {
        selected -> colors.primary
        hasReminder -> colors.tertiaryContainer
        hasTask -> colors.secondaryContainer
        else -> Color.Transparent
    }
    val content = if (selected) colors.onPrimary else colors.onSurface
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .then(if (isToday && !selected) Modifier.border(2.dp, colors.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || items.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
            if (hasReminder) Dot(if (selected) colors.onPrimary else colors.tertiary)
            if (hasTask) Dot(if (selected) colors.onPrimary else colors.secondary)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(colors.tertiary)
        Spacer(Modifier.width(4.dp))
        Text("Recordatorio", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(16.dp))
        Dot(colors.secondary)
        Spacer(Modifier.width(4.dp))
        Text("Tarea", style = MaterialTheme.typography.labelSmall)
    }
}
