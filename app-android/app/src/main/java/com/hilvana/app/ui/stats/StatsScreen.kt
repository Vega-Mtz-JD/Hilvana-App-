package com.hilvana.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.data.MAX_PRIORITY
import com.hilvana.app.ui.components.color
import com.hilvana.app.ui.components.emoji
import com.hilvana.app.ui.components.priorityName
import com.hilvana.app.ui.components.starsText
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SPANISH = Locale.forLanguageTag("es")

/**
 * Colores de los gráficos (guía de visualización): un solo tono (violeta) para
 * magnitudes y lo "completado"; gris para lo que es contexto ("creados"); los
 * tipos usan sus colores de la app. El texto nunca va del color de la serie.
 */
private object ChartColors {
    val accent @Composable get() = if (isSystemInDarkTheme()) Color(0xFF9085E9) else Color(0xFF6250D6)
    val context @Composable get() = Color(0xFF898781)
    val grid @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2C2C2A) else Color(0xFFE1E0D9)
    val baseline @Composable get() = if (isSystemInDarkTheme()) Color(0xFF383835) else Color(0xFFC3C2B7)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(items: List<Item>, folders: List<Folder>, dailyGoal: Int, onBack: () -> Unit) {
    val stats = remember(items, folders, dailyGoal) { StatsCalculator.compute(items, folders, dailyGoal = dailyGoal) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KpiGrid(stats)
            ChartCard("Actividad · últimos ${StatsCalculator.ACTIVITY_DAYS} días") { ActivityChart(stats.activity) }
            ChartCard("Qué guardas") { TypeBreakdown(stats.byType) }
            ChartCard("Pendientes por importancia") {
                val rows = (MAX_PRIORITY downTo 0).map { stars ->
                    BarRow(
                        label = if (stars == 0) "Sin estrellas" else "${starsText(stars)} ${priorityName(stars)}",
                        value = stats.pendingByPriority[stars],
                    )
                }
                if (stats.pending == 0) EmptyChart("No tienes tareas ni recordatorios pendientes 🎉")
                else HorizontalBars(rows)
            }
            ChartCard("Notas por carpeta") {
                if (stats.notesByFolder.isEmpty()) {
                    EmptyChart("Aún no tienes notas")
                } else {
                    // Máximo 7 barras: el resto se junta en "Otras".
                    val top = stats.notesByFolder.take(7)
                    val others = stats.notesByFolder.drop(7).sumOf { it.second }
                    val rows = top.map { (folder, count) -> BarRow(folder?.label ?: "Sin carpeta", count) } +
                        if (others > 0) listOf(BarRow("Otras", others)) else emptyList()
                    HorizontalBars(rows)
                }
            }
        }
    }
}

// ---------- Tarjetas de resumen ----------

@Composable
private fun KpiGrid(stats: Stats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("📦", "Guardados", "${stats.total}", null, Modifier.weight(1f))
            StatTile("⏳", "Pendientes", "${stats.pending}", null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("✅", "Completados", "${stats.completedLast7Days}", "últimos 7 días", Modifier.weight(1f))
            StatTile(
                "🔥", "Racha", "${stats.streak} ${if (stats.streak == 1) "día" else "días"}",
                "meta ${stats.dailyGoal}/día · mejor: ${stats.bestStreak}", Modifier.weight(1f),
            )
        }
        if (stats.overdue > 0) {
            // Estado "atrasado": siempre con ícono y texto, nunca solo color.
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "⚠️ ${stats.overdue} ${if (stats.overdue == 1) "pendiente atrasado" else "pendientes atrasados"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun StatTile(icon: String, label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text("$icon $label", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun EmptyChart(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Swatch(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

// ---------- Actividad: columnas agrupadas (creados vs completados) ----------

@Composable
private fun ActivityChart(days: List<DayActivity>) {
    var selected by remember(days) { mutableIntStateOf(days.lastIndex) }
    val createdColor = ChartColors.context
    val completedColor = ChartColors.accent
    val gridColor = ChartColors.grid
    val baselineColor = ChartColors.baseline
    val maxValue = days.maxOf { maxOf(it.created, it.completed) }.coerceAtLeast(1)
    val day = days[selected]

    // Detalle del día tocado (la "etiqueta flotante" del gráfico).
    Text(
        day.date.format(DateTimeFormatter.ofPattern("EEEE d MMM", SPANISH)).replaceFirstChar { it.uppercase() } +
            " · ${day.created} creados · ${day.completed} completados",
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Swatch(createdColor)
        Text(" Creados", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(8.dp))
        Swatch(completedColor)
        Text(" Completados", style = MaterialTheme.typography.labelSmall)
    }

    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .height(140.dp)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisLabel("$maxValue")
            AxisLabel("0")
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(140.dp)
                .pointerInput(days) {
                    // Toque en cualquier parte de la columna del día (más grande que la barra).
                    detectTapGestures { offset ->
                        selected = ((offset.x / size.width) * days.size).toInt().coerceIn(0, days.lastIndex)
                    }
                },
        ) {
            val slot = size.width / days.size
            val gap = 2.dp.toPx()
            val barWidth = ((slot * 0.7f) - gap) / 2
            val radius = 4.dp.toPx()

            // Rejilla tenue: tope y mitad; línea base más marcada.
            listOf(0f, 0.5f).forEach { f ->
                drawLine(gridColor, Offset(0f, size.height * f), Offset(size.width, size.height * f), 1.dp.toPx())
            }
            days.forEachIndexed { i, d ->
                val groupLeft = i * slot + (slot - (barWidth * 2 + gap)) / 2
                if (i == selected) {
                    drawRect(gridColor.copy(alpha = 0.6f), Offset(i * slot, 0f), Size(slot, size.height))
                }
                drawBar(groupLeft, barWidth, d.created, maxValue, radius, createdColor)
                drawBar(groupLeft + barWidth + gap, barWidth, d.completed, maxValue, radius, completedColor)
            }
            drawLine(baselineColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
    }
    // Eje X: un día sí y otro no, y "Hoy" al final.
    Row(Modifier.padding(start = 20.dp)) {
        days.forEachIndexed { i, d ->
            Text(
                when {
                    i == days.lastIndex -> "Hoy"
                    (days.lastIndex - i) % 2 == 0 -> "${d.date.dayOfMonth}"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Barra con esquinas redondeadas solo arriba (anclada a la línea base). */
private fun DrawScope.drawBar(left: Float, width: Float, value: Int, max: Int, radius: Float, color: Color) {
    if (value <= 0) return
    val height = (value.toFloat() / max) * size.height
    val top = size.height - height
    val r = minOf(radius, height, width / 2)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left, top = top, right = left + width, bottom = size.height,
                topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius(r),
            )
        )
    }
    drawPath(path, color)
}

// ---------- Qué guardas: barra apilada por tipo ----------

@Composable
private fun TypeBreakdown(byType: Map<ItemType, Int>) {
    val total = byType.values.sum()
    if (total == 0) {
        EmptyChart("Aún no has guardado nada")
        return
    }
    val types = ItemType.entries.filter { (byType[it] ?: 0) > 0 }
    // Segmentos separados por 2 dp (el "espaciador" entre rellenos).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        types.forEach { type ->
            Box(
                Modifier
                    .weight((byType[type] ?: 0).toFloat())
                    .fillMaxHeight()
                    .background(type.color)
            )
        }
    }
    // Leyenda con número y porcentaje (también sirve de tabla).
    ItemType.entries.forEach { type ->
        val count = byType[type] ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Swatch(type.color)
            Text(
                " ${type.emoji} ${type.label}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$count · ${count * 100 / total}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------- Barras horizontales (un solo tono) ----------

private data class BarRow(val label: String, val value: Int)

@Composable
private fun HorizontalBars(rows: List<BarRow>) {
    val max = rows.maxOf { it.value }.coerceAtLeast(1)
    val color = ChartColors.accent
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(130.dp),
                )
                Box(Modifier.weight(1f)) {
                    if (row.value > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(row.value.toFloat() / max)
                                .height(14.dp)
                                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                .background(color)
                        )
                    }
                }
                Text(
                    "${row.value}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(32.dp),
                )
            }
        }
    }
}
