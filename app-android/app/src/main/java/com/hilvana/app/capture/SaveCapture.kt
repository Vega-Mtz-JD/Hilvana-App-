package com.hilvana.app.capture

import android.content.Context
import android.widget.Toast
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.reminder.ReminderScheduler
import com.hilvana.app.repository
import java.text.DateFormat
import java.util.Date

/**
 * Guarda lo capturado en la Bandeja (programando la alarma si tiene hora) y avisa
 * con un mensaje corto. Llamar desde el hilo principal.
 */
suspend fun saveCapture(
    context: Context,
    text: String,
    type: ItemType,
    dueAt: Long?,
    folderId: Long?,
    imagePath: String? = null,
    priority: Int = 0,
) {
    val app = context.applicationContext
    app.repository.save(
        Item(
            text = text,
            type = type,
            dueAt = dueAt,
            folderId = folderId,
            imagePath = imagePath,
            priority = priority,
        )
    )

    val message = if (dueAt == null) {
        "Guardado en la Bandeja"
    } else {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(dueAt))
        if (ReminderScheduler.canScheduleExact(app)) {
            "Te aviso a las $time"
        } else {
            "Te aviso cerca de las $time (activa las alarmas exactas)"
        }
    }
    Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
}
