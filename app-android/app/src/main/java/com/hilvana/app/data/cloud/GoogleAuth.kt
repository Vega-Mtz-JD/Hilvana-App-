package com.hilvana.app.data.cloud

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Permiso de Google para guardar respaldos en la carpeta oculta de Drive de la
 * app (drive.appdata) y ver el correo/nombre de la cuenta. Hilvana nunca ve tu
 * contraseña ni tus archivos de Drive: solo su propia carpeta.
 */
object GoogleAuth {

    private val SCOPES = listOf(
        Scope("https://www.googleapis.com/auth/drive.appdata"),
        Scope("openid"),
        Scope("email"),
        Scope("profile"),
    )

    sealed interface Result {
        data class Token(val accessToken: String) : Result
        /** Hay que mostrarle al usuario la pantalla de Google (elegir cuenta / dar permiso). */
        data class NeedsUser(val intent: PendingIntent) : Result
    }

    /** Pide el permiso; si ya se dio antes, devuelve el token sin mostrar nada. */
    suspend fun authorize(context: Context): Result {
        val request = AuthorizationRequest.builder().setRequestedScopes(SCOPES).build()
        val result = Identity.getAuthorizationClient(context).authorize(request).await()
        return toResult(result)
    }

    /** Lee lo que devolvió la pantalla de Google. */
    fun fromIntent(context: Context, data: Intent?): Result? = runCatching {
        toResult(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data))
    }.getOrNull()

    private fun toResult(result: AuthorizationResult): Result {
        val pending = result.pendingIntent
        val token = result.accessToken
        return when {
            result.hasResolution() && pending != null -> Result.NeedsUser(pending)
            token != null -> Result.Token(token)
            else -> error("Google no devolvió permiso")
        }
    }

    /** Espera una tarea de Play Services sin librerías extra. */
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
