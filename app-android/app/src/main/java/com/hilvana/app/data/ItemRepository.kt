package com.hilvana.app.data

import android.content.Context
import com.hilvana.app.data.link.LinkPreviewFetcher
import com.hilvana.app.data.link.LinkPreviewParser
import com.hilvana.app.focus.FocusNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.hilvana.app.reminder.ReminderScheduler
import com.hilvana.app.widget.refreshWidgets
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Único lugar para guardar, cambiar o borrar elementos. Mantiene las alarmas
 * sincronizadas: si un recordatorio se borra, se completa o cambia de hora,
 * su alarma se cancela o se reprograma aquí.
 */
class ItemRepository(
    private val context: Context,
    private val dao: ItemDao,
    private val folderDao: FolderDao,
    private val backgroundScope: CoroutineScope,
) {

    val items: Flow<List<Item>> = dao.observeAll()
    val folders: Flow<List<Folder>> = folderDao.observeAll()

    suspend fun saveFolder(folder: Folder): Long =
        if (folder.id == 0L) folderDao.insert(folder) else folder.id.also { folderDao.update(folder) }

    /** Renombra una categoría en todas sus carpetas (null o vacío = quitarla). */
    suspend fun renameCategory(oldName: String, newName: String?) =
        folderDao.renameCategory(oldName, newName?.trim()?.ifEmpty { null })

    suspend fun deleteFolder(folder: Folder) {
        dao.clearFolder(folder.id)
        folderDao.delete(folder)
    }

    /** Inserta (id = 0) o actualiza el elemento. Devuelve su id. */
    suspend fun save(original: Item): Long {
        val previous = if (original.id == 0L) null else dao.getById(original.id)
        // Carpeta recién usada (nota nueva o movida): el panel de captura la propone la próxima vez.
        val folderId = original.folderId
        if (original.type == ItemType.NOTE && folderId != null && folderId != previous?.folderId) {
            UserPrefs(context).rememberFolderUse(folderId)
        }
        // Fecha de completado: se anota al marcar como hecho y se borra al desmarcar.
        var item = when {
            !original.done -> original.copy(completedAt = null)
            original.completedAt == null -> original.copy(completedAt = previous?.completedAt ?: System.currentTimeMillis())
            else -> original
        }
        // Se repite y se acaba de completar: queda un registro hecho (para historial y
        // estadísticas) y el original sigue pendiente. Si vence hoy o antes, salta a la
        // próxima fecha; si ya está en el futuro (un recordatorio que sonó ya saltó), se queda.
        val rule = item.repeatRule
        val dueAt = item.dueAt
        if (rule != null && dueAt != null && item.done && previous != null && !previous.done) {
            dao.insert(
                item.copy(
                    id = 0, repeatRule = null, imagePath = null, linkImage = null, audioPath = null,
                    inInbox = false, pinned = false,
                )
            )
            val endOfToday = startOfTomorrow()
            item = item.copy(
                done = false,
                completedAt = null,
                dueAt = if (dueAt < endOfToday) nextOccurrenceMillis(rule, dueAt) else dueAt,
                // La próxima vez los pasos empiezan de nuevo.
                checklist = item.checklist.map { it.copy(done = false) },
            )
        }
        // Vista previa del enlace: la maneja el repositorio (no la pantalla). Si el enlace
        // sigue igual se conserva la que ya había; si cambió o se quitó, se borra.
        val url = LinkPreviewParser.firstUrl(item.text)
        item = if (url != null && previous?.linkUrl == url) {
            item.copy(
                linkUrl = previous.linkUrl,
                linkTitle = previous.linkTitle,
                linkSite = previous.linkSite,
                linkImage = previous.linkImage,
            )
        } else {
            item.copy(linkUrl = null, linkTitle = null, linkSite = null, linkImage = null)
        }

        val id = if (item.id == 0L) dao.insert(item) else item.id.also { dao.update(item) }
        syncAlarm(item.copy(id = id))
        // Si cambió o se quitó la imagen (o la miniatura del enlace), la anterior ya no la usa nadie.
        if (previous?.imagePath != null && previous.imagePath != item.imagePath) {
            ImageStore.delete(previous.imagePath)
        }
        if (previous?.linkImage != null && previous.linkImage != item.linkImage) {
            ImageStore.delete(previous.linkImage)
        }
        if (previous?.audioPath != null && previous.audioPath != item.audioPath) {
            AudioStore.delete(previous.audioPath)
        }
        // Enlace nuevo: buscar su vista previa en segundo plano.
        if (url != null && item.linkUrl != url) {
            backgroundScope.launch { LinkPreviewFetcher.fetchFor(context, id, url) }
        }
        refreshWidgets(context)
        FocusNotifier.refresh(context)
        return id
    }

    suspend fun delete(item: Item) {
        ReminderScheduler.cancel(context, item.id)
        dao.delete(item)
        ImageStore.delete(item.imagePath)
        ImageStore.delete(item.linkImage)
        AudioStore.delete(item.audioPath)
        refreshWidgets(context)
        FocusNotifier.refresh(context)
    }

    val pinned: Flow<List<Item>> = dao.observePinned()

    /** Vistas previas que faltan (notas guardadas antes, o sin internet en su momento). */
    suspend fun backfillLinkPreviews(limit: Int = 15) {
        if (!UserPrefs(context).linkPreviews) return
        dao.getAll()
            .mapNotNull { item -> LinkPreviewParser.firstUrl(item.text)?.takeIf { it != item.linkUrl }?.let { item to it } }
            .take(limit)
            .forEach { (item, url) -> LinkPreviewFetcher.fetchFor(context, item.id, url) }
    }

    private fun startOfTomorrow(): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
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
