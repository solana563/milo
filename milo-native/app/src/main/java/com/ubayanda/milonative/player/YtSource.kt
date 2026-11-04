package com.ubayanda.milonative.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.ubayanda.milonative.data.YtMusic
import java.io.IOException

/**
 * Lets the player open "yt:<video id>" addresses. When the player opens one, the address is swapped for a fresh direct audio link
 * (looked up on the loader thread, cached until it nears its expiry). Everything else (files, radio, previews) opens exactly as before.
 * Every player that should play YouTube songs (the main one and the crossfade one) is built with [mediaSourceFactory].
 */
@OptIn(UnstableApi::class)
object YtSource {
    private val resolver = object : ResolvingDataSource.Resolver {
        override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
            val u = dataSpec.uri
            if (u.scheme != "yt") return dataSpec
            val id = u.schemeSpecificPart.orEmpty()
            // saved on the phone: play the file, no connection needed
            com.ubayanda.milonative.data.OfflineStore.fileOf(id)?.let { return dataSpec.buildUpon().setUri(Uri.fromFile(it)).build() }
            val s = try {
                YtMusic.stream(id)
            } catch (e: IOException) {
                com.ubayanda.milonative.util.ErrorLog.add("stream " + id, e.message)
                throw e
            } catch (e: Exception) {
                com.ubayanda.milonative.util.ErrorLog.add("stream " + id, e.javaClass.simpleName + ": " + e.message)
                throw IOException(e.message ?: "YouTube lookup failed", e)
            }
            return dataSpec.buildUpon()
                .setUri(Uri.parse(s.url))
                .setHttpRequestHeaders(dataSpec.httpRequestHeaders + ("User-Agent" to YtMusic.STREAM_UA))
                .build()
        }
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        YtMusic.init(context)
        com.ubayanda.milonative.data.OfflineStore.init(context)
        val http = DefaultHttpDataSource.Factory().setConnectTimeoutMs(12_000).setReadTimeoutMs(20_000).setAllowCrossProtocolRedirects(true)
        return ResolvingDataSource.Factory(DefaultDataSource.Factory(context, http), resolver)
    }

    fun mediaSourceFactory(context: Context) = DefaultMediaSourceFactory(dataSourceFactory(context))
}
