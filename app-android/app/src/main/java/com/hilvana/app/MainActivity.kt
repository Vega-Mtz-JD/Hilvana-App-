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
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {

    // Se relee en onResume: el usuario puede regresar de Ajustes con el permiso dado.
    private var permissions by mutableStateOf(PermissionsState())

    private val prefs by lazy { UserPrefs(this) }
    private var onboardingDone by mutableStateOf(true)
    private var userName by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onboardingDone = prefs.onboardingDone
        userName = prefs.userName

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

                if (!onboardingDone) {
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
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
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
}
