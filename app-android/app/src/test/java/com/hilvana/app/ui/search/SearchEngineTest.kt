package com.hilvana.app.ui.search

import com.hilvana.app.data.CheckItem
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private val links = Folder(id = 1, name = "Links")
    private val folders = listOf(links)
    private val items = listOf(
        Item(id = 1, text = "Canción para el cumpleaños", type = ItemType.NOTE, createdAt = 1),
        Item(id = 2, text = "Pagar la luz", type = ItemType.TASK, createdAt = 2, priority = 5),
        Item(id = 3, text = "Video de recetas https://tiktok.com/x", type = ItemType.NOTE, createdAt = 3, folderId = 1),
        Item(
            id = 4, text = "Mudanza", type = ItemType.TASK, createdAt = 4,
            checklist = listOf(CheckItem("Llamar al flete"), CheckItem("Empacar libros")),
        ),
        Item(id = 5, text = "Luz del pasillo arreglada", type = ItemType.TASK, createdAt = 5, done = true),
        Item(id = 6, text = "Captura", type = ItemType.NOTE, createdAt = 6, imagePath = "/x.jpg"),
        Item(id = 7, text = "Luz: revisar recibo", type = ItemType.REMINDER, createdAt = 7, dueAt = 99),
    )

    private fun ids(query: String, filters: SearchFilters = SearchFilters()) =
        SearchEngine.search(items, folders, query, filters).map { it.id }

    @Test fun sinAcentosNiMayusculas() = assertEquals(listOf(1L), ids("CANCION"))

    @Test fun variasPalabrasDebenEstarTodas() = assertEquals(listOf(2L), ids("pagar luz"))

    @Test fun buscaEnLosPasosDelChecklist() = assertEquals(listOf(4L), ids("flete"))

    @Test fun buscaPorNombreDeCarpeta() = assertEquals(listOf(3L), ids("links"))

    @Test fun lasHechasSoloSiSePide() {
        assertTrue(5L !in ids("luz"))
        assertTrue(5L in ids("luz", SearchFilters(includeDone = true)))
    }

    @Test fun lasQueEmpiezanConLoBuscadoVanPrimero() =
        // "Luz: revisar…" empieza con "luz"; "Pagar la luz" solo la contiene.
        assertEquals(listOf(7L, 2L), ids("luz"))

    @Test fun filtrosRapidosSinTexto() {
        assertEquals(listOf(3L), ids("", SearchFilters(withLink = true)))
        assertEquals(listOf(6L), ids("", SearchFilters(withImage = true)))
        assertEquals(listOf(2L), ids("", SearchFilters(important = true)))
        assertEquals(listOf(7L), ids("", SearchFilters(type = ItemType.REMINDER)))
    }

    @Test fun sinTextoNiFiltrosNoMuestraNada() = assertEquals(emptyList<Long>(), ids("  "))

    @Test fun resaltado() {
        assertEquals(listOf(0..6), SearchEngine.highlightRanges("Canción para", "cancion"))
        assertEquals(listOf(9..11), SearchEngine.highlightRanges("Pagar la luz", "LUZ"))
    }
}
