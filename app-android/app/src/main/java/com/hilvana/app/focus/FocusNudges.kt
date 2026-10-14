package com.hilvana.app.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hilvana.app.data.UserPrefs
import java.util.Calendar

/**
 * Empujoncitos: cada N horas un aviso suave con la tarea en foco, nunca de noche.
 * Usa alarmas inexactas (no necesitan el permiso de alarmas exactas).
 */
object FocusNudges {

    private const val REQUEST_CODE = 30
    const val QUIET_FROM_HOUR = 22
    const val QUIET_UNTIL_HOUR = 8

    /** Programa el próximo si está activado y no hay uno pendiente. */
    fun ensureScheduled(context: Context) {
        val prefs = UserPrefs(context)
        val hours = prefs.focusNudgeHours
        if (hours <= 0) {
            cancel(context)
            return
        }
        if (prefs.nextNudgeAt > System.currentTimeMillis()) return
        schedule(context, nextTime(System.currentTimeMillis(), hours))
    }

    /** Tras cambiar la frecuencia: empezar a contar de nuevo desde ahora. */
    fun reschedule(context: Context) {
        cancel(context)
        ensureScheduled(context)
    }

    fun schedule(context: Context, at: Long) {
        UserPrefs(context).nextNudgeAt = at
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
    }

    fun cancel(context: Context) {
        UserPrefs(context).nextNudgeAt = 0
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    fun isQuietTime(millis: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_FROM_HOUR || hour < QUIET_UNTIL_HOUR
    }

    /** Dentro de [hours] horas; si cae de noche, a las 8:00. */
    fun nextTime(now: Long, hours: Int): Long {
        val candidate = now + hours * 60 * 60_000L
        if (!isQuietTime(candidate)) return candidate
        return Calendar.getInstance().apply {
            timeInMillis = candidate
            if (get(Calendar.HOUR_OF_DAY) >= QUIET_FROM_HOUR) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, QUIET_UNTIL_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, FocusActionReceiver::class.java).setAction(FocusActionReceiver.ACTION_NUDGE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
