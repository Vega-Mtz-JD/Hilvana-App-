package com.hilvana.app.focus

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Botones de la notificación de foco y del empujoncito:
 *  - "Paso: <siguiente paso>": marca ese paso; si era el último, la tarea queda hecha.
 *  - "Hecho": completa la tarea.
 *  - (alarma) empujoncito: muestra el aviso y programa el siguiente.
 */
class FocusActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(ACTION_STEP, ACTION_DONE, ACTION_NUDGE)) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_NUDGE -> onNudge(context)
                    else -> onTaskAction(context, action, intent.getLongExtra(EXTRA_ITEM_ID, -1))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onTaskAction(context: Context, action: String, itemId: Long) {
        val item = context.database.itemDao().getById(itemId) ?: return
        FocusNotifier.cancelNudge(context)
        // Guardar vuelve a dibujar la notificación de foco, la burbuja y los widgets.
        if (action == ACTION_STEP) FocusActions.completeNextStep(context, item)
        else FocusActions.complete(context, item)
    }

    private suspend fun onNudge(context: Context) {
        val hours = UserPrefs(context).focusNudgeHours
        val pinned = context.database.itemDao().getPinned()
        if (hours <= 0 || pinned.isEmpty()) {
            FocusNudges.cancel(context)
            return
        }
        val now = System.currentTimeMillis()
        if (!FocusNudges.isQuietTime(now)) FocusNotifier.showNudge(context, pinned)
        FocusNudges.schedule(context, FocusNudges.nextTime(now, hours))
    }

    companion object {
        const val ACTION_NUDGE = "com.hilvana.app.focus.NUDGE"
        private const val ACTION_STEP = "com.hilvana.app.focus.STEP"
        private const val ACTION_DONE = "com.hilvana.app.focus.DONE"
        private const val EXTRA_ITEM_ID = "item_id"

        fun stepIntent(context: Context, itemId: Long) = taskIntent(context, itemId, ACTION_STEP, 1)
        fun doneIntent(context: Context, itemId: Long) = taskIntent(context, itemId, ACTION_DONE, 2)

        private fun taskIntent(context: Context, itemId: Long, action: String, slot: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                40_000 + itemId.toInt() * 10 + slot,
                Intent(context, FocusActionReceiver::class.java)
                    .setAction(action)
                    .putExtra(EXTRA_ITEM_ID, itemId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
