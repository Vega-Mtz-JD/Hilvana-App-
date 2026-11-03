package com.hilvana.app.bubble

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/**
 * Pantalla invisible que muestra u oculta la burbuja: la usan el widget "Burbuja" y el
 * mosaico de Ajustes rápidos. Pasa por una Activity porque Android solo deja iniciar el
 * servicio de la burbuja desde algo que el usuario está tocando.
 */
class BubbleToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            BubbleService.isRunning.value -> {
                BubbleService.stop(this)
                Toast.makeText(this, "Burbuja oculta", Toast.LENGTH_SHORT).show()
            }
            Settings.canDrawOverlays(this) -> BubbleService.start(this)
            else -> {
                Toast.makeText(
                    this,
                    "Activa \"Mostrar sobre otras apps\" para Hilvana y vuelve a tocar",
                    Toast.LENGTH_LONG,
                ).show()
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        finish()
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, BubbleToggleActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
