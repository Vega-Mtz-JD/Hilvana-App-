package com.hilvana.app.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.hilvana.app.database
import com.hilvana.app.repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Botones de la notificación del recordatorio: "Posponer 15 min" y "Hecho".
 * (Que no se pueda posponer es de las quejas más comunes en apps de recordatorios.)
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        val action = intent.action
        if (itemId < 0 || action !in setOf(ACTION_SNOOZE, ACTION_DONE)) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationManagerCompat.from(context).cancel(ReminderReceiver.notificationId(itemId))
                val item = context.database.itemDao().getById(itemId) ?: return@launch
                val updated = when (action) {
                    ACTION_SNOOZE -> item.copy(dueAt = System.currentTimeMillis() + SNOOZE_MS, done = false)
                    else -> item.copy(done = true)
                }
                // El repositorio reprograma (posponer) o cancela (hecho) la alarma.
                context.repository.save(updated)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_SNOOZE = "com.hilvana.app.reminder.SNOOZE"
        private const val ACTION_DONE = "com.hilvana.app.reminder.DONE"
        private const val EXTRA_ITEM_ID = "item_id"
        private const val SNOOZE_MS = 15 * 60_000L

        fun snoozeIntent(context: Context, itemId: Long) = pendingIntent(context, itemId, ACTION_SNOOZE, 1)
        fun doneIntent(context: Context, itemId: Long) = pendingIntent(context, itemId, ACTION_DONE, 2)

        private fun pendingIntent(context: Context, itemId: Long, action: String, slot: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                // Un código distinto por elemento y botón para que no se pisen entre sí.
                itemId.toInt() * 10 + slot,
                Intent(context, ReminderActionReceiver::class.java)
                    .setAction(action)
                    .putExtra(EXTRA_ITEM_ID, itemId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
