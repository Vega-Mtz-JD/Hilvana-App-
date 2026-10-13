package com.hilvana.app.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.hilvana.app.data.ImageStore
import com.hilvana.app.repository
import com.hilvana.app.ui.theme.HilvanaTheme
import kotlinx.coroutines.launch

/**
 * Panel de captura como pantalla transparente, para cuando no hay burbuja
 * (plan §7.5): "Compartir a Hilvana" (textos, enlaces e imágenes), mosaico de
 * Ajustes rápidos y acceso directo.
 */
class CaptureActivity : ComponentActivity() {

    /** Copia interna de la imagen compartida (se borra si no se guarda). */
    private var imagePath by mutableStateOf<String?>(null)
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = sharedText(intent)

        imagePath = savedInstanceState?.getString(KEY_IMAGE)
        val sharedImage = sharedImage(intent)
        if (savedInstanceState == null && sharedImage != null) {
            // Hay que copiarla ya: el permiso para leerla dura solo mientras esta pantalla vive.
            lifecycleScope.launch {
                imagePath = ImageStore.import(this@CaptureActivity, sharedImage)
                if (imagePath == null) {
                    Toast.makeText(this@CaptureActivity, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setContent {
            HilvanaTheme {
                val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
                CaptureContent(
                    folders = folders,
                    initialText = sharedText,
                    imagePath = imagePath,
                    onSave = { text, type, dueAt, folderId ->
                        lifecycleScope.launch {
                            saveCapture(this@CaptureActivity, text, type, dueAt, folderId, imagePath)
                            saved = true
                            finish()
                        }
                    },
                    onDismiss = ::finish,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_IMAGE, imagePath)
    }

    override fun onDestroy() {
        // Se canceló: la copia de la imagen no la usa ninguna nota.
        if (isFinishing && !saved) ImageStore.delete(imagePath)
        super.onDestroy()
    }

    /** Texto recibido desde "Compartir" (asunto + texto, p. ej. título y enlace). */
    private fun sharedText(intent: Intent?): String {
        if (intent?.action != Intent.ACTION_SEND) return ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        return listOf(subject, text)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
    }

    /** Imagen recibida desde "Compartir" (foto de la galería, captura de pantalla…). */
    private fun sharedImage(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("image/") != true) return null
        return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
    }

    companion object {
        const val ACTION_CAPTURE = "com.hilvana.app.CAPTURE"
        private const val KEY_IMAGE = "image_path"

        fun intent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)
                .setAction(ACTION_CAPTURE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
