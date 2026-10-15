package com.hilvana.app.data.link

import org.json.JSONObject
import java.net.URI

/** Lo que se muestra del enlace: título, sitio y miniatura. */
data class LinkPreview(
    val url: String,
    val title: String?,
    val site: String?,
    val imageUrl: String?,
) {
    val isUseful: Boolean get() = !title.isNullOrBlank() || imageUrl != null
}

/**
 * Lectura de vistas previas (sin red, para poder probarla):
 *  - oEmbed (YouTube, TikTok): JSON oficial con título y miniatura.
 *  - Open Graph (el resto): etiquetas <meta property="og:…"> que usan WhatsApp/Facebook.
 */
object LinkPreviewParser {

    private val URL = Regex("(?:https?://|www\\.)\\S+")
    private val META_TAG = Regex("<meta\\s[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("([\\w:-]+)\\s*=\\s*(\"[^\"]*\"|'[^']*')")
    private val TITLE_TAG = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    /** El primer enlace del texto (con https:// si empezaba con "www."). */
    fun firstUrl(text: String): String? = URL.find(text)?.value
        ?.trimEnd('.', ',', ')', ']', '!', '?', ';', ':')
        ?.let { if (it.startsWith("www.")) "https://$it" else it }

    enum class Provider { YOUTUBE, TIKTOK, OTHER }

    fun providerOf(url: String): Provider {
        val host = runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        return when {
            host.endsWith("youtube.com") || host == "youtu.be" -> Provider.YOUTUBE
            host.endsWith("tiktok.com") -> Provider.TIKTOK
            else -> Provider.OTHER
        }
    }

    fun parseOEmbed(url: String, json: String): LinkPreview? = runCatching {
        val o = JSONObject(json)
        LinkPreview(
            url = url,
            title = o.optString("title").ifBlank { null }?.let(::decodeEntities),
            site = (o.optString("provider_name").ifBlank { null } ?: o.optString("author_name").ifBlank { null }),
            imageUrl = o.optString("thumbnail_url").ifBlank { null },
        )
    }.getOrNull()

    fun parseHtml(url: String, html: String): LinkPreview {
        val meta = mutableMapOf<String, String>()
        META_TAG.findAll(html).forEach { tag ->
            val attrs = ATTRIBUTE.findAll(tag.value).associate { m ->
                m.groupValues[1].lowercase() to m.groupValues[2].substring(1, m.groupValues[2].length - 1)
            }
            val key = (attrs["property"] ?: attrs["name"])?.lowercase() ?: return@forEach
            val content = attrs["content"] ?: return@forEach
            meta.putIfAbsent(key, decodeEntities(content.trim()))
        }
        val title = meta["og:title"] ?: meta["twitter:title"]
            ?: TITLE_TAG.find(html)?.groupValues?.get(1)?.trim()?.let(::decodeEntities)
        val image = (meta["og:image"] ?: meta["og:image:url"] ?: meta["twitter:image"])?.let { resolve(url, it) }
        val site = meta["og:site_name"] ?: runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()
        return LinkPreview(url, title?.ifBlank { null }, site, image)
    }

    private fun resolve(base: String, link: String): String? = runCatching {
        URI(base).resolve(link.trim()).toString()
    }.getOrNull()?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

    fun decodeEntities(text: String): String = text
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
}
