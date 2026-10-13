package com.hilvana.app.data.backup

import android.content.Context
import android.net.Uri
import com.hilvana.app.data.ImageStore
import com.hilvana.app.data.Item
import com.hilvana.app.database
import com.hilvana.app.reminder.ReminderScheduler
import com.hilvana.app.widget.refreshWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Cuánto se exportó o importó, para avisarle al usuario. */
data class BackupResult(val items: Int, val images: Int, val skipped: Int = 0)

/**
 * Respaldo en un solo archivo .zip:
 *   data.json  → carpetas y elementos (ver [BackupJson])
 *   images/…   → las imágenes de las notas
 * Importar SUMA a lo que ya hay (nunca borra) y no duplica lo que ya existe.
 */
object BackupManager {

    private const val DATA_ENTRY = "data.json"
    private const val IMAGES_DIR = "images/"

    suspend fun export(context: Context, uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val db = context.database
        val folders = db.folderDao().getAll()
        val items = db.itemDao().getAll()
        var images = 0

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw InvalidBackupException("No se pudo crear el archivo")
        ZipOutputStream(output.buffered()).use { zip ->
            items.mapNotNull { it.imagePath }.distinct().map(::File).filter { it.exists() }.forEach { file ->
                zip.putNextEntry(ZipEntry(IMAGES_DIR + file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                images++
            }
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(BackupJson.encode(folders, items, System.currentTimeMillis()).toByteArray())
            zip.closeEntry()
        }
        BackupResult(items = items.size, images = images)
    }

    suspend fun import(context: Context, uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        // 1) Leer el zip: las imágenes se copian con nombres nuevos (nunca se usa la
        //    ruta que trae el zip, así un archivo malicioso no puede escribir fuera).
        val extractedImages = mutableMapOf<String, String>()
        var json: String? = null
        val input = context.contentResolver.openInputStream(uri)
            ?: throw InvalidBackupException("No se pudo abrir el archivo")
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                when {
                    entry.isDirectory -> Unit
                    entry.name == DATA_ENTRY -> json = zip.readBytes().decodeToString()
                    entry.name.startsWith(IMAGES_DIR) ->
                        ImageStore.saveCopy(context, zip)?.let { extractedImages[entry.name.removePrefix(IMAGES_DIR)] = it }
                }
            }
        }
        val content = try {
            BackupJson.decode(json ?: throw InvalidBackupException("Ese archivo no es un respaldo de Hilvana"))
        } catch (e: Exception) {
            extractedImages.values.forEach(ImageStore::delete)
            throw e
        }

        // 2) Carpetas: las que se llaman igual se unen; las demás se crean.
        val db = context.database
        val existingFolders = db.folderDao().getAll()
        val folderIds = content.folders.associate { backupFolder ->
            val match = existingFolders.firstOrNull { it.matches(backupFolder.name) }
            backupFolder.id to (match?.id ?: db.folderDao().insert(backupFolder.copy(id = 0)))
        }

        // 3) Elementos: solo los que no existen ya.
        val toInsert = BackupMerge.newItems(db.itemDao().getAll(), content.items)
        var images = 0
        toInsert.forEach { (item, imageName) ->
            val imagePath = imageName?.let { extractedImages.remove(it) }
            if (imagePath != null) images++
            db.itemDao().insert(
                item.copy(id = 0, folderId = item.folderId?.let(folderIds::get), imagePath = imagePath)
            )
        }
        // Imágenes de elementos que ya existían: sobran.
        extractedImages.values.forEach(ImageStore::delete)

        ReminderScheduler.rescheduleAll(context)
        refreshWidgets(context)
        BackupResult(items = toInsert.size, images = images, skipped = content.items.size - toInsert.size)
    }
}

/** Regla para no duplicar al importar (separada para poder probarla). */
object BackupMerge {

    private fun Item.key() = Triple(text, createdAt, type)

    /** Elementos del respaldo que todavía no existen (mismo texto, fecha de creación y tipo). */
    fun newItems(existing: List<Item>, incoming: List<BackupItem>): List<BackupItem> {
        val seen = existing.map { it.key() }.toMutableSet()
        return incoming.filter { seen.add(it.item.key()) }
    }
}
