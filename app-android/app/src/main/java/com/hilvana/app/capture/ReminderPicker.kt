package com.hilvana.app.capture

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hilvana.app.reminder.QuickTime
import com.hilvana.app.ui.components.ChipIcon
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.components.IconText
import com.hilvana.app.ui.components.MonthPicker
import com.hilvana.app.ui.formatDue
import java.time.LocalDate
import java.time.ZoneId

private val HOURS = 6..23
private val MINUTES = listOf(0, 15, 30, 45)

/**
 * Cuándo avisar: atajos (15 min, 30 min, 1 h, 3 h, esta noche, mañana, en una
 * semana) o "Otro día…", que despliega el calendario del mes y la hora.
 */
@Composable
fun ReminderPicker(
    dueAt: Long?,
    selectedQuick: QuickTime?,
    onQuick: (QuickTime) -> Unit,
    onCustom: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onOpenCalendar: () -> Unit = {},
) {
    var showCalendar by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconText(
            HilvanaIcons.Reminder,
            dueAt?.let { formatDue(it) } ?: "¿Cuándo te aviso?",
            style = MaterialTheme.typography.labelLarge,
        )
        if (dueAt != null && dueAt < System.currentTimeMillis()) {
            IconText(
                HilvanaIcons.Warning,
                "Esa hora ya pasó",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickTime.available().forEach { option ->
                FilterChip(
                    selected = selectedQuick == option && !showCalendar,
                    onClick = {
                        showCalendar = false
                        onQuick(option)
                    },
                    label = { Text(option.label) },
                )
            }
            FilterChip(
                selected = showCalendar,
                onClick = {
                    showCalendar = !showCalendar
                    if (showCalendar) onOpenCalendar()
                },
                label = { Text("Otro día…") },
                leadingIcon = { ChipIcon(HilvanaIcons.Calendar) },
            )
        }

        if (showCalendar) {
            var date by remember { mutableStateOf<LocalDate?>(null) }
            var hour by remember { mutableIntStateOf(9) }
            var minute by remember { mutableIntStateOf(0) }

            // Cada cambio de día u hora actualiza el recordatorio.
            LaunchedEffect(date, hour, minute) {
                date?.let { day ->
                    onCustom(day.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                }
            }

            MonthPicker(selected = date, onSelect = { date = it }, modifier = Modifier.fillMaxWidth())
            if (date == null) {
                Text(
                    "Toca un día del calendario",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Hora", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState(initial = 0)),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HOURS.forEach { h ->
                    FilterChip(selected = hour == h, onClick = { hour = h }, label = { Text("$h") })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MINUTES.forEach { m ->
                    FilterChip(
                        selected = minute == m,
                        onClick = { minute = m },
                        label = { Text(":%02d".format(m)) },
                    )
                }
            }
        }
    }
}
