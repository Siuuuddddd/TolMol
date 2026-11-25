package com.tolmol.app.feature.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tolmol.app.BuildConfig
import kotlinx.coroutines.CancellationException

class GoogleSignInController(private val activity: Activity) {
    suspend fun google(): String {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) { "Google sign-in is not configured yet" }
        val manager = CredentialManager.create(activity)
        suspend fun get(authorized: Boolean): String {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(authorized)
                .setAutoSelectEnabled(authorized).build()
            val result = manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = result.credential
            check(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Select a Google account to continue." }
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        return try { try { get(true) } catch (_: NoCredentialException) { get(false) } }
        catch (_: GetCredentialCancellationException) { throw CancellationException("Sign-in cancelled") }
        catch (_: NoCredentialException) { throw IllegalStateException("Add or sign back into a Google account on this device, then retry.") }
        catch (error: androidx.credentials.exceptions.GetCredentialException) {
            val network = error.type.contains("NETWORK",true) || error.message.orEmpty().contains("network",true) || error.cause is java.io.IOException
            throw IllegalStateException(if (network) "Check your connection and retry Google sign-in." else "Google sign-in could not finish. Check Google Play services and retry.")
        }
    }

}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
