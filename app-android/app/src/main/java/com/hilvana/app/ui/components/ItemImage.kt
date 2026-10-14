package com.hilvana.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hilvana.app.data.ImageStore

/** Carga la imagen guardada en segundo plano, reducida a [maxSide] px. */
@Composable
private fun rememberStoredImage(path: String, maxSide: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(initialValue = null, path, maxSide) {
        value = ImageStore.load(path, maxSide)
    }
    return image
}

/** Miniatura recortada (lista de notas, editor, panel de captura). */
@Composable
fun ItemImage(
    path: String,
    modifier: Modifier = Modifier,
    maxSide: Int = 480,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (() -> Unit)? = null,
) {
    val image = rememberStoredImage(path, maxSide)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Imagen de la nota",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Imagen a pantalla completa sobre fondo negro; se cierra con un toque o "atrás". */
@Composable
fun ImageViewer(path: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
        ) {
            val image = rememberStoredImage(path, maxSide = 2048)
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "Imagen de la nota",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) { Text("Cerrar ✕", color = Color.White) }
        }
    }
}
