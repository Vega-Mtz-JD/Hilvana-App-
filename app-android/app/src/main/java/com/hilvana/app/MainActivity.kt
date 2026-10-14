package com.hilvana.app

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hilvana.app.security.AppLock
import com.hilvana.app.security.LockScreen
import com.hilvana.app.focus.FocusNotifier
import com.hilvana.app.widget.refreshWidgets
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilvana.app.bubble.BubbleService
import com.hilvana.app.capture.CaptureTileService
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.reminder.ReminderScheduler
import com.hilvana.app.ui.HilvanaRoot
import com.hilvana.app.ui.onboarding.BatteryHelp
import com.hilvana.app.ui.onboarding.OnboardingScreen
import com.hilvana.app.ui.screens.PermissionsState
import com.hilvana.app.ui.theme.HilvanaTheme

class MainActivity : FragmentActivity() {

    // Se relee en onResume: el usuario puede regresar de Ajustes con el permiso dado.
    private var permissions by mutableStateOf(PermissionsState())

    private val prefs by lazy { UserPrefs(this) }
    private var onboardingDone by mutableStateOf(true)
    private var userName by mutableStateOf("")

    // Bloqueo: una vez desbloqueado no se vuelve a pedir hasta pasar 1 min en segundo plano.
    private var appLockEnabled by mutableStateOf(false)
    private var unlocked by mutableStateOf(false)
    private var backgroundSince = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onboardingDone = prefs.onboardingDone
        userName = prefs.userName
        appLockEnabled = prefs.appLockEnabled

        setContent {
            HilvanaTheme {
                val bubbleRunning by BubbleService.isRunning.collectAsStateWithLifecycle()
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { refreshPermissions() }
                val requestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (appLockEnabled && !unlocked && onboardingDone) {
                    LockScreen(onUnlock = { unlock("Desbloquear Hilvana") {} })
                } else if (!onboardingDone) {
                    OnboardingScreen(
                        permissions = permissions,
                        initialName = userName,
                        onRequestOverlay = ::openOverlaySettings,
                        onRequestNotifications = requestNotifications,
                        onRequestExactAlarms = ::openExactAlarmSettings,
                        onOpenAutostart = { BatteryHelp.openAutostart(this) },
                        onOpenAppDetails = ::openAppDetails,
                        onFinish = { name, showBubble ->
                            prefs.userName = name
                            prefs.onboardingDone = true
                            userName = prefs.userName
                            onboardingDone = true
                            if (showBubble && !bubbleRunning) BubbleService.start(this)
                        },
                    )
                } else {
                    HilvanaRoot(
                        permissions = permissions,
                        bubbleRunning = bubbleRunning,
                        userName = userName,
                        onRequestOverlay = ::openOverlaySettings,
                        onRequestNotifications = requestNotifications,
                        onRequestExactAlarms = ::openExactAlarmSettings,
                        onOpenAppSettings = { BatteryHelp.openAutostart(this) },
                        onToggleBubble = {
                            if (bubbleRunning) BubbleService.stop(this) else BubbleService.start(this)
                        },
                        onAddCaptureTile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ::requestAddCaptureTile
                        } else {
                            null
                        },
                        onShowWelcome = {
                            prefs.onboardingDone = false
                            onboardingDone = false
                        },
                        unlocked = unlocked,
                        onRequestUnlock = ::unlock,
                        appLockEnabled = appLockEnabled,
                        canUseAppLock = AppLock.isAvailable(this),
                        onSetAppLock = ::setAppLock,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundSince > 0 && SystemClock.elapsedRealtime() - backgroundSince > RELOCK_AFTER_MS) {
            unlocked = false
        }
        backgroundSince = 0
    }

    override fun onStop() {
        super.onStop()
        backgroundSince = SystemClock.elapsedRealtime()
    }

    /** Pide huella/PIN; si sale bien, desbloquea la sesión y sigue con [onSuccess]. */
    private fun unlock(title: String, onSuccess: () -> Unit) {
        if (unlocked) {
            onSuccess()
            return
        }
        AppLock.authenticate(this, title) {
            unlocked = true
            onSuccess()
        }
    }

    /** Activar o desactivar el bloqueo de la app siempre exige confirmar con huella/PIN. */
    private fun setAppLock(enabled: Boolean) {
        AppLock.authenticate(
            this,
            title = if (enabled) "Activar bloqueo de Hilvana" else "Quitar bloqueo de Hilvana",
        ) {
            prefs.appLockEnabled = enabled
            appLockEnabled = enabled
            unlocked = true
            lifecycleScope.launch {
                // Con o sin bloqueo cambia qué se ve en la pantalla de inicio y la de bloqueo.
                refreshWidgets(this@MainActivity)
                FocusNotifier.refresh(this@MainActivity)
            }
        }
    }

    private fun refreshPermissions() {
        permissions = PermissionsState(
            overlay = Settings.canDrawOverlays(this),
            notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
            exactAlarms = ReminderScheduler.canScheduleExact(this),
        )
    }

    /** Lleva directo al ajuste "Mostrar sobre otras apps" de Hilvana. */
    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()))
    }

    /** Lleva al ajuste "Alarmas y recordatorios" (solo existe en Android 12+). */
    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()))
        }
    }

    /** Información de la app: batería, inicio automático (Xiaomi), etc. */
    private fun openAppDetails() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
    }

    /** Android 13+: pide al sistema agregar el mosaico "Capturar" con un diálogo. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddCaptureTile() {
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, CaptureTileService::class.java),
            getString(R.string.capture_label),
            Icon.createWithResource(this, R.drawable.ic_capture),
            mainExecutor,
        ) { result ->
            val message = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                    "El mosaico \"Capturar\" está en tus Ajustes rápidos"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> null
                // Algunos fabricantes no lo permiten desde la app.
                else -> "Agrégalo a mano: baja la barra, toca el lápiz ✏️ y arrastra \"Capturar\""
            }
            message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun packageUri(): Uri = Uri.parse("package:$packageName")

    private companion object {
        const val RELOCK_AFTER_MS = 60_000L
    }
}
