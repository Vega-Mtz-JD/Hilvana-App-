package com.hilvana.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.hilvana.app.MainActivity
import com.hilvana.app.capture.CaptureActivity
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.database
import com.hilvana.app.repository
import com.hilvana.app.ui.addDays
import com.hilvana.app.ui.formatDue
import com.hilvana.app.ui.startOfDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Widget "Mi día" (plan §5): lo atrasado y lo de hoy, con casillas para marcar
 * como hecho sin abrir la app. Debajo, las tareas sin fecha que siguen pendientes.
 */
class MyDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = context.repository.items
        // Con la app bloqueada, la pantalla de inicio no muestra el texto: solo cuántos hay.
        val hideText = UserPrefs(context).appLockEnabled
        val goal = UserPrefs(context).dailyGoal
        val dynamicColor = UserPrefs(context).dynamicColor
        provideContent {
            val all by items.collectAsState(initial = emptyList())
            val startOfToday = startOfDay(System.currentTimeMillis())
            val completedToday = all.count { (it.completedAt ?: 0L) >= startOfToday }
            HilvanaWidgetTheme(dynamicColor = dynamicColor) { MyDayContent(myDayItems(all), hideText, goal, completedToday) }
        }
    }
}

/** Pendientes con fecha hasta hoy (incluye atrasados) primero; luego tareas sin fecha. */
private fun myDayItems(items: List<Item>, now: Long = System.currentTimeMillis()): List<Item> {
    val tomorrow = addDays(startOfDay(now), 1)
    val pending = items.filter { !it.done && it.type != ItemType.NOTE }
    val dated = pending.filter { it.dueAt != null && it.dueAt < tomorrow }.sortedBy { it.dueAt }
    val undatedTasks = pending.filter { it.dueAt == null && it.type == ItemType.TASK }
    return dated + undatedTasks
}

@Composable
private fun MyDayContent(items: List<Item>, hideText: Boolean, goal: Int, completedToday: Int) {
    val context = LocalContext.current
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es")))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(
                    "Mi día",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                )
                Text(
                    today + when {
                        goal <= 0 -> ""
                        completedToday >= goal -> " · 🎉 meta $completedToday/$goal"
                        else -> " · 🎯 $completedToday/$goal"
                    },
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
            Box(
                modifier = GlanceModifier
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clickable(actionStartActivity(CaptureActivity.intent(context))),
            ) {
                Text(
                    "＋",
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onPrimaryContainer),
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))

        if (hideText && items.isNotEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "🔒 " + if (items.size == 1) "1 pendiente" else "${items.size} pendientes",
                    style = TextStyle(fontSize = 15.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Nada pendiente hoy 🎉",
                    style = TextStyle(fontSize = 15.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(items, itemId = { it.id }) { item -> MyDayRow(item) }
            }
        }
    }
}

@Composable
private fun MyDayRow(item: Item) {
    val overdue = item.dueAt != null && item.dueAt < System.currentTimeMillis()
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBox(
            checked = item.done,
            onCheckedChange = actionRunCallback<ToggleDoneAction>(actionParametersOf(ItemIdKey to item.id)),
        )
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                item.text,
                maxLines = 2,
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
            )
            item.dueAt?.let {
                Text(
                    (if (overdue) "⚠️ " else "⏰ ") + formatDue(it),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

private val ItemIdKey = ActionParameters.Key<Long>("item_id")

/** Marca/desmarca como hecho desde el widget (cancela la alarma si la tenía). */
class ToggleDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ItemIdKey] ?: return
        val item = context.database.itemDao().getById(id) ?: return
        context.repository.save(item.copy(done = !item.done))
    }
}

class MyDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyDayWidget()
}
