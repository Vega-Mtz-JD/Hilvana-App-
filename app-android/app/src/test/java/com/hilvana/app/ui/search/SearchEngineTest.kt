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

    // ---- Título, categoría, carpeta y orden ----

    private val tech = Folder(id = 10, name = "Videos", category = "Tecnología")
    private val work = Folder(id = 11, name = "Ideas", category = "Trabajo")
    private val notes = listOf(
        Item(id = 20, text = "https://vm.tiktok.com/abc", title = "Truco de Android", type = ItemType.NOTE, createdAt = 20, folderId = 10, priority = 2),
        Item(id = 21, text = "Bot para clientes", type = ItemType.NOTE, createdAt = 21, folderId = 11, priority = 5),
        Item(id = 22, text = "android studio sin emulador", type = ItemType.NOTE, createdAt = 22),
    )
    private fun noteIds(query: String, filters: SearchFilters = SearchFilters(), sort: SortOrder = SortOrder.RELEVANCE) =
        SearchEngine.search(notes, listOf(tech, work), query, filters, sort).map { it.id }

    // La 22 empieza con "android" en el texto; la 20 lo tiene en el título (el link no dice nada).
    @Test fun buscaEnElTitulo() = assertEquals(listOf(22L, 20L), noteIds("android"))

    @Test fun tituloQueEmpiezaConLoBuscadoVaPrimero() = assertEquals(listOf(20L), noteIds("truco"))

    @Test fun buscaPorCategoria() = assertEquals(listOf(20L), noteIds("tecnologia"))

    @Test fun filtraPorCategoriaYCarpeta() {
        assertEquals(listOf(21L), noteIds("", SearchFilters(category = "Trabajo")))
        assertEquals(listOf(20L), noteIds("", SearchFilters(folderId = 10)))
    }

    @Test fun ordenes() {
        val all = SearchFilters(type = ItemType.NOTE)
        assertEquals(listOf(22L, 21L, 20L), noteIds("", all, SortOrder.NEWEST))
        assertEquals(listOf(20L, 21L, 22L), noteIds("", all, SortOrder.OLDEST))
        // A–Z usa el título si lo tiene: "android…", "Bot…", "Truco…".
        assertEquals(listOf(22L, 21L, 20L), noteIds("", all, SortOrder.ALPHABETICAL))
        assertEquals(listOf(21L, 20L, 22L), noteIds("", all, SortOrder.IMPORTANCE))
    }
}
