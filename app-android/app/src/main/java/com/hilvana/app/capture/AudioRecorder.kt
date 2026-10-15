package com.hilvana.app.capture

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.hilvana.app.data.AudioStore
import java.io.File

/**
 * Graba una nota de voz (AAC/M4A, buena calidad y archivos pequeños: ~1 MB por
 * minuto). Solo se usa con una pantalla visible (CaptureActivity).
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean = runCatching {
        val output = AudioStore.newFile(context)
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44_100)
            setOutputFile(output.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        file = output
        startedAt = SystemClock.elapsedRealtime()
        true
    }.getOrElse {
        cancel()
        false
    }

    fun elapsedMs(): Long = if (isRecording) SystemClock.elapsedRealtime() - startedAt else 0L

    /** Termina y devuelve (ruta, duración), o null si fue demasiado corta o falló. */
    fun stop(): Pair<String, Long>? {
        val duration = elapsedMs()
        val output = file
        val ok = runCatching { recorder?.stop() }.isSuccess
        release()
        if (!ok || output == null || duration < 700) {
            AudioStore.delete(output?.absolutePath)
            return null
        }
        return output.absolutePath to duration
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        AudioStore.delete(file?.absolutePath)
        release()
    }

    private fun release() {
        runCatching { recorder?.release() }
        recorder = null
        file = null
    }
}
