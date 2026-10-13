package com.hilvana.app.data

import android.content.Context
import com.hilvana.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Único lugar para guardar, cambiar o borrar elementos. Mantiene las alarmas
 * sincronizadas: si un recordatorio se borra, se completa o cambia de hora,
 * su alarma se cancela o se reprograma aquí.
 */
class ItemRepository(
    private val context: Context,
    private val dao: ItemDao,
    private val folderDao: FolderDao,
) {

    val items: Flow<List<Item>> = dao.observeAll()
    val folders: Flow<List<Folder>> = folderDao.observeAll()

    suspend fun saveFolder(folder: Folder): Long =
        if (folder.id == 0L) folderDao.insert(folder) else folder.id.also { folderDao.update(folder) }

    suspend fun deleteFolder(folder: Folder) {
        dao.clearFolder(folder.id)
        folderDao.delete(folder)
    }

    /** Inserta (id = 0) o actualiza el elemento. Devuelve su id. */
    suspend fun save(item: Item): Long {
        val previous = if (item.id == 0L) null else dao.getById(item.id)
        val id = if (item.id == 0L) dao.insert(item) else item.id.also { dao.update(item) }
        syncAlarm(item.copy(id = id))
        // Si cambió o se quitó la imagen, la anterior ya no la usa nadie.
        if (previous?.imagePath != null && previous.imagePath != item.imagePath) {
            ImageStore.delete(previous.imagePath)
        }
        return id
    }

    suspend fun delete(item: Item) {
        ReminderScheduler.cancel(context, item.id)
        dao.delete(item)
        ImageStore.delete(item.imagePath)
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
