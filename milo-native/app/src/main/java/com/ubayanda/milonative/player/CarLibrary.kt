package com.ubayanda.milonative.player

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.ubayanda.milonative.data.MediaRepo
import com.ubayanda.milonative.data.PlaylistStore
import com.ubayanda.milonative.data.SavedStore
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.util.NaturalOrder
import java.util.concurrent.Executors

/** The last songs played, newest first. The service writes it, the car browser reads it. */
object RecentStore {
    private const val MAX = 40
    fun add(ctx: Context, id: String) {
        if (id.toLongOrNull() == null) return
        val p = ctx.getSharedPreferences("milo_recent", Context.MODE_PRIVATE)
        val list = (listOf(id) + p.getString("ids", "").orEmpty().split(',').filter { it.isNotEmpty() && it != id }).take(MAX)
        p.edit().putString("ids", list.joinToString(",")).apply()
    }
    fun ids(ctx: Context): List<Long> =
        ctx.getSharedPreferences("milo_recent", Context.MODE_PRIVATE).getString("ids", "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }
}

/**
 * What Android Auto (and other media browsers: Bluetooth head units, Wear, Assistant) can see and play.
 * Folders: Recently played, Liked songs, Playlists, Albums, Artists. Only official Media3 / MediaBrowser APIs are used,
 * and the car never gets more than a short page of items at a time, as the driving rules ask.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CarLibrary(private val ctx: Context) : MediaLibrarySession.Callback {
    private val io = Executors.newSingleThreadExecutor()
    private val repo = MediaRepo(ctx)
    @Volatile private var cache: List<Track> = emptyList()
    @Volatile private var cachedAt = 0L
    /** The folder the car last opened: tapping a song there queues that folder, like a normal player. */
    @Volatile private var lastParent = ROOT

    companion object {
        const val ROOT = "root"
        private const val RECENT = "recent"
        private const val LIKED = "liked"
        private const val PLAYLISTS = "playlists"
        private const val ALBUMS = "albums"
        private const val ARTISTS = "artists"
        private const val PAGE_CAP = 100
        private val ART = Uri.parse("content://media/external/audio/albumart")
    }

    // ---------- data ----------
    private fun all(): List<Track> {
        val now = System.currentTimeMillis()
        if (cache.isEmpty() || now - cachedAt > 60_000L) {
            val local = try { kotlinx.coroutines.runBlocking { repo.scan() } } catch (_: Throwable) { cache.filter { it.id >= 0 } }
            val saved = try { SavedStore(ctx).load().values.toList() } catch (_: Throwable) { emptyList() }
            cache = local + saved
            cachedAt = now
        }
        return cache
    }

    private fun favIds(): Set<Long> =
        ctx.getSharedPreferences("milo", Context.MODE_PRIVATE).getStringSet("favs", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    private fun byIds(ids: List<Long>): List<Track> {
        val m = all().associateBy { it.id }
        return ids.mapNotNull { m[it] }
    }

    private fun tracksOf(parent: String): List<Track> = when {
        parent == RECENT -> byIds(RecentStore.ids(ctx))
        parent == LIKED -> all().filter { it.id in favIds() }.sortedWith { a, b -> NaturalOrder.compare(a.title, b.title) }
        parent.startsWith("pl:") -> PlaylistStore(ctx).load().firstOrNull { it.id == parent.removePrefix("pl:") }?.let { byIds(it.trackIds) } ?: emptyList()
        parent.startsWith("al:") -> parent.removePrefix("al:").toLongOrNull()?.let { id -> all().filter { it.albumId == id }.sortedWith(compareBy({ it.trackNo }, { NaturalOrder.key(it.title) })) } ?: emptyList()
        parent.startsWith("ar:") -> parent.removePrefix("ar:").let { n -> all().filter { it.artist == n }.sortedWith { a, b -> NaturalOrder.compare(a.title, b.title) } }
        else -> emptyList()
    }

    private fun search(q: String): List<Track> {
        val n = q.trim().lowercase()
        if (n.isEmpty()) return emptyList()
        return all().filter { it.title.lowercase().contains(n) || it.artist.lowercase().contains(n) || it.album.lowercase().contains(n) }.take(60)
    }

    // ---------- items ----------
    private fun folder(id: String, title: String, subtitle: String? = null, art: Uri? = null, type: Int = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED): MediaItem =
        MediaItem.Builder().setMediaId(id).setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setSubtitle(subtitle).setArtworkUri(art)
                .setIsBrowsable(true).setIsPlayable(false).setMediaType(type).build()
        ).build()

    private fun song(t: Track): MediaItem = MediaItem.Builder()
        .setMediaId(t.id.toString())
        .setUri(t.uri)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album)
                .setArtworkUri(if (t.artUrl.startsWith("http")) Uri.parse(t.artUrl) else if (t.albumId > 0 && t.id >= 0) ContentUris.withAppendedId(ART, t.albumId) else null)
                .setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()
        ).build()

    private fun children(parent: String): List<MediaItem> = when (parent) {
        ROOT -> listOf(
            folder(RECENT, "Recently played"),
            folder(LIKED, "Liked songs"),
            folder(PLAYLISTS, "Playlists", type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
            folder(ALBUMS, "Albums", type = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            folder(ARTISTS, "Artists", type = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
        )
        PLAYLISTS -> PlaylistStore(ctx).load().map { folder("pl:" + it.id, it.name, it.trackIds.size.toString() + " songs", type = MediaMetadata.MEDIA_TYPE_PLAYLIST) }
        ALBUMS -> all().filter { it.id >= 0 }.groupBy { it.albumId }.values
            .map { v -> v.first().let { folder("al:" + it.albumId, it.album, it.artist, ContentUris.withAppendedId(ART, it.albumId), MediaMetadata.MEDIA_TYPE_ALBUM) } }
            .sortedWith { a, b -> NaturalOrder.compare(a.mediaMetadata.title.toString(), b.mediaMetadata.title.toString()) }
        ARTISTS -> all().filter { it.id >= 0 }.groupBy { it.artist }.keys
            .sortedWith { a, b -> NaturalOrder.compare(a, b) }.map { folder("ar:$it", it, type = MediaMetadata.MEDIA_TYPE_ARTIST) }
        else -> tracksOf(parent).map(::song)
    }

    private fun <T> async(block: () -> T): ListenableFuture<T> {
        val f = SettableFuture.create<T>()
        io.execute { try { f.set(block()) } catch (e: Throwable) { f.setException(e) } }
        return f
    }

    private fun page(list: List<MediaItem>, page: Int, size: Int): ImmutableList<MediaItem> {
        val n = size.coerceIn(1, PAGE_CAP)
        val from = (page.coerceAtLeast(0) * n).coerceAtMost(list.size)
        return ImmutableList.copyOf(list.subList(from, (from + n).coerceAtMost(list.size)))
    }

    // ---------- Callback ----------
    override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> {
        val extras = Bundle().apply {
            putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
            putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)   // lists, easiest to read while driving
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
        }
        return Futures.immediateFuture(LibraryResult.ofItem(folder(ROOT, "milo."), LibraryParams.Builder().setExtras(extras).build()))
    }

    override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String): ListenableFuture<LibraryResult<MediaItem>> = async {
        if (mediaId == ROOT) return@async LibraryResult.ofItem(folder(ROOT, "milo."), null)
        val t = mediaId.toLongOrNull()?.let { id -> all().firstOrNull { it.id == id } }
        if (t != null) LibraryResult.ofItem(song(t), null)
        else children(ROOT).firstOrNull { it.mediaId == mediaId }?.let { LibraryResult.ofItem(it, null) } ?: LibraryResult.ofError(SessionResult.RESULT_ERROR_BAD_VALUE)
    }

    override fun onGetChildren(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = async {
        lastParent = parentId
        LibraryResult.ofItemList(page(children(parentId), page, pageSize), params)
    }

    override fun onSearch(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> = async {
        session.notifySearchResultChanged(browser, query, search(query).size, params)
        LibraryResult.ofVoid()
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = async {
        lastParent = "search:$query"
        LibraryResult.ofItemList(page(search(query).map(::song), page, pageSize), params)
    }

    /**
     * The car sends back only the id of the tapped song (or a spoken request). Turn that into a real queue:
     * the folder it was tapped in, starting at that song. Items that already carry an address (this app's own player) pass straight through.
     */
    override fun onSetMediaItems(
        mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        if (mediaItems.isEmpty() || mediaItems.all { it.localConfiguration != null }) return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
        return async {
            val first = mediaItems[0]
            val q = first.requestMetadata.searchQuery
            val queue: List<Track>
            var at = 0
            if (mediaItems.size == 1 && !q.isNullOrBlank()) {
                // voice: "play <something> on milo."
                queue = search(q).ifEmpty { all().filter { it.id >= 0 }.shuffled().take(50) }
            } else if (mediaItems.size == 1 && first.mediaId.toLongOrNull() != null) {
                val ctxList = if (lastParent.startsWith("search:")) search(lastParent.removePrefix("search:")) else tracksOf(lastParent)
                val id = first.mediaId.toLong()
                val i = ctxList.indexOfFirst { it.id == id }
                if (i >= 0) { queue = ctxList; at = i } else { queue = byIds(listOf(id)) }
            } else if (mediaItems.size == 1 && first.mediaId != ROOT && (first.mediaId.contains(':') || first.mediaId == RECENT || first.mediaId == LIKED)) {
                // a whole folder (album, playlist, artist, liked...) was chosen to play
                queue = tracksOf(first.mediaId)
            } else {
                val ids = mediaItems.mapNotNull { it.mediaId.toLongOrNull() }
                queue = byIds(ids)
            }
            val out = queue.take(200).map(::song)
            MediaSession.MediaItemsWithStartPosition(out, if (out.isEmpty()) C_NONE else at.coerceIn(0, out.size - 1), startPositionMs)
        }
    }
}

private const val C_NONE = androidx.media3.common.C.INDEX_UNSET
