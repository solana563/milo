package com.ubayanda.milonative.player

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * A tiny web server on the phone so a TV or speaker can fetch songs that only exist as files on the phone while casting.
 * It only runs while casting, hands out songs through a secret link, and supports seeking (byte ranges).
 */
class LocalAudioServer(private val ctx: Context) {
    private var server: ServerSocket? = null
    private val token = UUID.randomUUID().toString().replace("-", "").take(12)
    private val songs = ConcurrentHashMap<String, Uri>()

    fun start() {
        if (server != null) return
        val ss = ServerSocket(0)
        server = ss
        thread(isDaemon = true, name = "milo-cast-server") {
            while (!ss.isClosed) {
                val s = try { ss.accept() } catch (e: Exception) { break }
                thread(isDaemon = true) { try { handle(s) } catch (_: Exception) {} }
            }
        }
    }

    fun stop() {
        try { server?.close() } catch (_: Exception) {}
        server = null
        songs.clear()
    }

    private fun ip(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }?.hostAddress
    } catch (e: Exception) { null }

    /** Address the cast device can use for this song, or null if the phone has no Wi-Fi address. */
    fun urlFor(id: String, uri: Uri): String? {
        val host = ip() ?: return null
        val port = server?.localPort ?: return null
        songs[id] = uri
        return "http://$host:$port/$token/" + URLEncoder.encode(id, "UTF-8")
    }

    private fun handle(s: Socket) = s.use { sock ->
        val input = BufferedReader(InputStreamReader(sock.getInputStream()))
        val first = input.readLine() ?: return@use
        val parts = first.split(" ")
        if (parts.size < 2) return@use
        val head = parts[0] == "HEAD"
        var range: String? = null
        while (true) {
            val l = input.readLine() ?: break
            if (l.isEmpty()) break
            if (l.startsWith("Range:", true)) range = l.substringAfter(":").trim()
        }
        val out = sock.getOutputStream()
        val seg = parts[1].trim('/').split("/")
        val uri = if (seg.size == 2 && seg[0] == token) songs[URLDecoder.decode(seg[1], "UTF-8")] else null
        if (uri == null) { out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()); return@use }
        val afd = ctx.contentResolver.openAssetFileDescriptor(uri, "r")
        if (afd == null) { out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()); return@use }
        afd.use { fd ->
            val total = fd.length
            val mime = ctx.contentResolver.getType(uri) ?: "audio/mpeg"
            var start = 0L
            var end = total - 1
            var partial = false
            val m = range?.let { Regex("""bytes=(\d*)-(\d*)""").find(it) }
            if (m != null && total > 0) {
                val a = m.groupValues[1]; val b = m.groupValues[2]
                if (a.isNotEmpty()) { start = a.toLong(); if (b.isNotEmpty()) end = minOf(b.toLong(), total - 1) }
                else if (b.isNotEmpty()) { start = maxOf(0L, total - b.toLong()) }
                partial = true
            }
            if (start >= total && total > 0) { out.write("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()); return@use }
            val len = if (total > 0) end - start + 1 else -1L
            val sb = StringBuilder(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
            sb.append("Content-Type: $mime\r\nAccept-Ranges: bytes\r\nConnection: close\r\n")
            if (len >= 0) sb.append("Content-Length: $len\r\n")
            if (partial) sb.append("Content-Range: bytes $start-$end/$total\r\n")
            sb.append("\r\n")
            out.write(sb.toString().toByteArray())
            if (head) return@use
            val ins = fd.createInputStream()
            ins.channel.position(fd.startOffset + start)
            val buf = ByteArray(64 * 1024)
            var left = if (len >= 0) len else Long.MAX_VALUE
            while (left > 0) {
                val n = ins.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                if (n < 0) break
                out.write(buf, 0, n)
                left -= n
            }
            out.flush()
        }
    }
}
