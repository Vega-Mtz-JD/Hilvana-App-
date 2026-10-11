package com.hilvana.app.bubble

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.hilvana.app.MainActivity
import com.hilvana.app.data.Item
import com.hilvana.app.data.ItemType
import com.hilvana.app.database
import com.hilvana.app.reminder.ReminderScheduler
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Servicio en primer plano que dibuja la burbuja flotante encima de las demás apps.
 *
 * - Toque: abre el mini-panel de captura; lo escrito se guarda en la Bandeja.
 * - Arrastrar: mueve la burbuja; al soltarla se pega al borde más cercano.
 * - Soltar sobre la ✕ inferior: oculta la burbuja y detiene el servicio.
 */
class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var dismissView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var capturePanel: CapturePanel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        capturePanel = CapturePanel(
            context = this,
            windowManager = windowManager,
            onSave = ::saveCapture,
            onClosed = { bubbleView?.visibility = View.VISIBLE },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Si el usuario quitó el permiso mientras tanto, no podemos dibujar nada.
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()
        if (bubbleView == null) showBubble()
        _isRunning.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        capturePanel.dismiss()
        scope.cancel()
        removeDismissTarget()
        bubbleView?.let { windowManager.removeView(it) }
        bubbleView = null
        _isRunning.value = false
        super.onDestroy()
    }

    // ---------- Notificación del servicio en primer plano ----------

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Burbuja flotante", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Mantiene la burbuja de captura disponible"
                setShowBadge(false)
            }
        )

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val hide = PendingIntent.getService(
            this, 1,
            Intent(this, BubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Hilvana está lista")
            .setContentText("Toca la burbuja para capturar una idea")
            .setContentIntent(openApp)
            .addAction(0, "Ocultar burbuja", hide)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // ---------- Burbuja ----------

    private fun overlayParams(sizePx: Int) = WindowManager.LayoutParams(
        sizePx,
        sizePx,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // No roba el foco del teclado y solo recibe toques dentro de su área.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun showBubble() {
        val size = dp(BUBBLE_SIZE_DP)
        bubbleParams = overlayParams(size).apply {
            x = screenWidth() - size
            y = screenHeight() / 3
        }

        val view = TextView(this).apply {
            text = "H"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(BUBBLE_COLOR)
                setStroke(dp(2), Color.WHITE)
            }
            contentDescription = "Burbuja de Hilvana: toca para capturar"
        }
        view.setOnTouchListener(BubbleTouchListener())
        windowManager.addView(view, bubbleParams)
        bubbleView = view
    }

    private fun onBubbleTapped() {
        if (capturePanel.isShowing) return
        bubbleView?.visibility = View.GONE
        capturePanel.show()
    }

    private fun saveCapture(text: String, type: ItemType, dueAt: Long?) {
        scope.launch {
            val item = Item(text = text, type = type, dueAt = dueAt)
            val id = database.itemDao().insert(item)

            val message = if (dueAt == null) {
                "Guardado en la Bandeja"
            } else {
                val exact = ReminderScheduler.schedule(this@BubbleService, item.copy(id = id))
                val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(dueAt))
                if (exact) "Te aviso a las $time" else "Te aviso cerca de las $time (activa las alarmas exactas)"
            }
            Toast.makeText(this@BubbleService, message, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        private val touchSlop = ViewConfiguration.get(this@BubbleService).scaledTouchSlop
        private var startX = 0
        private var startY = 0
        private var startRawX = 0f
        private var startRawY = 0f
        private var dragging = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = bubbleParams.x
                    startY = bubbleParams.y
                    startRawX = event.rawX
                    startRawY = event.rawY
                    dragging = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                        showDismissTarget()
                    }
                    if (dragging) {
                        bubbleParams.x = startX + dx.toInt()
                        bubbleParams.y = startY + dy.toInt()
                        windowManager.updateViewLayout(v, bubbleParams)
                        highlightDismissTarget(isOverDismissTarget())
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        v.performClick()
                        onBubbleTapped()
                    } else {
                        val dismiss = isOverDismissTarget()
                        removeDismissTarget()
                        if (dismiss) stopSelf() else snapToEdge(v)
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        removeDismissTarget()
                        snapToEdge(v)
                    }
                }
            }
            return true
        }
    }

    /** Pega la burbuja al borde izquierdo o derecho más cercano, con animación. */
    private fun snapToEdge(view: View) {
        val size = bubbleParams.width
        val maxX = screenWidth() - size
        val targetX = if (bubbleParams.x + size / 2 < screenWidth() / 2) 0 else maxX
        bubbleParams.y = bubbleParams.y.coerceIn(0, screenHeight() - size)

        ValueAnimator.ofInt(bubbleParams.x, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                if (bubbleView == null) return@addUpdateListener
                bubbleParams.x = it.animatedValue as Int
                windowManager.updateViewLayout(view, bubbleParams)
            }
            start()
        }
    }

    // ---------- Zona para ocultar (✕) ----------

    private fun dismissParams(): WindowManager.LayoutParams {
        val size = dp(DISMISS_SIZE_DP)
        return overlayParams(size).apply {
            // Solo es un indicador visual: los toques la atraviesan.
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            x = (screenWidth() - size) / 2
            y = screenHeight() - size - dp(DISMISS_MARGIN_DP)
        }
    }

    private fun showDismissTarget() {
        if (dismissView != null) return
        val view = TextView(this).apply {
            text = "✕"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DISMISS_COLOR)
            }
            // Android 12+ bloquea superposiciones no táctiles con opacidad > 0.8.
            alpha = 0.8f
        }
        windowManager.addView(view, dismissParams())
        dismissView = view
    }

    private fun highlightDismissTarget(active: Boolean) {
        val scale = if (active) 1.25f else 1f
        dismissView?.animate()?.scaleX(scale)?.scaleY(scale)?.setDuration(100)?.start()
    }

    private fun removeDismissTarget() {
        dismissView?.let { windowManager.removeView(it) }
        dismissView = null
    }

    private fun isOverDismissTarget(): Boolean {
        if (dismissView == null) return false
        val target = dismissParams()
        val bubbleCx = bubbleParams.x + bubbleParams.width / 2f
        val bubbleCy = bubbleParams.y + bubbleParams.height / 2f
        val targetCx = target.x + target.width / 2f
        val targetCy = target.y + target.height / 2f
        return hypot(bubbleCx - targetCx, bubbleCy - targetCy) < target.width
    }

    // ---------- Utilidades ----------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun screenWidth(): Int = resources.displayMetrics.widthPixels
    private fun screenHeight(): Int = resources.displayMetrics.heightPixels

    companion object {
        private const val CHANNEL_ID = "bubble"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.hilvana.app.bubble.STOP"

        private const val BUBBLE_SIZE_DP = 56
        private const val DISMISS_SIZE_DP = 64
        private const val DISMISS_MARGIN_DP = 48
        private val BUBBLE_COLOR = Color.parseColor("#6750A4")
        private val DISMISS_COLOR = Color.parseColor("#B3261E")

        private val _isRunning = MutableStateFlow(false)
        /** Si la burbuja está visible ahora mismo (para la UI de la app). */
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, BubbleService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BubbleService::class.java))
        }
    }
}
