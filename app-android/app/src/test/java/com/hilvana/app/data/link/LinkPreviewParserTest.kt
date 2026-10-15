package com.hilvana.app.data.link

import com.hilvana.app.data.link.LinkPreviewParser.Provider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
