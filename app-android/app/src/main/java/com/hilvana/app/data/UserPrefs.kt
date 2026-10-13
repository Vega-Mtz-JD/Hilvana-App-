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

    private companion object {
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_USER_NAME = "user_name"
    }
}
