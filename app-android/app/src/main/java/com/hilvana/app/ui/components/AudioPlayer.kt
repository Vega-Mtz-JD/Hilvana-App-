package com.hilvana.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hilvana.app.data.formatDuration
import kotlinx.coroutines.delay

/** ▶️ Escuchar una nota de voz: reproducir/pausar y el tiempo ("0:04 / 0:12"). */
@Composable
fun AudioPlayer(
    path: String,
    durationMs: Long?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(path) { mutableStateOf(false) }
    var position by remember(path) { mutableLongStateOf(0L) }

    // Liberar el reproductor al salir de la pantalla.
    DisposableEffect(path) {
        onDispose { player?.release() }
    }
    // Avanzar el contador mientras suena.
    LaunchedEffect(playing) {
        while (playing) {
            position = player?.currentPosition?.toLong() ?: 0L
            delay(250)
        }
    }

    fun toggle() {
        val current = player ?: runCatching {
            MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    playing = false
                    position = 0
                }
            }
        }.getOrNull()?.also { player = it } ?: return
        if (playing) current.pause() else current.start()
        playing = !playing
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = ::toggle) { Text(if (playing) "⏸" else "▶️") }
        Text(
            "🎙️ " + (if (playing || position > 0) "${formatDuration(position)} / " else "") +
                formatDuration(durationMs ?: 0),
            style = MaterialTheme.typography.labelMedium,
        )
        trailing?.invoke()
    }
}
