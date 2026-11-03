package com.hilvana.app.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    @Test fun comparaVersiones() {
        assertTrue(ReleaseParser.isNewer("0.10.0", "0.9.5"))
        assertTrue(ReleaseParser.isNewer("0.10.1", "0.10.0"))
        assertTrue(ReleaseParser.isNewer("1.0", "0.99.9"))
        assertFalse(ReleaseParser.isNewer("0.10.0", "0.10.0"))
        assertFalse(ReleaseParser.isNewer("0.9.9", "0.10.0"))
        // El sufijo "-beta" no cuenta.
        assertFalse(ReleaseParser.isNewer("0.10.0-beta", "0.10.0"))
    }

    @Test fun leeLasVersionesDeGitHub() {
        val json = """
            [
              {"tag_name":"v0.11.0","draft":true,"html_url":"d","body":"","assets":[{"name":"Hilvana-0.11.0.apk","browser_download_url":"x"}]},
              {"tag_name":"v0.10.1","draft":false,"prerelease":true,"html_url":"https://github.com/r/v0.10.1","body":" Arreglos \n",
               "assets":[{"name":"notas.txt","browser_download_url":"t"},{"name":"Hilvana-0.10.1.apk","browser_download_url":"https://github.com/a.apk"}]},
              {"tag_name":"v0.9.0-beta","draft":false,"html_url":"p","body":"","assets":[]}
            ]
        """.trimIndent()
        val releases = ReleaseParser.parse(json)
        // Sin el borrador ni la que no trae APK.
        assertEquals(1, releases.size)
        val r = releases.single()
        assertEquals("0.10.1", r.version)
        assertEquals("https://github.com/a.apk", r.apkUrl)
        assertEquals("https://github.com/r/v0.10.1", r.pageUrl)
        assertEquals("Arreglos", r.notes)
    }

    @Test fun respuestaRaraNoRompe() {
        assertTrue(ReleaseParser.parse("{\"message\":\"rate limit\"}").isEmpty())
        assertNull(ReleaseParser.parse("no es json").firstOrNull())
    }
}
