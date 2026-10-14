package com.hilvana.app.data.backup

import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {

    private val folders = listOf(
        Folder(id = 1, name = "Ideas", emoji = "💡", createdAt = 100),
        Folder(id = 2, name = "Claves", emoji = "🔑", createdAt = 200, locked = true),
    )
    private val items = listOf(
        Item(id = 10, text = "App de recetas", type = ItemType.NOTE, createdAt = 1_000, folderId = 1, inInbox = false),
        Item(id = 11, text = "Llamar a mamá", type = ItemType.REMINDER, dueAt = 5_000, createdAt = 2_000, priority = 4),
        Item(
            id = 12, text = "Captura", type = ItemType.NOTE, createdAt = 3_000, done = false,
            imagePath = "/data/user/0/com.hilvana.app/files/images/abc.jpg",
        ),
        Item(id = 13, text = "Pagar luz", type = ItemType.TASK, createdAt = 4_000, done = true, priority = 5, completedAt = 4_500),
    )

    @Test fun idaYVueltaConservaTodo() {
        val decoded = BackupJson.decode(BackupJson.encode(folders, items, exportedAt = 9_999))

        assertEquals(folders, decoded.folders)
        assertEquals(items.size, decoded.items.size)
        decoded.items.zip(items).forEach { (backup, original) ->
            // Al importar se crean ids nuevos y la imagen se ubica por nombre de archivo.
            assertEquals(original.copy(id = 0, imagePath = null), backup.item)
        }
        assertEquals("abc.jpg", decoded.items[2].imageName)
        assertNull(decoded.items[0].imageName)
    }

    @Test fun archivoQueNoEsRespaldo() {
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode("{\"hola\": 1}") }
        assertThrows(InvalidBackupException::class.java) { BackupJson.decode("esto no es json") }
    }

    @Test fun tipoViejoIdeaSeLeeComoNota() {
        val json = """{"format":"hilvana-backup","version":1,"folders":[],
            "items":[{"text":"x","type":"IDEA","createdAt":1}]}"""
        assertEquals(ItemType.NOTE, BackupJson.decode(json).items.single().item.type)
    }

    @Test fun importarDosVecesNoDuplica() {
        val backup = BackupJson.decode(BackupJson.encode(folders, items, 0)).items
        // Ya está todo: no se agrega nada.
        assertEquals(0, BackupMerge.newItems(existing = items, incoming = backup).size)
        // Teléfono nuevo: entra todo.
        assertEquals(items.size, BackupMerge.newItems(existing = emptyList(), incoming = backup).size)
        // Dentro del mismo respaldo tampoco se repite.
        assertEquals(items.size, BackupMerge.newItems(emptyList(), backup + backup).size)
    }
}
