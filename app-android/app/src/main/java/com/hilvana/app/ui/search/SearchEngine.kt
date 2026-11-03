package com.hilvana.app.ui.search

import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import java.text.Normalizer

/** Filtros rápidos de la búsqueda. */
data class SearchFilters(
    val type: ItemType? = null,
    val includeDone: Boolean = false,
    val withLink: Boolean = false,
    val withImage: Boolean = false,
    val important: Boolean = false,
    /** Solo lo de esta carpeta. */
    val folderId: Long? = null,
    /** Solo lo de las carpetas de esta categoría. */
    val category: String? = null,
) {
    val isEmpty: Boolean get() = this == SearchFilters()
}

/** Cómo ordenar una lista de resultados o de notas. */
enum class SortOrder(val label: String) {
    RELEVANCE("Relevancia"),
    NEWEST("Más recientes"),
    OLDEST("Más antiguas"),
    ALPHABETICAL("A–Z"),
    IMPORTANCE("Importancia"),
}

/**
 * Búsqueda en todo: título, texto, título del link, pasos del checklist, nombre de la
 * carpeta y su categoría, sin importar acentos ni mayúsculas. Con varias palabras,
 * deben estar todas.
 */
object SearchEngine {

    private val LINK = Regex("(?:https?://|www\\.)\\S+")
    const val IMPORTANT_MIN_STARS = 4

    /** "Canción" → "cancion" (sin acentos, minúsculas). */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    fun search(
        items: List<Item>,
        folders: List<Folder>,
        query: String,
        filters: SearchFilters = SearchFilters(),
        sort: SortOrder = SortOrder.RELEVANCE,
    ): List<Item> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
        // Sin texto ni filtros no se muestra nada (evita listar todo de golpe).
        if (words.isEmpty() && filters.isEmpty) return emptyList()

        val foldersById = folders.associateBy { it.id }
        val phrase = normalize(query.trim())

        val scored = items
            .asSequence()
            .filter { filters.includeDone || !it.done }
            .filter { filters.type == null || it.type == filters.type }
            .filter { !filters.withLink || LINK.containsMatchIn(it.text) }
            .filter { !filters.withImage || it.imagePath != null }
            .filter { !filters.important || it.priority >= IMPORTANT_MIN_STARS }
            .filter { filters.folderId == null || it.folderId == filters.folderId }
            .filter { filters.category == null || it.folderId?.let(foldersById::get)?.category == filters.category }
            .mapNotNull { item ->
                val text = normalize(item.text)
                val title = item.title?.let(::normalize).orEmpty()
                val folder = item.folderId?.let(foldersById::get)
                val haystack = buildString {
                    append(title).append(' ').append(text)
                    item.linkTitle?.let { append(' ').append(normalize(it)) }
                    item.checklist.forEach { append(' ').append(normalize(it.text)) }
                    folder?.let { append(' ').append(normalize(it.name)) }
                    folder?.category?.let { append(' ').append(normalize(it)) }
                }
                if (words.any { it !in haystack }) return@mapNotNull null
                // Orden: título que empieza con lo buscado > texto que empieza > contiene la frase > palabras sueltas.
                val score = when {
                    phrase.isEmpty() -> 0
                    title.startsWith(phrase) -> 4
                    text.startsWith(phrase) -> 3
                    phrase in title || phrase in text -> 2
                    words.all { it in title || it in text } -> 1
                    else -> 0
                }
                item to score
            }
            .toList()

        return if (sort == SortOrder.RELEVANCE) {
            scored.sortedWith(compareByDescending<Pair<Item, Int>> { it.second }.thenByDescending { it.first.createdAt })
                .map { it.first }
        } else {
            sorted(scored.map { it.first }, sort)
        }
    }

    /** Ordena una lista (Relevancia aquí = más recientes, porque no hay nada buscado). */
    fun sorted(items: List<Item>, order: SortOrder): List<Item> = when (order) {
        SortOrder.RELEVANCE, SortOrder.NEWEST -> items.sortedByDescending { it.createdAt }
        SortOrder.OLDEST -> items.sortedBy { it.createdAt }
        SortOrder.ALPHABETICAL -> items.sortedBy { normalize(sortName(it)) }
        SortOrder.IMPORTANCE -> items.sortedWith(
            compareByDescending<Item> { it.priority }.thenByDescending { it.createdAt }
        )
    }

    /** Nombre para ordenar A–Z: el título, o el título del link, o el texto. */
    private fun sortName(item: Item): String =
        (item.title ?: item.linkTitle ?: item.text).trim()

    /**
     * Rangos de [text] donde aparece alguna palabra buscada (para resaltarlas).
     * Solo si quitar acentos no cambia el largo del texto (lo normal en Android).
     */
    fun highlightRanges(text: String, query: String): List<IntRange> {
        val normalized = normalize(text)
        if (normalized.length != text.length) return emptyList()
        return normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }.flatMap { word ->
            Regex(Regex.escape(word)).findAll(normalized).map { it.range }.toList()
        }
    }
}
