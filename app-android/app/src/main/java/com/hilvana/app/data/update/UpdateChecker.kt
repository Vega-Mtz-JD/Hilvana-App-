package com.hilvana.app.data.update

import android.content.Context
import com.hilvana.app.BuildConfig
import com.hilvana.app.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Una versión publicada en GitHub Releases. */
data class AppRelease(
    val version: String,
    /** Página de la versión (notas + botón de descarga del APK). */
    val pageUrl: String,
    /** Descarga directa del APK, si la versión trae uno. */
    val apkUrl: String?,
    val notes: String,
)

/** Lectura de la respuesta de GitHub y comparación de versiones (sin red, para poder probarla). */
object ReleaseParser {

    /** Versiones de la respuesta de /releases, la más nueva primero; sin borradores ni sin APK. */
    fun parse(json: String): List<AppRelease> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val release = array.getJSONObject(index)
            if (release.optBoolean("draft")) return@mapNotNull null
            val assets = release.optJSONArray("assets") ?: JSONArray()
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: return@mapNotNull null
            AppRelease(
                version = release.optString("tag_name").removePrefix("v"),
                pageUrl = release.optString("html_url"),
                apkUrl = apk.optString("browser_download_url").ifBlank { null },
                notes = release.optString("body").trim(),
            )
        }
    }.getOrDefault(emptyList())

    /** "0.10.2" > "0.9.5". Lo que va después de un guion ("-beta") no cuenta. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = numbers(candidate)
        val b = numbers(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbers(version: String): List<Int> =
        version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
}

/**
 * Busca en GitHub si hay una versión de prueba más nueva que la instalada. Como mucho una
 * vez cada 12 horas (salvo que el usuario lo pida); solo pregunta a la API pública de GitHub.
 */
object UpdateChecker {

    private const val RELEASES_URL = "https://api.github.com/repos/Vega-Mtz-JD/Hilvana-App-/releases?per_page=10"
    private const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L
    private const val TIMEOUT_MS = 8_000

    /**
     * La versión más nueva que la instalada, o null si no hay (o no se pudo revisar).
     * [force]: revisar ya, aunque se haya revisado hace poco.
     */
    suspend fun check(context: Context, force: Boolean = false): AppRelease? = withContext(Dispatchers.IO) {
        val prefs = UserPrefs(context)
        val now = System.currentTimeMillis()
        val latest = if (force || now - prefs.lastUpdateCheckAt > CHECK_EVERY_MS) {
            fetchLatest()?.also {
                prefs.lastUpdateCheckAt = now
                prefs.latestRelease = it
            } ?: prefs.latestRelease
        } else {
            prefs.latestRelease
        }
        latest?.takeIf { ReleaseParser.isNewer(it.version, BuildConfig.VERSION_NAME) }
    }

    private fun fetchLatest(): AppRelease? = runCatching {
        val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Hilvana/${BuildConfig.VERSION_NAME} (Android)")
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            ReleaseParser.parse(connection.inputStream.bufferedReader().readText())
                .maxWithOrNull { a, b ->
                    when {
                        ReleaseParser.isNewer(a.version, b.version) -> 1
                        ReleaseParser.isNewer(b.version, a.version) -> -1
                        else -> 0
                    }
                }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
