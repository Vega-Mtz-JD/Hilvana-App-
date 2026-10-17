package com.hilvana.app.capture

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.hilvana.app.data.AudioStore
import com.hilvana.app.data.ImageStore
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.data.formatDuration
import com.hilvana.app.focus.FocusActions
import com.hilvana.app.repository
import com.hilvana.app.ui.components.HilvanaIcons
import com.hilvana.app.ui.theme.HilvanaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Panel de captura como pantalla transparente, para cuando no hay burbuja
 * (plan §7.5): "Compartir a Hilvana" (textos, enlaces e imágenes), mosaico de
 * Ajustes rápidos, acceso directo y widget. También la usa la burbuja para lo que
 * necesita una pantalla visible: elegir una foto, dictar o grabar una nota de voz.
 */
class CaptureActivity : ComponentActivity() {

    /** Copias internas de la imagen y el audio del panel (se borran si no se guarda). */
    private var imagePath by mutableStateOf<String?>(null)
    private var audioPath by mutableStateOf<String?>(null)
    private var audioDurationMs by mutableStateOf<Long?>(null)
    private var dictated by mutableStateOf<String?>(null)
    private var recording by mutableStateOf(false)
    private var saved = false
    private val recorder by lazy { AudioRecorder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialText = initialText(intent)
        val startMode = if (savedInstanceState == null) intent.getStringExtra(EXTRA_MODE) else null

        imagePath = savedInstanceState?.getString(KEY_IMAGE)
        audioPath = savedInstanceState?.getString(KEY_AUDIO)
        audioDurationMs = savedInstanceState?.getLong(KEY_AUDIO_MS)?.takeIf { audioPath != null }
        val sharedImage = sharedImage(intent)
        if (savedInstanceState == null && sharedImage != null) {
            // Hay que copiarla ya: el permiso para leerla dura solo mientras esta pantalla vive.
            importImage(sharedImage)
        }

        setContent {
            HilvanaTheme {
                val folders by repository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
                val focusTasks by repository.pinned.collectAsStateWithLifecycle(initialValue = emptyList())

                val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                    if (uri != null) importImage(uri)
                }
                val askMicrophone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) startRecording()
                    else Toast.makeText(this, "Sin permiso de micrófono no se puede grabar", Toast.LENGTH_LONG).show()
                }
                val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        dictated = result.data
                            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                            ?.firstOrNull()
                    }
                }
                val dictate = {
                    try {
                        speech.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora…")
                        )
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this, "Tu teléfono no tiene dictado por voz", Toast.LENGTH_LONG).show()
                    }
                }
                val record = { askMicrophone.launch(Manifest.permission.RECORD_AUDIO) }

                // Lo que pidió la burbuja al abrir esta pantalla.
                LaunchedEffect(Unit) {
                    when (startMode) {
                        MODE_IMAGE -> pickImage.launch(PickVisualMediaRequest(ImageOnly))
                        MODE_DICTATE -> dictate()
                        MODE_RECORD -> record()
                    }
                }

                Box {
                    CaptureContent(
                        folders = folders,
                        focusTasks = focusTasks,
                        hideFocusText = UserPrefs(this@CaptureActivity).appLockEnabled,
                        recentFolderIds = UserPrefs(this@CaptureActivity).recentFolderIds,
                        onFocusStep = { lifecycleScope.launch { FocusActions.completeNextStep(this@CaptureActivity, it) } },
                        onFocusDone = { lifecycleScope.launch { FocusActions.complete(this@CaptureActivity, it) } },
                        initialText = initialText,
                        imagePath = imagePath,
                        onImageChange = ::replaceImage,
                        onPickImage = { pickImage.launch(PickVisualMediaRequest(ImageOnly)) },
                        audioPath = audioPath,
                        audioDurationMs = audioDurationMs,
                        onAudioRemove = { replaceAudio(null, null) },
                        onRecordAudio = { record() },
                        onDictate = { dictate() },
                        dictatedText = dictated,
                        onDictationConsumed = { dictated = null },
                        onSave = { text, type, dueAt, folderId, priority, repeat ->
                            lifecycleScope.launch {
                                saveCapture(
                                    this@CaptureActivity, text, type, dueAt, folderId, imagePath, priority, repeat,
                                    audioPath, audioDurationMs,
                                )
                                saved = true
                                finish()
                            }
                        },
                        onDismiss = ::finish,
                    )
                    if (recording) {
                        RecordingOverlay(
                            elapsedMs = { recorder.elapsedMs() },
                            onStop = ::stopRecording,
                            onCancel = ::cancelRecording,
                        )
                    }
                }
            }
        }
    }

    private fun startRecording() {
        if (recorder.start()) recording = true
        else Toast.makeText(this, "No se pudo usar el micrófono", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        val result = recorder.stop()
        recording = false
        if (result == null) {
            Toast.makeText(this, "Grabación muy corta", Toast.LENGTH_SHORT).show()
        } else {
            replaceAudio(result.first, result.second)
        }
    }

    private fun cancelRecording() {
        recorder.cancel()
        recording = false
    }

    override fun onPause() {
        // Si sale de la pantalla mientras graba, se guarda lo grabado hasta ahí.
        if (recording) stopRecording()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_IMAGE, imagePath)
        outState.putString(KEY_AUDIO, audioPath)
        audioDurationMs?.let { outState.putLong(KEY_AUDIO_MS, it) }
    }

    override fun onDestroy() {
        // Se canceló: las copias no las usa ninguna nota.
        if (isFinishing && !saved) {
            ImageStore.delete(imagePath)
            AudioStore.delete(audioPath)
        }
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

    private fun replaceAudio(newPath: String?, durationMs: Long?) {
        if (audioPath != newPath) AudioStore.delete(audioPath)
        audioPath = newPath
        audioDurationMs = durationMs
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
        private const val EXTRA_MODE = "mode"
        const val MODE_IMAGE = "image"
        const val MODE_DICTATE = "dictate"
        const val MODE_RECORD = "record"
        private const val KEY_IMAGE = "image_path"
        private const val KEY_AUDIO = "audio_path"
        private const val KEY_AUDIO_MS = "audio_ms"

        fun intent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)
                .setAction(ACTION_CAPTURE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Desde la burbuja: abre el panel con [text] y lanza [mode] (imagen, dictar o grabar). */
        fun modeIntent(context: Context, text: String, mode: String): Intent =
            intent(context)
                .putExtra(Intent.EXTRA_TEXT, text)
                .putExtra(EXTRA_MODE, mode)
    }
}

/** Pantalla de "Grabando…" con el tiempo, Detener y Cancelar. */
@Composable
private fun RecordingOverlay(elapsedMs: () -> Long, onStop: () -> Unit, onCancel: () -> Unit) {
    var elapsed by androidx.compose.runtime.remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            elapsed = elapsedMs()
            delay(200)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(HilvanaIcons.VoiceNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
            Text("Grabando…", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(formatDuration(elapsed), style = MaterialTheme.typography.displaySmall, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancelar", color = Color.White) }
                Button(onClick = onStop) { Text("Detener") }
            }
        }
    }
}
