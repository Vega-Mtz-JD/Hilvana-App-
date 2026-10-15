package com.hilvana.app.data.link

import android.content.Context
import android.net.Uri
import com.hilvana.app.data.ImageStore
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Trae la vista previa de un enlace y la guarda en el elemento. Límites para no
 * gastar datos ni colgarse: 8 s de espera, 512 KB de página, 5 MB de imagen.
 */
object LinkPreviewFetcher {

    private const val TIMEOUT_MS = 8_000
    private const val MAX_PAGE_BYTES = 512 * 1024
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) Hilvana/1.0 (vista previa de enlaces)"

    /** Busca la vista previa de [url] y, si el elemento todavía tiene ese enlace, la guarda. */
    suspend fun fetchFor(context: Context, itemId: Long, url: String) = withContext(Dispatchers.IO) {
        if (!UserPrefs(context).linkPreviews) return@withContext
        val preview = fetch(url)?.takeIf { it.isUseful } ?: return@withContext
        val imagePath = preview.imageUrl?.let { downloadImage(context, it) }

        val dao = context.database.itemDao()
        val item = dao.getById(itemId)
        if (item == null || LinkPreviewParser.firstUrl(item.text) != url) {
            ImageStore.delete(imagePath) // el enlace cambió o se borró mientras tanto
            return@withContext
        }
        if (item.linkImage != imagePath) ImageStore.delete(item.linkImage)
        dao.update(item.copy(linkUrl = url, linkTitle = preview.title, linkSite = preview.site, linkImage = imagePath))
    }

    fun fetch(url: String): LinkPreview? = runCatching {
        val secureUrl = url.replaceFirst(Regex("^http://"), "https://") // Android bloquea http sin cifrar
        when (LinkPreviewParser.providerOf(secureUrl)) {
            LinkPreviewParser.Provider.YOUTUBE ->
                LinkPreviewParser.parseOEmbed(url, get("https://www.youtube.com/oembed?format=json&url=${encode(secureUrl)}"))
            LinkPreviewParser.Provider.TIKTOK ->
                LinkPreviewParser.parseOEmbed(url, get("https://www.tiktok.com/oembed?url=${encode(secureUrl)}"))
            LinkPreviewParser.Provider.OTHER ->
                LinkPreviewParser.parseHtml(secureUrl, get(secureUrl, stopAtBody = true)).copy(url = url)
        }
    }.getOrNull()

    private fun encode(text: String) = URLEncoder.encode(text, "UTF-8")

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept-Language", "es,en;q=0.8")
        }

    /** Descarga texto; con [stopAtBody] deja de leer al llegar al <body> (lo útil está en <head>). */
    private fun get(url: String, stopAtBody: Boolean = false): String {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val reader = connection.inputStream.bufferedReader()
            val out = StringBuilder()
            val buffer = CharArray(8 * 1024)
            while (out.length < MAX_PAGE_BYTES) {
                val read = reader.read(buffer)
                if (read < 0) break
                out.appendRange(buffer, 0, read)
                if (stopAtBody && out.contains("<body", ignoreCase = true)) break
            }
            return out.toString()
        } finally {
            connection.disconnect()
        }
    }

    /** Baja la miniatura y la guarda reducida dentro de la app (se ve sin internet). */
    private suspend fun downloadImage(context: Context, imageUrl: String): String? = runCatching {
        val temp = File.createTempFile("preview", ".img", context.cacheDir)
        try {
            val connection = open(imageUrl.replaceFirst(Regex("^http://"), "https://"))
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                connection.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var total = 0
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MAX_IMAGE_BYTES) return@runCatching null
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            ImageStore.import(context, Uri.fromFile(temp))
        } finally {
            temp.delete()
        }
    }.getOrNull()
}
