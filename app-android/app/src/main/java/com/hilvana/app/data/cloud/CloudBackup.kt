package com.hilvana.app.data.cloud

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hilvana.app.data.UserPrefs
import com.hilvana.app.data.backup.BackupManager
import com.hilvana.app.data.backup.BackupResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Respaldo en Google Drive: el mismo .zip del respaldo manual (datos, imágenes y
 * audios), subido a la carpeta oculta de la app en el Drive del usuario.
 * Restaurar SUMA a lo que hay (nunca borra), igual que importar.
 */
object CloudBackup {

    private const val WORK_NAME = "cloud-backup-daily"

    val Context.isConnected: Boolean get() = UserPrefs(this).googleEmail.isNotBlank()

    /** Tras dar permiso: guarda la cuenta y activa el respaldo diario. */
    suspend fun onConnected(context: Context, token: String) = withContext(Dispatchers.IO) {
        val profile = DriveApi(token).profile()
        val prefs = UserPrefs(context)
        prefs.googleEmail = profile.email ?: "Cuenta de Google"
        prefs.googleName = profile.name.orEmpty()
        if (prefs.userName.isBlank() && !profile.name.isNullOrBlank()) prefs.userName = profile.name.substringBefore(' ')
        schedule(context)
    }

    suspend fun backupNow(context: Context, token: String): BackupResult = withContext(Dispatchers.IO) {
        val zip = File(context.cacheDir, DriveApi.BACKUP_NAME)
        try {
            val result = BackupManager.export(context, Uri.fromFile(zip))
            DriveApi(token).upload(zip)
            UserPrefs(context).cloudLastBackupAt = System.currentTimeMillis()
            result
        } finally {
            zip.delete()
        }
    }

    /** Baja el respaldo de Drive y lo importa. null si todavía no hay ninguno en Drive. */
    suspend fun restore(context: Context, token: String): BackupResult? = withContext(Dispatchers.IO) {
        val api = DriveApi(token)
        val file = api.findBackup() ?: return@withContext null
        val zip = File(context.cacheDir, "restaurar-${DriveApi.BACKUP_NAME}")
        try {
            api.download(file.id, zip)
            BackupManager.import(context, Uri.fromFile(zip))
        } finally {
            zip.delete()
        }
    }

    suspend fun disconnect(context: Context, token: String?) = withContext(Dispatchers.IO) {
        token?.let { DriveApi(it).revoke() }
        val prefs = UserPrefs(context)
        prefs.googleEmail = ""
        prefs.googleName = ""
        prefs.cloudLastBackupAt = 0
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Respaldo diario solo con internet (y sin gastar batería si está baja). */
    fun schedule(context: Context) {
        val prefs = UserPrefs(context)
        val work = WorkManager.getInstance(context)
        if (!context.isConnected || !prefs.cloudAutoBackup) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

/** Trabajo en segundo plano del respaldo diario. */
class CloudBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (UserPrefs(applicationContext).googleEmail.isBlank()) return Result.success()
        return runCatching {
            when (val auth = GoogleAuth.authorize(applicationContext)) {
                // En segundo plano no se puede mostrar la pantalla de Google: se intenta otro día.
                is GoogleAuth.Result.NeedsUser -> Result.success()
                is GoogleAuth.Result.Token -> {
                    CloudBackup.backupNow(applicationContext, auth.accessToken)
                    Result.success()
                }
            }
        }.getOrElse { Result.retry() }
    }
}
