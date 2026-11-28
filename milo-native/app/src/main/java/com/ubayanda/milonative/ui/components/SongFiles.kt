package com.ubayanda.milonative.ui.components

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ubayanda.milonative.data.MetaEdits
import com.ubayanda.milonative.data.TagWriter
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.ErrorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Deleting a song from the phone and writing its tags into the file. Android asks the person to allow each change (Android 10 and newer) or the storage permission once (Android 8-9). */
class SongFiles internal constructor(
    val delete: (Track) -> Unit,
    val saveTags: (Track, String, String, String) -> Unit,
)

private class TagJob(val t: Track, val title: String, val artist: String, val album: String)

@Composable
fun rememberSongFiles(vm: MainViewModel): SongFiles {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Track?>(null) }
    var pendingDelete by remember { mutableStateOf<Track?>(null) }
    var pendingTags by remember { mutableStateOf<TagJob?>(null) }

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_LONG).show()
    fun hasLegacyWrite() = Build.VERSION.SDK_INT > 28 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    lateinit var runWrite: (TagJob) -> Unit
    lateinit var runDelete: (Track) -> Unit

    val sender = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val d = pendingDelete; val j = pendingTags
        pendingDelete = null; pendingTags = null
        if (r.resultCode != Activity.RESULT_OK) { toast("Nothing was changed"); return@rememberLauncherForActivityResult }
        // Android 11+: the system already deleted the file when the person agreed. Android 10: the grant lets us do it now.
        if (d != null) { if (Build.VERSION.SDK_INT >= 30) vm.songsDeleted(listOf(d)) else runDelete(d) }
        if (j != null) runWrite(j)
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val d = pendingDelete; val j = pendingTags
        pendingDelete = null; pendingTags = null
        if (!ok) { toast("milo. needs the storage permission to change files on Android 9 and older"); return@rememberLauncherForActivityResult }
        if (d != null) runDelete(d)
        if (j != null) runWrite(j)
    }

    fun openSender(intentSender: android.content.IntentSender) { sender.launch(IntentSenderRequest.Builder(intentSender).build()) }

    runDelete = { t ->
        val uri = Uri.parse(t.uri)
        try {
            when {
                Build.VERSION.SDK_INT >= 30 -> { pendingDelete = t; openSender(MediaStore.createDeleteRequest(ctx.contentResolver, listOf(uri)).intentSender) }
                !hasLegacyWrite() -> { pendingDelete = t; perm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                else -> if (ctx.contentResolver.delete(uri, null, null) > 0) vm.songsDeleted(listOf(t)) else toast("Couldn't delete that song")
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) { pendingDelete = t; openSender(e.userAction.actionIntent.intentSender) }
            else { ErrorLog.swallowed(e); toast("Couldn't delete that song") }
        } catch (e: Throwable) { ErrorLog.swallowed(e); toast("Couldn't delete that song") }
    }

    runWrite = { j ->
        val uri = Uri.parse(j.t.uri)
        scope.launch {
            var ask: android.content.IntentSender? = null
            val result = withContext(Dispatchers.IO) {
                try { if (TagWriter.write(ctx, uri, j.title, j.artist, j.album)) "ok" else "no" }
                catch (e: SecurityException) {
                    // Android 10 asks for access to this one file through the exception itself
                    if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) { ask = e.userAction.actionIntent.intentSender; "ask" }
                    else { ErrorLog.swallowed(e); "no" }
                }
            }
            when (result) {
                "ok" -> { vm.resetInfo(j.t.id); toast("Saved in the file") }
                "ask" -> { pendingTags = j; ask?.let { openSender(it) } }
                else -> { vm.editInfo(j.t.id, j.title, j.artist, j.album); toast("This file can't be edited, so the new details are saved inside milo. only") }
            }
        }
    }

    confirm?.let { t ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Milo.card,
            title = { Text("Delete from this phone?", color = Milo.text) },
            text = { Text("\"${t.title}\" will be removed from your storage. This can't be undone.", color = Milo.dim) },
            confirmButton = { TextButton(onClick = { confirm = null; runDelete(t) }) { Text("Delete", color = Color(0xFFFF8A80)) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = Milo.dim) } },
        )
    }

    return SongFiles(
        delete = { t -> if (t.uri.startsWith("content:")) confirm = t else toast("Only songs on this phone can be deleted") },
        saveTags = { t, n, a, al ->
            val job = TagJob(t, n, a, al)
            val uri = Uri.parse(t.uri)
            when {
                Build.VERSION.SDK_INT >= 30 -> try { pendingTags = job; openSender(MediaStore.createWriteRequest(ctx.contentResolver, listOf(uri)).intentSender) } catch (e: Throwable) { pendingTags = null; ErrorLog.swallowed(e); toast("Couldn't ask for access") }
                !hasLegacyWrite() -> { pendingTags = job; perm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                else -> runWrite(job)
            }
        },
    )
}
