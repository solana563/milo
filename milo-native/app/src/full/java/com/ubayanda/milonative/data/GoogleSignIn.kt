package com.ubayanda.milonative.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** "Continue with Google": asks Android for a Google ID token for milo.'s web client id. The server (Supabase) then trusts it. */
object GoogleSignIn {
    const val available = true
    const val CLIENT_ID = "607031598574-svt5i48ppk9udu7mujo76k2j89j2qk3u.apps.googleusercontent.com"

    private fun activityOf(c: Context): Context {
        var x = c
        while (x is ContextWrapper) { if (x is Activity) return x; x = x.baseContext }
        return c
    }

    /** Returns the ID token, or throws with a message that can be shown (null message = the person cancelled). */
    suspend fun idToken(context: Context): String {
        val ctx = activityOf(context)
        val opt = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(CLIENT_ID).setAutoSelectEnabled(false).build()
        val req = GetCredentialRequest.Builder().addCredentialOption(opt).build()
        try {
            val cred = CredentialManager.create(ctx).getCredential(ctx, req).credential
            if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) return GoogleIdTokenCredential.createFrom(cred.data).idToken
            throw IllegalStateException("Google sign-in returned something unexpected")
        } catch (e: GetCredentialCancellationException) {
            throw CancelledSignIn()
        } catch (e: NoCredentialException) {
            throw IllegalStateException("No Google account on this phone. Add one in Settings, then try again.")
        } catch (e: GetCredentialException) {
            throw IllegalStateException(e.message ?: "Google sign-in failed")
        }
    }
}

class CancelledSignIn : Exception()
