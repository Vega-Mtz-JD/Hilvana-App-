package com.hilvana.app.data

import android.content.Context
import androidx.core.content.edit
import com.hilvana.app.data.update.AppRelease
import com.hilvana.app.ui.search.SortOrder
import com.hilvana.app.ui.theme.ThemeMode

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

    /** Carpetas usadas hace poco, la última primero (para elegirla sola al capturar). */
    val recentFolderIds: List<Long>
        get() = prefs.getString(KEY_RECENT_FOLDERS, "").orEmpty()
            .split(',').mapNotNull { it.toLongOrNull() }

    fun rememberFolderUse(folderId: Long) {
        val updated = (listOf(folderId) + recentFolderIds.filter { it != folderId }).take(5)
        prefs.edit { putString(KEY_RECENT_FOLDERS, updated.joinToString(",")) }
    }

    /** Cuenta de Google conectada para el respaldo en Drive (vacío = no conectada). */
    var googleEmail: String
        get() = prefs.getString(KEY_GOOGLE_EMAIL, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_GOOGLE_EMAIL, value) }

    var googleName: String
        get() = prefs.getString(KEY_GOOGLE_NAME, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_GOOGLE_NAME, value) }

    /** Respaldo automático diario en Drive. */
    var cloudAutoBackup: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_AUTO, true)
        set(value) = prefs.edit { putBoolean(KEY_CLOUD_AUTO, value) }

    /** Cuándo se subió el último respaldo a Drive (0 = nunca). */
    var cloudLastBackupAt: Long
        get() = prefs.getLong(KEY_CLOUD_LAST, 0L)
        set(value) = prefs.edit { putLong(KEY_CLOUD_LAST, value) }

    /** Apariencia: claro, oscuro o como el sistema. */
    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

    /** Usar los colores del fondo de pantalla (Material You) en vez de los de Hilvana. */
    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) = prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, value) }

    /** Traer título y miniatura de los enlaces guardados (necesita internet). */
    var linkPreviews: Boolean
        get() = prefs.getBoolean(KEY_LINK_PREVIEWS, true)
        set(value) = prefs.edit { putBoolean(KEY_LINK_PREVIEWS, value) }

    /** Avisar cuando hay una versión de prueba nueva en GitHub. */
    var updateChecks: Boolean
        get() = prefs.getBoolean(KEY_UPDATE_CHECKS, true)
        set(value) = prefs.edit { putBoolean(KEY_UPDATE_CHECKS, value) }

    var lastUpdateCheckAt: Long
        get() = prefs.getLong(KEY_UPDATE_LAST_CHECK, 0L)
        set(value) = prefs.edit { putLong(KEY_UPDATE_LAST_CHECK, value) }

    /** La última versión encontrada en GitHub (para no preguntar a cada rato). */
    var latestRelease: AppRelease?
        get() = prefs.getString(KEY_UPDATE_VERSION, null)?.let { version ->
            AppRelease(
                version = version,
                pageUrl = prefs.getString(KEY_UPDATE_PAGE, "").orEmpty(),
                apkUrl = prefs.getString(KEY_UPDATE_APK, null),
                notes = prefs.getString(KEY_UPDATE_NOTES, "").orEmpty(),
            )
        }
        set(value) = prefs.edit {
            putString(KEY_UPDATE_VERSION, value?.version)
            putString(KEY_UPDATE_PAGE, value?.pageUrl)
            putString(KEY_UPDATE_APK, value?.apkUrl)
            putString(KEY_UPDATE_NOTES, value?.notes)
        }

    /** Versión cuyo aviso el usuario cerró con "Después". */
    var dismissedUpdate: String
        get() = prefs.getString(KEY_UPDATE_DISMISSED, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_UPDATE_DISMISSED, value) }

    /** Cómo se ordenan las notas en la pestaña Notas. */
    var notesSort: SortOrder
        get() = SortOrder.entries.firstOrNull { it.name == prefs.getString(KEY_NOTES_SORT, null) } ?: SortOrder.NEWEST
        set(value) = prefs.edit { putString(KEY_NOTES_SORT, value.name) }

    /** Meta diaria: cuántas cosas completar al día (0 = apagada). */
    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 3)
        set(value) = prefs.edit { putInt(KEY_DAILY_GOAL, value) }

    /** Empujoncitos de las tareas en prioridad: cada cuántas horas (0 = apagados). */
    var focusNudgeHours: Int
        get() = prefs.getInt(KEY_NUDGE_HOURS, 0)
        set(value) = prefs.edit { putInt(KEY_NUDGE_HOURS, value) }

    /** Cuándo está programado el próximo empujoncito (0 = ninguno). */
    var nextNudgeAt: Long
        get() = prefs.getLong(KEY_NEXT_NUDGE, 0L)
        set(value) = prefs.edit { putLong(KEY_NEXT_NUDGE, value) }

    private companion object {
        const val KEY_RECENT_FOLDERS = "recent_folders"
        const val KEY_NOTES_SORT = "notes_sort"
        const val KEY_UPDATE_CHECKS = "update_checks"
        const val KEY_UPDATE_LAST_CHECK = "update_last_check"
        const val KEY_UPDATE_VERSION = "update_version"
        const val KEY_UPDATE_PAGE = "update_page"
        const val KEY_UPDATE_APK = "update_apk"
        const val KEY_UPDATE_NOTES = "update_notes"
        const val KEY_UPDATE_DISMISSED = "update_dismissed"
        const val KEY_GOOGLE_EMAIL = "google_email"
        const val KEY_GOOGLE_NAME = "google_name"
        const val KEY_CLOUD_AUTO = "cloud_auto_backup"
        const val KEY_CLOUD_LAST = "cloud_last_backup_at"
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_LINK_PREVIEWS = "link_previews"
        const val KEY_NUDGE_HOURS = "focus_nudge_hours"
        const val KEY_NEXT_NUDGE = "focus_next_nudge_at"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_USER_NAME = "user_name"
        const val KEY_APP_LOCK = "app_lock"
    }
}
