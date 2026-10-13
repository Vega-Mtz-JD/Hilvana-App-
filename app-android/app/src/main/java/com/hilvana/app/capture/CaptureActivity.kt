package com.hilvana.app.capture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.hilvana.app.repository
import com.hilvana.app.ui.theme.HilvanaTheme
import kotlinx.coroutines.launch

/**
 * Panel de captura como pantalla transparente, para cuando no hay burbuja
 * (plan §7.5): "Compartir a Hilvana", mosaico de Ajustes rápidos y acceso directo.
 */
class CaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = sharedText(intent)

        setContent {
            HilvanaTheme {
                val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
                CaptureContent(
                    folders = folders,
                    initialText = sharedText,
                    onSave = { text, type, dueAt, folderId ->
                        lifecycleScope.launch {
                            saveCapture(this@CaptureActivity, text, type, dueAt, folderId)
                            finish()
                        }
                    },
                    onDismiss = ::finish,
                )
            }
        }
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

    companion object {
        const val ACTION_CAPTURE = "com.hilvana.app.CAPTURE"

        fun intent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)
                .setAction(ACTION_CAPTURE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
