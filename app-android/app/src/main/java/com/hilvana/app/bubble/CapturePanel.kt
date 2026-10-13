package com.hilvana.app.bubble

import android.content.Context
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.hilvana.app.repository
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hilvana.app.capture.CaptureContent
import com.hilvana.app.data.ItemType
import com.hilvana.app.ui.theme.HilvanaTheme

/**
 * Mini-panel de captura que se abre al tocar la burbuja, encima de la app actual.
 * Es una ventana superpuesta (no una Activity) para no depender del permiso de
 * "abrir ventanas en segundo plano" que algunos fabricantes (Xiaomi) bloquean.
 */
class CapturePanel(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onSave: (text: String, type: ItemType, dueAt: Long?, folderId: Long?) -> Unit,
    private val onClosed: () -> Unit,
) {
    private var root: FrameLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = root != null

    fun show() {
        if (root != null) return
        val owner = OverlayLifecycleOwner().also { it.resume() }

        val composeView = ComposeView(context).apply {
            setContent {
                HilvanaTheme {
                    val folders by context.repository.folders.collectAsState(initial = emptyList())
                    CaptureContent(
                        folders = folders,
                        onSave = { text, type, dueAt, folderId ->
                            onSave(text, type, dueAt, folderId)
                            dismiss()
                        },
                        onDismiss = ::dismiss,
                    )
                }
            }
        }

        // Contenedor que intercepta el botón "atrás" para cerrar el panel.
        val container = object : FrameLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) dismiss()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        container.setViewTreeLifecycleOwner(owner)
        container.setViewTreeSavedStateRegistryOwner(owner)
        container.addView(composeView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Sin FLAG_NOT_FOCUSABLE: este panel sí necesita el teclado.
            0,
            PixelFormat.TRANSLUCENT,
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        windowManager.addView(container, params)
        root = container
        lifecycleOwner = owner
    }

    fun dismiss() {
        val view = root ?: return
        root = null
        windowManager.removeView(view)
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        onClosed()
    }
}
