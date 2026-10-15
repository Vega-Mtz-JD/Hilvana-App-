package com.hilvana.app.data

import android.content.Context
import java.io.File
import java.io.InputStream
import java.util.UUID

/** Audios de las notas de voz, guardados dentro de la app (files/audio). */
object AudioStore {

    private fun dir(context: Context) = File(context.filesDir, "audio").apply { mkdirs() }

    /** Archivo nuevo (vacío) donde grabar. */
    fun newFile(context: Context): File = File(dir(context), "${UUID.randomUUID()}.m4a")

    /** Copia un audio leído de un respaldo. */
    fun saveCopy(context: Context, input: InputStream): String? = runCatching {
        val file = newFile(context)
        file.outputStream().use { input.copyTo(it) }
        file.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}

/** "0:07", "1:23" */
fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
