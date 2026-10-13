package com.hilvana.app.data

import android.content.Context
import com.hilvana.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Único lugar para guardar, cambiar o borrar elementos. Mantiene las alarmas
 * sincronizadas: si un recordatorio se borra, se completa o cambia de hora,
 * su alarma se cancela o se reprograma aquí.
 */
class ItemRepository(private val context: Context, private val dao: ItemDao) {

    val items: Flow<List<Item>> = dao.observeAll()

    /** Inserta (id = 0) o actualiza el elemento. Devuelve su id. */
    suspend fun save(item: Item): Long {
        val id = if (item.id == 0L) dao.insert(item) else item.id.also { dao.update(item) }
        syncAlarm(item.copy(id = id))
        return id
    }

    suspend fun delete(item: Item) {
        ReminderScheduler.cancel(context, item.id)
        dao.delete(item)
    }

    private fun syncAlarm(item: Item) {
        val dueAt = item.dueAt
        if (dueAt != null && !item.done && dueAt > System.currentTimeMillis()) {
            ReminderScheduler.schedule(context, item)
        } else {
            ReminderScheduler.cancel(context, item.id)
        }
    }
}
