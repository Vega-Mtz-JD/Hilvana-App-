package com.hilvana.app.ui.onboarding

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Fabricantes que cierran apps en segundo plano (plan §7.2, dontkillmyapp.com).
 * En ellos hay que guiar al usuario a "Inicio automático" y "Sin restricciones".
 */
object BatteryHelp {

    private val AGGRESSIVE = setOf(
        "xiaomi", "redmi", "poco", "huawei", "honor", "oppo", "vivo", "realme", "oneplus", "samsung", "meizu",
    )

    val isAggressiveManufacturer: Boolean
        get() = Build.MANUFACTURER.lowercase() in AGGRESSIVE || Build.BRAND.lowercase() in AGGRESSIVE

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    /** Xiaomi: pantalla de "Inicio automático". En otros (o si falla): información de la app. */
    fun openAutostart(context: Context) {
        if (isXiaomi) {
            val miui = Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                )
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(miui) }.isSuccess) return
        }
        openAppDetails(context)
    }

    /** Información de la app: batería, permisos, inicio automático en algunas marcas. */
    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
