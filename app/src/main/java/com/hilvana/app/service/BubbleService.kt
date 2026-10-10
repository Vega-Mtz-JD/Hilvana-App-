package com.hilvana.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service que mantiene viva la burbuja flotante.
 * Fase 0: stub — implementación completa pendiente.
 */
@AndroidEntryPoint
class BubbleService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
