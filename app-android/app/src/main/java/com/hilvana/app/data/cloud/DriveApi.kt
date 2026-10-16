package com.hilvana.app.data.cloud

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Archivo de respaldo guardado en Drive. */
data class DriveFile(val id: String, val modifiedTime: String?, val size: Long?)

/** Datos de la cuenta conectada. */
data class GoogleProfile(val email: String?, val name: String?)

/**
 * Llamadas directas a la API de Google Drive (v3) usando solo la carpeta oculta
 * de la app (appDataFolder). Sin librerías pesadas: HttpURLConnection + JSON.
 */
class DriveApi(private val accessToken: String) {

    companion object {
        const val BACKUP_NAME = "hilvana-respaldo.zip"
        private const val API = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
        private const val TIMEOUT_MS = 30_000
    }

    fun profile(): GoogleProfile {
        val json = JSONObject(request("GET", "https://www.googleapis.com/oauth2/v3/userinfo").readText())
        return GoogleProfile(json.optString("email").ifBlank { null }, json.optString("name").ifBlank { null })
    }

    /** El respaldo de Hilvana en Drive, si existe. */
    fun findBackup(): DriveFile? {
        val q = URLEncoder.encode("name = '$BACKUP_NAME' and trashed = false", "UTF-8")
        val url = "$API?spaces=appDataFolder&q=$q&fields=files(id,modifiedTime,size)&orderBy=modifiedTime%20desc"
        val files = JSONObject(request("GET", url).readText()).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        val f = files.getJSONObject(0)
        return DriveFile(f.getString("id"), f.optString("modifiedTime").ifBlank { null }, f.optString("size").toLongOrNull())
    }

    /** Sube el zip: reemplaza el respaldo anterior (así ocupa un solo archivo) o lo crea. */
    fun upload(zip: File) {
        val existing = findBackup()
        if (existing != null) {
            // HttpURLConnection no permite PATCH: Google acepta POST + este encabezado.
            request(
                "POST", "$UPLOAD/${existing.id}?uploadType=media",
                body = zip, contentType = "application/zip",
                headers = mapOf("X-HTTP-Method-Override" to "PATCH"),
            ).close()
        } else {
            val boundary = "hilvana-${System.nanoTime()}"
            val metadata = JSONObject()
                .put("name", BACKUP_NAME)
                .put("parents", JSONArray().put("appDataFolder"))
                .toString()
            val connection = open("POST", "$UPLOAD?uploadType=multipart", "multipart/related; boundary=$boundary")
            connection.doOutput = true
            connection.outputStream.use { out ->
                out.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n".toByteArray())
                out.write("--$boundary\r\nContent-Type: application/zip\r\n\r\n".toByteArray())
                zip.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            check(connection).close()
        }
    }

    fun download(id: String, target: File) {
        val response = request("GET", "$API/$id?alt=media")
        response.connection.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
        response.close()
    }

    /** Quita el permiso que se le dio a Hilvana (al desconectar). */
    fun revoke() {
        runCatching {
            request("POST", "https://oauth2.googleapis.com/revoke?token=${URLEncoder.encode(accessToken, "UTF-8")}").close()
        }
    }

    // ---------- HTTP ----------

    class Response(val connection: HttpURLConnection) {
        fun readText(): String = connection.inputStream.bufferedReader().use { it.readText() }.also { close() }
        fun close() = connection.disconnect()
    }

    private fun open(method: String, url: String, contentType: String? = null) =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $accessToken")
            contentType?.let { setRequestProperty("Content-Type", it) }
        }

    private fun request(
        method: String,
        url: String,
        body: File? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): Response {
        val connection = open(method, url, contentType)
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        if (body != null) {
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.length())
            connection.outputStream.use { out -> body.inputStream().use { it.copyTo(out) } }
        }
        return check(connection)
    }

    private fun check(connection: HttpURLConnection): Response {
        val code = connection.responseCode
        if (code !in 200..299) {
            val detail = runCatching { connection.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull()
            connection.disconnect()
            throw DriveException(code, detail)
        }
        return Response(connection)
    }
}

class DriveException(val code: Int, detail: String?) : IOException("Google Drive respondió $code: ${detail?.take(200)}")
