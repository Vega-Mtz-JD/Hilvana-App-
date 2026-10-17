package com.hilvana.app.data.backup

import com.hilvana.app.data.ChecklistJson
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.data.MAX_PRIORITY
import com.hilvana.app.data.RepeatRule
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Un elemento leído del respaldo, con los nombres de su imagen y su audio dentro del zip. */
data class BackupItem(val item: Item, val imageName: String?, val audioName: String? = null)

data class BackupContent(val folders: List<Folder>, val items: List<BackupItem>)

class InvalidBackupException(message: String) : Exception(message)

/**
 * Formato del respaldo (data.json dentro del zip). Se guardan los ids originales
 * solo para relacionar notas con carpetas; al importar se crean ids nuevos.
 */
object BackupJson {

    const val FORMAT = "hilvana-backup"
    const val VERSION = 1

    fun encode(folders: List<Folder>, items: List<Item>, exportedAt: Long): String {
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("folders", JSONArray(folders.map { folder ->
                JSONObject()
                    .put("id", folder.id)
                    .put("name", folder.name)
                    .put("emoji", folder.icon)
                    .put("createdAt", folder.createdAt)
                    .put("locked", folder.locked)
                    .put("category", folder.category ?: JSONObject.NULL)
            }))
            .put("items", JSONArray(items.map { item ->
                JSONObject()
                    .put("text", item.text)
                    .put("type", item.type.name)
                    .put("inInbox", item.inInbox)
                    .put("done", item.done)
                    .put("dueAt", item.dueAt ?: JSONObject.NULL)
                    .put("createdAt", item.createdAt)
                    .put("folderId", item.folderId ?: JSONObject.NULL)
                    .put("image", item.imagePath?.let { File(it).name } ?: JSONObject.NULL)
                    .put("priority", item.priority)
                    .put("completedAt", item.completedAt ?: JSONObject.NULL)
                    .put("repeat", item.repeatRule?.name ?: JSONObject.NULL)
                    .put("checklist", JSONArray(ChecklistJson.encode(item.checklist)))
                    .put("pinned", item.pinned)
                    // La miniatura del enlace no se guarda: se vuelve a traer al importar.
                    .put("linkUrl", item.linkUrl ?: JSONObject.NULL)
                    .put("linkTitle", item.linkTitle ?: JSONObject.NULL)
                    .put("linkSite", item.linkSite ?: JSONObject.NULL)
                    .put("audio", item.audioPath?.let { File(it).name } ?: JSONObject.NULL)
                    .put("audioMs", item.audioDurationMs ?: JSONObject.NULL)
            }))
        return json.toString(2)
    }

    fun decode(text: String): BackupContent {
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (json == null || json.optString("format") != FORMAT) {
            throw InvalidBackupException("Ese archivo no es un respaldo de Hilvana")
        }
        if (json.optInt("version") > VERSION) {
            throw InvalidBackupException("El respaldo es de una versión más nueva de Hilvana: actualiza la app")
        }

        val folders = json.optJSONArray("folders").objects().map { f ->
            Folder(
                id = f.getLong("id"),
                name = f.getString("name"),
                icon = f.optString("emoji", Folder.DEFAULT_ICON),
                createdAt = f.optLong("createdAt", System.currentTimeMillis()),
                locked = f.optBoolean("locked", false),
                category = if (f.isNull("category")) null else f.optString("category").ifBlank { null },
            )
        }
        val items = json.optJSONArray("items").objects().map { i ->
            BackupItem(
                item = Item(
                    text = i.getString("text"),
                    // Tipos de versiones viejas (p. ej. "IDEA") se leen como Nota.
                    type = ItemType.entries.firstOrNull { it.name == i.optString("type") } ?: ItemType.NOTE,
                    inInbox = i.optBoolean("inInbox", false),
                    done = i.optBoolean("done", false),
                    dueAt = i.optLongOrNull("dueAt"),
                    createdAt = i.optLong("createdAt", System.currentTimeMillis()),
                    folderId = i.optLongOrNull("folderId"),
                    priority = i.optInt("priority", 0).coerceIn(0, MAX_PRIORITY),
                    completedAt = i.optLongOrNull("completedAt"),
                    repeatRule = i.optStringOrNull("repeat")?.let { name ->
                        RepeatRule.entries.firstOrNull { it.name == name }
                    },
                    checklist = ChecklistJson.decode(i.optJSONArray("checklist")?.toString()),
                    pinned = i.optBoolean("pinned", false),
                    // linkUrl vacío = "falta la vista previa": se vuelve a traer con su miniatura.
                    linkUrl = null,
                    linkTitle = i.optStringOrNull("linkTitle"),
                    linkSite = i.optStringOrNull("linkSite"),
                    audioDurationMs = i.optLongOrNull("audioMs"),
                ),
                imageName = i.optStringOrNull("image"),
                audioName = i.optStringOrNull("audio"),
            )
        }
        return BackupContent(folders, items)
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key)
}
