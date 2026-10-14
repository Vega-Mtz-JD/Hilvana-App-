package com.hilvana.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hilvana.app.data.Item
import com.hilvana.app.database

/**
 * Programa los recordatorios con AlarmManager: es el sistema quien los dispara,
 * así que suenan aunque Hilvana esté cerrada.
 */
object ReminderScheduler {

    /** Android 12+ exige permiso para alarmas exactas (en 14+ viene denegado por defecto). */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    /**
     * Programa el recordatorio del elemento. Sin permiso de alarmas exactas cae en una
     * alarma inexacta (puede retrasarse varios minutos). Devuelve si quedó exacta.
     */
    fun schedule(context: Context, item: Item): Boolean {
        val at = item.dueAt ?: return false
        val manager = alarmManager(context)
        val operation = pendingIntent(context, item.id)
        return if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            true
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            false
        }
    }

    /**
     * Alarma de "Posponer" aparte de la normal: así, en algo que se repite, posponer
     * no borra la alarma de la próxima vez.
     */
    fun scheduleSnooze(context: Context, itemId: Long, at: Long) {
        val operation = PendingIntent.getBroadcast(
            context,
            itemId.toInt() + SNOOZE_REQUEST_OFFSET,
            Intent(context, ReminderReceiver::class.java)
                .putExtra(ReminderReceiver.EXTRA_ITEM_ID, itemId)
                .putExtra(ReminderReceiver.EXTRA_SNOOZE, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val manager = alarmManager(context)
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private const val SNOOZE_REQUEST_OFFSET = 500_000

    fun cancel(context: Context, itemId: Long) {
        alarmManager(context).cancel(pendingIntent(context, itemId))
    }

    /** Las alarmas se borran al reiniciar el teléfono: hay que volver a programarlas. */
    suspend fun rescheduleAll(context: Context) {
        context.database.itemDao()
            .getUpcomingReminders(System.currentTimeMillis())
            .forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, itemId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            Intent(context, ReminderReceiver::class.java).putExtra(ReminderReceiver.EXTRA_ITEM_ID, itemId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)
}
