package com.hilvana.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Guarda las imágenes de las notas dentro de la app (files/images), comprimidas.
 * Se copian para que la nota no dependa de la foto original: si el usuario la
 * borra de la galería, la nota la conserva.
 */
object ImageStore {

    /** Lado más largo de las imágenes guardadas: suficiente para leer una captura. */
    private const val MAX_SIDE = 1600
    private const val JPEG_QUALITY = 85

    private fun dir(context: Context) = File(context.filesDir, "images").apply { mkdirs() }

    /** Copia la imagen a la app. Devuelve la ruta guardada, o null si no se pudo leer. */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE)
            }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching null
            val rotation = resolver.openInputStream(uri)?.use { rotationOf(ExifInterface(it)) } ?: 0
            val bitmap = scaleAndRotate(decoded, MAX_SIDE, rotation)

            val file = File(dir(context), "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            file.absolutePath
        }.getOrNull()
    }

    /** Carga una imagen guardada reducida a [maxSide] píxeles (para miniaturas). */
    suspend fun load(path: String, maxSide: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            }
            BitmapFactory.decodeFile(path, options)?.asImageBitmap()
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }

    /** Potencia de 2 más grande que deja la imagen todavía ≥ maxSide (decodificar barato). */
    private fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private fun scaleAndRotate(source: Bitmap, maxSide: Int, rotation: Int): Bitmap {
        val scale = minOf(1f, maxSide.toFloat() / max(source.width, source.height))
        if (scale == 1f && rotation == 0) return source
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /** Las fotos de cámara suelen venir "acostadas" con la orientación en el EXIF. */
    private fun rotationOf(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}
