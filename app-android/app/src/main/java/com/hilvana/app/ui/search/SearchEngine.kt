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
) {
    val isEmpty: Boolean get() = this == SearchFilters()
}

/**
 * Búsqueda en todo: texto, pasos del checklist y nombre de la carpeta, sin
 * importar acentos ni mayúsculas. Con varias palabras, deben estar todas.
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
    ): List<Item> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotBlank() }
        // Sin texto ni filtros no se muestra nada (evita listar todo de golpe).
        if (words.isEmpty() && filters.isEmpty) return emptyList()

        val foldersById = folders.associateBy { it.id }
        val phrase = normalize(query.trim())

        return items
            .asSequence()
            .filter { filters.includeDone || !it.done }
            .filter { filters.type == null || it.type == filters.type }
            .filter { !filters.withLink || LINK.containsMatchIn(it.text) }
            .filter { !filters.withImage || it.imagePath != null }
            .filter { !filters.important || it.priority >= IMPORTANT_MIN_STARS }
            .mapNotNull { item ->
                val text = normalize(item.text)
                val haystack = buildString {
                    append(text)
                    item.checklist.forEach { append(' ').append(normalize(it.text)) }
                    item.folderId?.let(foldersById::get)?.let { append(' ').append(normalize(it.name)) }
                }
                if (words.any { it !in haystack }) return@mapNotNull null
                // Orden: empieza con lo buscado > contiene la frase > solo las palabras sueltas.
                val score = when {
                    phrase.isEmpty() -> 0
                    text.startsWith(phrase) -> 3
                    phrase in text -> 2
                    words.all { it in text } -> 1
                    else -> 0
                }
                item to score
            }
            .sortedWith(compareByDescending<Pair<Item, Int>> { it.second }.thenByDescending { it.first.createdAt })
            .map { it.first }
            .toList()
    }

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
