package com.hilvana.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hilvana.app.data.Folder
import com.hilvana.app.data.Item
import com.hilvana.app.repository
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.hilvana.app.data.cloud.CloudBackup
import com.hilvana.app.data.cloud.GoogleAuth
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

    // ---------- Respaldo en Google Drive ----------

    data class CloudState(
        val email: String,
        val name: String,
        val lastBackupAt: Long,
        val autoBackup: Boolean,
        val busy: Boolean = false,
    ) {
        val connected: Boolean get() = email.isNotBlank()
    }

    private enum class CloudAction { CONNECT, BACKUP, RESTORE }

    private fun readCloudState(busy: Boolean = false) =
        CloudState(prefs.googleEmail, prefs.googleName, prefs.cloudLastBackupAt, prefs.cloudAutoBackup, busy)

    private val _cloud = MutableStateFlow(readCloudState())
    val cloud: StateFlow<CloudState> = _cloud.asStateFlow()

    /** Pantalla de Google que hay que mostrar (elegir cuenta / dar permiso); la UI la lanza. */
    private val _cloudAuthIntent = MutableStateFlow<PendingIntent?>(null)
    val cloudAuthIntent: StateFlow<PendingIntent?> = _cloudAuthIntent.asStateFlow()
    private var pendingCloudAction: CloudAction? = null

    fun connectGoogle() = runCloud(CloudAction.CONNECT)
    fun cloudBackupNow() = runCloud(CloudAction.BACKUP)
    fun cloudRestore() = runCloud(CloudAction.RESTORE)

    fun setCloudAutoBackup(enabled: Boolean) {
        prefs.cloudAutoBackup = enabled
        CloudBackup.schedule(getApplication())
        _cloud.value = readCloudState()
    }

    fun disconnectGoogle() {
        viewModelScope.launch {
            val token = (runCatching { GoogleAuth.authorize(getApplication()) }.getOrNull() as? GoogleAuth.Result.Token)
                ?.accessToken
            CloudBackup.disconnect(getApplication(), token)
            _cloud.value = readCloudState()
            toast("Cuenta de Google desconectada")
        }
    }

    /** La UI ya mostró la pantalla de Google: seguir con lo que se estaba haciendo. */
    fun onCloudAuthResult(data: Intent?) {
        _cloudAuthIntent.value = null
        val action = pendingCloudAction ?: return
        pendingCloudAction = null
        when (val result = GoogleAuth.fromIntent(getApplication(), data)) {
            is GoogleAuth.Result.Token -> viewModelScope.launch { perform(action, result.accessToken) }
            else -> {
                _cloud.value = readCloudState()
                toast("No se conectó la cuenta de Google")
            }
        }
    }

    private fun runCloud(action: CloudAction) {
        if (_cloud.value.busy) return
        viewModelScope.launch {
            _cloud.value = readCloudState(busy = true)
            try {
                when (val auth = GoogleAuth.authorize(getApplication())) {
                    is GoogleAuth.Result.Token -> perform(action, auth.accessToken)
                    is GoogleAuth.Result.NeedsUser -> {
                        pendingCloudAction = action
                        _cloudAuthIntent.value = auth.intent
                    }
                }
            } catch (e: Exception) {
                _cloud.value = readCloudState()
                toast("Google no respondió: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private suspend fun perform(action: CloudAction, token: String) {
        _cloud.value = readCloudState(busy = true)
        val app = getApplication<Application>()
        val message = try {
            when (action) {
                CloudAction.CONNECT -> {
                    CloudBackup.onConnected(app, token)
                    val result = CloudBackup.backupNow(app, token)
                    "Conectado ✓ Primer respaldo en Drive: ${result.items} elementos"
                }
                CloudAction.BACKUP -> {
                    val result = CloudBackup.backupNow(app, token)
                    "Respaldo en Drive listo: ${result.items} elementos y ${result.images} imágenes"
                }
                CloudAction.RESTORE -> {
                    val result = CloudBackup.restore(app, token)
                    if (result == null) {
                        "Todavía no hay ningún respaldo de Hilvana en tu Drive"
                    } else {
                        "Restaurado desde Drive: ${result.items} elementos" +
                            if (result.skipped > 0) " (${result.skipped} ya estaban)" else ""
                    }
                }
            }
        } catch (e: Exception) {
            "No se pudo completar: ${e.message ?: e.javaClass.simpleName}"
        }
        _cloud.value = readCloudState()
        toast(message)
    }

    private fun toast(message: String) =
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()

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

    // ---------- Tareas en prioridad ----------

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
