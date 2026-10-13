package com.hilvana.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hilvana.app.bubble.BubbleService
import com.hilvana.app.reminder.ReminderScheduler
import com.hilvana.app.ui.HilvanaRoot
import com.hilvana.app.ui.screens.PermissionsState
import com.hilvana.app.ui.theme.HilvanaTheme

class MainActivity : ComponentActivity() {

    // Se relee en onResume: el usuario puede regresar de Ajustes con el permiso dado.
    private var permissions by mutableStateOf(PermissionsState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HilvanaTheme {
                val bubbleRunning by BubbleService.isRunning.collectAsStateWithLifecycle()
                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { refreshPermissions() }

                HilvanaRoot(
                    permissions = permissions,
                    bubbleRunning = bubbleRunning,
                    onRequestOverlay = ::openOverlaySettings,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestExactAlarms = ::openExactAlarmSettings,
                    onOpenAppSettings = ::openAppDetails,
                    onToggleBubble = {
                        if (bubbleRunning) BubbleService.stop(this) else BubbleService.start(this)
                    },
                )
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

    private fun packageUri(): Uri = Uri.parse("package:$packageName")
}
