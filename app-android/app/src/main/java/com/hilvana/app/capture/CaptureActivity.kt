package com.hilvana.app.capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.runtime.LaunchedEffect
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
 * Ajustes rápidos, acceso directo y widget. También la usa la burbuja para
 * elegir una foto (el selector de fotos necesita una pantalla).
 */
class CaptureActivity : ComponentActivity() {

    /** Copia interna de la imagen del panel (se borra si no se guarda). */
    private var imagePath by mutableStateOf<String?>(null)
    private var saved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialText = initialText(intent)
        val pickOnStart = savedInstanceState == null && intent.getBooleanExtra(EXTRA_PICK_IMAGE, false)

        imagePath = savedInstanceState?.getString(KEY_IMAGE)
        val sharedImage = sharedImage(intent)
        if (savedInstanceState == null && sharedImage != null) {
            // Hay que copiarla ya: el permiso para leerla dura solo mientras esta pantalla vive.
            importImage(sharedImage)
        }

        setContent {
            HilvanaTheme {
                val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
                val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) importImage(uri)
                }
                LaunchedEffect(Unit) {
                    if (pickOnStart) pickImage.launch(PickVisualMediaRequest(ImageOnly))
                }

                CaptureContent(
                    folders = folders,
                    initialText = initialText,
                    imagePath = imagePath,
                    onImageChange = ::replaceImage,
                    onPickImage = { pickImage.launch(PickVisualMediaRequest(ImageOnly)) },
                    onSave = { text, type, dueAt, folderId, priority, repeat ->
                        lifecycleScope.launch {
                            saveCapture(this@CaptureActivity, text, type, dueAt, folderId, imagePath, priority, repeat)
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

    private fun importImage(uri: Uri) {
        lifecycleScope.launch {
            val imported = ImageStore.import(this@CaptureActivity, uri)
            if (imported != null) {
                replaceImage(imported)
            } else {
                Toast.makeText(this@CaptureActivity, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun replaceImage(newPath: String?) {
        if (imagePath != newPath) ImageStore.delete(imagePath)
        imagePath = newPath
    }

    /** Texto inicial: lo compartido (asunto + texto) o lo que ya se había escrito en la burbuja. */
    private fun initialText(intent: Intent?): String {
        if (intent == null) return ""
        if (intent.action == ACTION_CAPTURE) return intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (intent.action != Intent.ACTION_SEND) return ""
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
        private const val EXTRA_PICK_IMAGE = "pick_image"
        private const val KEY_IMAGE = "image_path"

        fun intent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)
                .setAction(ACTION_CAPTURE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Desde la burbuja: abre el panel con [text] y lanza el selector de fotos. */
        fun pickImageIntent(context: Context, text: String): Intent =
            intent(context)
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(EXTRA_PICK_IMAGE, true)
    }
}
