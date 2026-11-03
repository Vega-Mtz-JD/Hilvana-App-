package com.hilvana.app.data.link

import com.hilvana.app.data.link.LinkPreviewParser.Provider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPreviewParserTest {

    @Test fun primerEnlaceDelTexto() {
        assertEquals("https://youtu.be/abc", LinkPreviewParser.firstUrl("mira esto https://youtu.be/abc."))
        assertEquals("https://www.ejemplo.com/x", LinkPreviewParser.firstUrl("ver www.ejemplo.com/x"))
        assertNull(LinkPreviewParser.firstUrl("sin enlaces"))
    }

    @Test fun proveedores() {
        assertEquals(Provider.YOUTUBE, LinkPreviewParser.providerOf("https://www.youtube.com/watch?v=1"))
        assertEquals(Provider.YOUTUBE, LinkPreviewParser.providerOf("https://youtu.be/1"))
        assertEquals(Provider.TIKTOK, LinkPreviewParser.providerOf("https://vm.tiktok.com/xyz"))
        assertEquals(Provider.OTHER, LinkPreviewParser.providerOf("https://example.com"))
    }

    @Test fun openGraph() {
        val html = """
            <html><head>
            <title>Título de respaldo</title>
            <meta property="og:title" content="Receta de pan &amp; queso">
            <meta content="/img/pan.jpg" property="og:image" />
            <meta property="og:site_name" content="Cocina Fácil">
            </head><body>…</body></html>
        """.trimIndent()
        val p = LinkPreviewParser.parseHtml("https://cocina.com/recetas/pan", html)
        assertEquals("Receta de pan & queso", p.title)
        assertEquals("https://cocina.com/img/pan.jpg", p.imageUrl) // ruta relativa → completa
        assertEquals("Cocina Fácil", p.site)
    }

    @Test fun sinOpenGraphUsaElTitleYElDominio() {
        val p = LinkPreviewParser.parseHtml("https://www.blog.com/a", "<head><title> Mi blog </title></head>")
        assertEquals("Mi blog", p.title)
        assertEquals("blog.com", p.site)
        assertNull(p.imageUrl)
    }

    @Test fun oEmbedDeYouTube() {
        val json = """{"title":"Cómo hacer pan","author_name":"Chef","provider_name":"YouTube",
            "thumbnail_url":"https://i.ytimg.com/vi/abc/hqdefault.jpg"}"""
        val p = LinkPreviewParser.parseOEmbed("https://youtu.be/abc", json)!!
        assertEquals("Cómo hacer pan", p.title)
        assertEquals("YouTube", p.site)
        assertEquals("https://i.ytimg.com/vi/abc/hqdefault.jpg", p.imageUrl)
    }

    @Test fun tiktokSinMiniaturaMuestraElUsuario() {
        // Así responde hoy el oEmbed de TikTok: sin título ni miniatura.
        val json = """{"version":"1.0","type":"video","provider_name":"TikTok","html":"<iframe></iframe>"}"""
        val video = "https://www.tiktok.com/@ana.cocina/video/123"
        val oEmbed = LinkPreviewParser.parseOEmbed(video, json)
        val p = LinkPreviewParser.tiktokPreview("https://vm.tiktok.com/xyz/", video, oEmbed)
        assertEquals("TikTok · @ana.cocina", p.site)
        assertNull(p.imageUrl)
        assertTrue(p.isUseful)
        assertEquals("https://vm.tiktok.com/xyz/", p.url)
    }

    @Test fun tiktokConMiniaturaLaUsa() {
        val json = """{"title":"Pan casero","thumbnail_url":"https://p16.tiktokcdn.com/a.jpg","provider_name":"TikTok"}"""
        val video = "https://www.tiktok.com/@ana/video/1"
        val p = LinkPreviewParser.tiktokPreview(video, video, LinkPreviewParser.parseOEmbed(video, json))
        assertEquals("Pan casero", p.title)
        assertEquals("https://p16.tiktokcdn.com/a.jpg", p.imageUrl)
    }

    @Test fun linksDeTiktok() {
        assertTrue(LinkPreviewParser.isTiktokShortLink("https://vm.tiktok.com/ZMabc/"))
        assertTrue(LinkPreviewParser.isTiktokShortLink("https://www.tiktok.com/t/ZTabc/"))
        assertFalse(LinkPreviewParser.isTiktokShortLink("https://www.tiktok.com/@ana/video/1"))
        assertEquals("@ana", LinkPreviewParser.tiktokAuthor("https://www.tiktok.com/@ana/video/1"))
        // El link corto a veces redirige sin usuario: "@/video/…".
        assertNull(LinkPreviewParser.tiktokAuthor("https://www.tiktok.com/@/video/1"))
        assertEquals("TikTok", LinkPreviewParser.tiktokPreview("u", "https://www.tiktok.com/@/video/1", null).site)
    }
}
