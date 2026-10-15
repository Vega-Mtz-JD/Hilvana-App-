package com.hilvana.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.repository
import android.net.Uri
import android.widget.Toast
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.focus.FocusNudges
import com.hilvana.app.widget.refreshWidgets
import com.hilvana.app.data.backup.BackupManager
import com.hilvana.app.data.backup.InvalidBackupException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado y acciones de las pantallas. Sobrevive a giros de pantalla. */
class ItemsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.repository

    val items: StateFlow<List<Item>> = repository.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Vistas previas de enlaces que quedaron pendientes (notas viejas o sin internet).
        viewModelScope.launch { repository.backfillLinkPreviews() }
    }

    val folders: StateFlow<List<Folder>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveFolder(folder: Folder) {
        viewModelScope.launch { repository.saveFolder(folder) }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch { repository.deleteFolder(folder) }
    }

    // ---------- Respaldo ----------

    private val prefs = UserPrefs(application)
    private val _lastBackupAt = MutableStateFlow(prefs.lastBackupAt)
    val lastBackupAt: StateFlow<Long> = _lastBackupAt.asStateFlow()
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    fun exportBackup(uri: Uri) = runBackup {
        val result = BackupManager.export(getApplication(), uri)
        prefs.lastBackupAt = System.currentTimeMillis()
        _lastBackupAt.value = prefs.lastBackupAt
        "Respaldo guardado: ${result.items} elementos y ${result.images} imágenes"
    }

    fun importBackup(uri: Uri) = runBackup {
        val result = BackupManager.import(getApplication(), uri)
        buildString {
            append("Importados ${result.items} elementos")
            if (result.images > 0) append(" y ${result.images} imágenes")
            if (result.skipped > 0) append(" (${result.skipped} ya estaban)")
        }
    }

    private fun runBackup(action: suspend () -> String) {
        if (_backupBusy.value) return
        viewModelScope.launch {
            _backupBusy.value = true
            val message = try {
                action()
            } catch (e: InvalidBackupException) {
                e.message ?: "Respaldo no válido"
            } catch (e: Exception) {
                "No se pudo completar: ${e.message ?: e.javaClass.simpleName}"
            }
            _backupBusy.value = false
            Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
        }
    }

    // ---------- Vista previa de enlaces ----------

    private val _linkPreviews = MutableStateFlow(prefs.linkPreviews)
    val linkPreviews: StateFlow<Boolean> = _linkPreviews.asStateFlow()

    fun setLinkPreviews(enabled: Boolean) {
        prefs.linkPreviews = enabled
        _linkPreviews.value = enabled
        if (enabled) viewModelScope.launch { repository.backfillLinkPreviews() }
    }

    // ---------- Meta diaria ----------

    private val _dailyGoal = MutableStateFlow(prefs.dailyGoal)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    fun setDailyGoal(goal: Int) {
        prefs.dailyGoal = goal
        _dailyGoal.value = goal
        viewModelScope.launch { refreshWidgets(getApplication()) }
    }

    // ---------- Tareas en foco ----------

    private val _nudgeHours = MutableStateFlow(prefs.focusNudgeHours)
    val nudgeHours: StateFlow<Int> = _nudgeHours.asStateFlow()

    fun setNudgeHours(hours: Int) {
        prefs.focusNudgeHours = hours
        _nudgeHours.value = hours
        FocusNudges.reschedule(getApplication())
    }

    fun save(item: Item) {
        viewModelScope.launch { repository.save(item) }
    }

    fun delete(item: Item) {
        viewModelScope.launch { repository.delete(item) }
    }

    fun toggleDone(item: Item) {
        viewModelScope.launch { repository.save(item.copy(done = !item.done)) }
    }

    /** Sacar de la Bandeja: ya se revisó/ordenó. */
    fun markReviewed(item: Item) {
        viewModelScope.launch { repository.save(item.copy(inInbox = false)) }
    }
}
