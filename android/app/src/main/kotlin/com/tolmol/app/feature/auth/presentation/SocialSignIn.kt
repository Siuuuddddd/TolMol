package com.tolmol.app.feature.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tolmol.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SocialSignIn(private val activity: Activity) {
    private val callbacks = CallbackManager.Factory.create()
    private var pending: kotlinx.coroutines.CancellableContinuation<String>? = null

    suspend fun google(): String {
        check(BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()) { "Google sign-in needs the TolMol OAuth client ID." }
        val manager = CredentialManager.create(activity)
        suspend fun get(authorized: Boolean): String {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .setFilterByAuthorizedAccounts(authorized)
                .setAutoSelectEnabled(authorized).build()
            val result = manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = result.credential
            check(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Select a Google account to continue." }
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        return try { try { get(true) } catch (_: NoCredentialException) { get(false) } }
        catch (_: GetCredentialCancellationException) { throw CancellationException("Sign-in cancelled") }
    }

    @Suppress("DEPRECATION")
    suspend fun facebook(): String {
        check(BuildConfig.FACEBOOK_APP_ID.isNotBlank() && BuildConfig.FACEBOOK_CLIENT_TOKEN.isNotBlank()) {
            "Facebook sign-in needs the TolMol Facebook app configuration."
        }
        if (!FacebookSdk.isInitialized()) {
            FacebookSdk.setApplicationId(BuildConfig.FACEBOOK_APP_ID)
            FacebookSdk.setClientToken(BuildConfig.FACEBOOK_CLIENT_TOKEN)
            FacebookSdk.sdkInitialize(activity.applicationContext)
            FacebookSdk.fullyInitialize()
        }
        return suspendCancellableCoroutine { continuation ->
            check(pending == null) { "A sign-in request is already open." }
            pending = continuation
            val manager = LoginManager.getInstance()
            manager.registerCallback(callbacks, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    pending = null
                    manager.unregisterCallback(callbacks)
                    if (continuation.isActive) continuation.resume(result.accessToken.token)
                }
                override fun onCancel() {
                    pending = null
                    manager.unregisterCallback(callbacks)
                    continuation.cancel()
                }
                override fun onError(error: FacebookException) {
                    pending = null
                    manager.unregisterCallback(callbacks)
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Facebook sign-in failed. Retry."))
                }
            })
            continuation.invokeOnCancellation { pending = null; manager.unregisterCallback(callbacks) }
            manager.logInWithReadPermissions(activity,listOf("email","public_profile"))
        }
    }

    fun onActivityResult(requestCode: Int,resultCode: Int,data: Intent?): Boolean =
        callbacks.onActivityResult(requestCode,resultCode,data)
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
