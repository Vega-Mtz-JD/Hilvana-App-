package com.hilvana.app.data

import android.content.Context
import androidx.core.content.edit

/** Preferencias simples del usuario (perfil local, plan §6.1). */
class UserPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("user", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) }

    /** Nombre del perfil local ("Hola, Diego"); vacío si no lo escribió. */
    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_USER_NAME, value.trim()) }

    /** Pedir huella/PIN al abrir la app. */
    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) = prefs.edit { putBoolean(KEY_APP_LOCK, value) }

    /** Cuándo se exportó el último respaldo (0 = nunca). */
    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_BACKUP, value) }

    /** Meta diaria: cuántas cosas completar al día (0 = apagada). */
    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 3)
        set(value) = prefs.edit { putInt(KEY_DAILY_GOAL, value) }

    /** Empujoncitos de las tareas en foco: cada cuántas horas (0 = apagados). */
    var focusNudgeHours: Int
        get() = prefs.getInt(KEY_NUDGE_HOURS, 0)
        set(value) = prefs.edit { putInt(KEY_NUDGE_HOURS, value) }

    /** Cuándo está programado el próximo empujoncito (0 = ninguno). */
    var nextNudgeAt: Long
        get() = prefs.getLong(KEY_NEXT_NUDGE, 0L)
        set(value) = prefs.edit { putLong(KEY_NEXT_NUDGE, value) }

    private companion object {
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_NUDGE_HOURS = "focus_nudge_hours"
        const val KEY_NEXT_NUDGE = "focus_next_nudge_at"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_USER_NAME = "user_name"
        const val KEY_APP_LOCK = "app_lock"
    }
}
