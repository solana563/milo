package com.ubayanda.milonative.push

import android.content.Context

/** This build has no Google libraries, so there is no push: messages are checked every 15 minutes instead. */
object Push {
    fun available(ctx: Context): Boolean = false
    fun init(ctx: Context) {}
    fun sync(ctx: Context) {}
    fun unregister(ctx: Context) {}
    suspend fun unregisterNow(ctx: Context) {}
    fun active(ctx: Context): Boolean = false
    fun enabled(ctx: Context): Boolean = false
    fun setEnabled(ctx: Context, on: Boolean) {}
}
