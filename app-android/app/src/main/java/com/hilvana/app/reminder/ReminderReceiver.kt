package com.hilvana.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hilvana.app.MainActivity
import com.hilvana.app.data.Item
import com.hilvana.app.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Recibe la alarma del sistema a la hora indicada y muestra la notificación. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1)
        if (itemId < 0) return

        // goAsync: nos da unos segundos extra para leer la base de datos.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = context.database.itemDao().getById(itemId)
                if (item != null && !item.done) showNotification(context, item)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, item: Item) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return

        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos a la hora que elegiste"
            }
        )

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Recordatorio")
            .setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(0, "Posponer 15 min", ReminderActionReceiver.snoozeIntent(context, item.id))
            .addAction(0, "✓ Hecho", ReminderActionReceiver.doneIntent(context, item.id))
            .build()

        try {
            notifications.notify(notificationId(item.id), notification)
        } catch (_: SecurityException) {
            // El usuario quitó el permiso de notificaciones justo ahora.
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
        private const val CHANNEL_ID = "reminders"
        // Evita chocar con el id 1 de la notificación de la burbuja.
        private const val NOTIFICATION_ID_OFFSET = 1000

        fun notificationId(itemId: Long): Int = NOTIFICATION_ID_OFFSET + itemId.toInt()
    }
}
