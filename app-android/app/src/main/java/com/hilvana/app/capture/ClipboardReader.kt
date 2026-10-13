package com.hilvana.app.capture

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

/**
 * Lee lo último que el usuario copió (enlace, texto o imagen) para pegarlo en la
 * nota de un toque. Android solo deja leer el portapapeles a la ventana que tiene
 * el foco, por eso se lee al tocar "Pegar" (y no en segundo plano).
 */
object ClipboardReader {

    enum class Kind(val label: String) {
        LINK("enlace"),
        TEXT("texto"),
        IMAGE("imagen"),
    }

    sealed interface Clip {
        data class Text(val text: String) : Clip
        data class Image(val uri: Uri) : Clip
    }

    private fun manager(context: Context) = context.getSystemService(ClipboardManager::class.java)

    /** Qué tipo de cosa hay copiada, sin leer su contenido (para el texto del botón). */
    fun peekKind(context: Context): Kind? {
        val description = runCatching { manager(context).primaryClipDescription }.getOrNull() ?: return null
        return when {
            description.hasMimeType("image/*") -> Kind.IMAGE
            description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) -> Kind.LINK
            description.hasMimeType("text/*") -> Kind.TEXT
            else -> null
        }
    }

    fun read(context: Context): Clip? {
        val clip = runCatching { manager(context).primaryClip }.getOrNull() ?: return null
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null && clip.description.hasMimeType("image/*")) return Clip.Image(uri)
        val text = item.coerceToText(context)?.toString()?.trim()
        return if (text.isNullOrEmpty()) null else Clip.Text(text)
    }
}
