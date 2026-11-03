package com.ubayanda.milonative.data

import android.content.Context

/** The F-Droid build carries no Google libraries, so "Continue with Google" is simply not offered. */
object GoogleSignIn {
    const val available = false
    const val CLIENT_ID = ""
    suspend fun idToken(context: Context): String = throw IllegalStateException("Google sign-in isn't part of this build")
}

class CancelledSignIn : Exception()
