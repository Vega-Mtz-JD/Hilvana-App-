package com.hilvana.app.capture

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hilvana.app.bubble.BubbleService
import com.hilvana.app.bubble.BubbleToggleActivity

/**
 * Mosaico "Burbuja" en Ajustes rápidos: bajas la barra y lo tocas para mostrar u ocultar
 * la burbuja. Se ve encendido mientras la burbuja está en pantalla.
 * (Conserva el nombre de clase para que no desaparezca de quien ya lo tenía agregado.)
 */
class CaptureTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val running = BubbleService.isRunning.value
        qsTile?.apply {
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = if (running) "Visible" else "Oculta"
            }
            updateTile()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = BubbleToggleActivity.intent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ exige un PendingIntent para abrir una pantalla desde el mosaico.
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Pide al sistema que redibuje el mosaico (la burbuja se mostró u ocultó). */
        fun refresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, CaptureTileService::class.java))
            }
        }
    }
}
