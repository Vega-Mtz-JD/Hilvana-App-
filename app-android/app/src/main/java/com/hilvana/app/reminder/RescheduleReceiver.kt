package com.hilvana.app.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hilvana.app.focus.FocusNotifier
import com.hilvana.app.focus.FocusNudges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Vuelve a programar todos los recordatorios cuando el sistema los pierde o
 * pueden haber quedado mal: reinicio del teléfono, cambio de hora o zona horaria,
 * actualización de la app, o cuando el usuario concede las alarmas exactas.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(context)
                // Al reiniciar se borran notificaciones y alarmas: volver a poner la de foco
                // y empezar de nuevo los empujoncitos.
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) FocusNudges.cancel(context)
                FocusNotifier.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
