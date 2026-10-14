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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.hilvana.app.MainActivity
import com.hilvana.app.data.Item
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.data.progressText
import com.hilvana.app.database
import com.hilvana.app.focus.FocusActions
import com.hilvana.app.repository

/**
 * Widget "🎯 En foco": la primera tarea en foco con su checklist (se marca cada paso
 * desde la pantalla de inicio) y debajo las demás tareas en foco.
 */
class FocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pinned = context.repository.pinned
        val hideText = UserPrefs(context).appLockEnabled
        provideContent {
            val tasks by pinned.collectAsState(initial = emptyList())
            GlanceTheme { FocusContent(tasks, hideText) }
        }
    }
}

private sealed interface FocusLine {
    data class Step(val task: Item, val index: Int) : FocusLine
    data class Done(val task: Item) : FocusLine
    data class Other(val task: Item) : FocusLine
}

@Composable
private fun FocusContent(tasks: List<Item>, hideText: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
    ) {
        val first = tasks.firstOrNull()
        Text(
            when {
                first == null -> "🎯 En foco"
                hideText -> "🎯 ${tasks.size} en foco"
                else -> "🎯 " + first.text
            },
            maxLines = 2,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
        )
        if (first != null && !hideText && first.checklist.isNotEmpty()) {
            Text(
                "${first.checklist.progressText} pasos",
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
        Spacer(GlanceModifier.height(6.dp))

        when {
            first == null -> Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Fija una tarea con 🎯 para verla aquí",
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
            hideText -> Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔒 Abre Hilvana para verlas", style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant))
            }
            else -> {
                // Pasos de la primera (o "Hecho" si no tiene), luego las otras tareas.
                val rows: List<FocusLine> =
                    (if (first.checklist.isEmpty()) listOf(FocusLine.Done(first))
                    else first.checklist.indices.map { FocusLine.Step(first, it) }) +
                        tasks.drop(1).map { FocusLine.Other(it) }
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(rows) { row -> FocusRow(row) }
                }
            }
        }
    }
}

@Composable
private fun FocusRow(row: FocusLine) {
    val textColor = GlanceTheme.colors.onSurface
    when (row) {
        is FocusLine.Step -> {
            val step = row.task.checklist[row.index]
            CheckBox(
                checked = step.done,
                onCheckedChange = actionRunCallback<ToggleFocusStepAction>(
                    actionParametersOf(ItemIdKey to row.task.id, StepKey to row.index)
                ),
                text = step.text,
                maxLines = 2,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = if (step.done) GlanceTheme.colors.onSurfaceVariant else textColor,
                    textDecoration = if (step.done) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
        }
        is FocusLine.Done -> CheckBox(
            checked = false,
            onCheckedChange = actionRunCallback<ToggleFocusStepAction>(
                actionParametersOf(ItemIdKey to row.task.id, StepKey to -1)
            ),
            text = "Marcar como hecha",
            style = TextStyle(fontSize = 14.sp, color = textColor),
        )
        is FocusLine.Other -> Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp).clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "🎯 " + row.task.text + if (row.task.checklist.isNotEmpty()) " · ${row.task.checklist.progressText}" else "",
                maxLines = 1,
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
            )
        }
    }
}

private val ItemIdKey = ActionParameters.Key<Long>("item_id")
private val StepKey = ActionParameters.Key<Int>("step")

/** Marca un paso (o la tarea completa si step = -1) desde el widget. */
class ToggleFocusStepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val item = context.database.itemDao().getById(parameters[ItemIdKey] ?: return) ?: return
        val step = parameters[StepKey] ?: return
        if (step < 0) FocusActions.complete(context, item) else FocusActions.toggleStep(context, item, step)
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}
