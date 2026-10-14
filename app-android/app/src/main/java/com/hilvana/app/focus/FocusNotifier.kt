package com.hilvana.app.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hilvana.app.MainActivity
import com.hilvana.app.data.Item
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.data.nextStep
import com.hilvana.app.data.progressText
import com.hilvana.app.database

/**
 * 🎯 Tareas en foco: una notificación fija (también en la pantalla de bloqueo) con
 * la tarea, su avance y el siguiente paso, para avanzar SIN abrir la app.
 */
object FocusNotifier {

    private const val NOTIFICATION_ID = 2 // 1 = burbuja, 1000+ = recordatorios
    private const val NUDGE_ID = 3
    private const val CHANNEL_FOCUS = "focus"
    private const val CHANNEL_NUDGE = "focus_nudge"

    /** Vuelve a dibujar la notificación fija según las tareas en foco (o la quita). */
    suspend fun refresh(context: Context) {
        val pinned = context.database.itemDao().getPinned()
        val notifications = NotificationManagerCompat.from(context)
        if (pinned.isEmpty()) {
            notifications.cancel(NOTIFICATION_ID)
            notifications.cancel(NUDGE_ID)
            FocusNudges.cancel(context)
            return
        }
        notify(context, NOTIFICATION_ID, build(context, pinned, nudge = false))
        FocusNudges.ensureScheduled(context)
    }

    /** Empujoncito: el mismo contenido, pero con sonido y como aviso emergente. */
    fun showNudge(context: Context, pinned: List<Item>) {
        if (pinned.isNotEmpty()) notify(context, NUDGE_ID, build(context, pinned, nudge = true))
    }

    fun cancelNudge(context: Context) = NotificationManagerCompat.from(context).cancel(NUDGE_ID)

    private fun notify(context: Context, id: Int, notification: Notification) {
        val notifications = NotificationManagerCompat.from(context)
        if (!notifications.areNotificationsEnabled()) return
        createChannels(context)
        try {
            notifications.notify(id, notification)
        } catch (_: SecurityException) {
            // Sin permiso de notificaciones.
        }
    }

    private fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_FOCUS, "Tareas en foco", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Tus tareas en foco, siempre a la vista"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NUDGE, "Empujoncitos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos suaves para avanzar con tus tareas en foco"
            }
        )
    }

    private fun build(context: Context, pinned: List<Item>, nudge: Boolean): Notification {
        val first = pinned.first()
        val next = first.checklist.nextStep

        val title = when {
            nudge -> "🎯 ¿Avanzaste? ${first.text}"
            pinned.size == 1 -> "🎯 En foco: ${first.text}"
            else -> "🎯 En foco (${pinned.size})"
        }
        val summary = buildString {
            if (pinned.size > 1) append(first.text).append(" · ")
            if (first.checklist.isNotEmpty()) append("${first.checklist.progressText} pasos")
            if (next != null) append(" · Sigue: ${next.text}")
            if (isEmpty()) append("Toca ✓ Hecho cuando la termines")
        }
        val style = NotificationCompat.InboxStyle().also { inbox ->
            pinned.forEach { item ->
                inbox.addLine(buildString {
                    append("☐ ").append(item.text)
                    if (item.checklist.isNotEmpty()) append(" · ").append(item.checklist.progressText)
                    item.checklist.nextStep?.let { append(" → ").append(it.text) }
                })
            }
        }

        val openApp = PendingIntent.getActivity(
            context, 20, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, if (nudge) CHANNEL_NUDGE else CHANNEL_FOCUS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(if (pinned.size > 1) style else NotificationCompat.BigTextStyle().bigText(summary))
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(!nudge)

        if (next != null) {
            builder.addAction(0, "✓ ${next.text.take(24)}", FocusActionReceiver.stepIntent(context, first.id))
        }
        builder.addAction(0, "✓ Hecho", FocusActionReceiver.doneIntent(context, first.id))

        if (nudge) {
            builder.setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT)
        } else {
            builder.setOngoing(true).setSilent(true).setPriority(NotificationCompat.PRIORITY_LOW)
        }

        // Con la app bloqueada con huella, la pantalla de bloqueo no muestra el texto.
        if (UserPrefs(context).appLockEnabled) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            builder.setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_FOCUS)
                    .setSmallIcon(android.R.drawable.ic_menu_agenda)
                    .setContentTitle("🎯 ${pinned.size} ${if (pinned.size == 1) "tarea" else "tareas"} en foco")
                    .build()
            )
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        return builder.build()
    }
}
