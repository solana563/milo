package com.ubayanda.milo;

import android.content.ComponentName;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

/**
 * "MiloAudio": plays songs that are on the phone with Media3 / ExoPlayer instead of the web view's audio element.
 * The HTML player stays the interface; it sends commands here and gets "ne" events back
 * (state, end, advance, skip, err). Songs are opened straight from MediaStore, so nothing is copied.
 */
@CapacitorPlugin(name = "MiloAudio")
public class MiloAudioPlugin extends Plugin {
    private MediaController mc;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final List<Runnable> pending = new ArrayList<>();
    private boolean connecting;
    private final Runnable poller = new Runnable() {
        @Override public void run() {
            if (mc != null && mc.isPlaying()) { emitState(); h.postDelayed(this, 500); }
        }
    };

    @Override
    public void load() {
        MiloPlayService.skip = d -> h.post(() -> { JSObject o = new JSObject(); o.put("ev", "skip"); o.put("d", d); notifyListeners("ne", o); });
    }

    private void emit(JSObject o) { notifyListeners("ne", o); }

    private void emitState() {
        if (mc == null) return;
        JSObject o = new JSObject();
        long d = mc.getDuration();
        o.put("ev", "state");
        o.put("pw", mc.getPlayWhenReady());
        o.put("pl", mc.isPlaying());
        o.put("pos", mc.getCurrentPosition() / 1000.0);
        o.put("dur", d == C.TIME_UNSET ? 0 : d / 1000.0);
        o.put("st", mc.getPlaybackState());
        emit(o);
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override public void onPlayWhenReadyChanged(boolean pw, int reason) { emitState(); }
        @Override public void onIsPlayingChanged(boolean playing) { emitState(); if (playing) { h.removeCallbacks(poller); h.post(poller); } }
        @Override public void onPlaybackStateChanged(int st) {
            if (st == Player.STATE_ENDED) { JSObject o = new JSObject(); o.put("ev", "end"); emit(o); } else emitState();
        }
        @Override public void onMediaItemTransition(MediaItem item, int reason) {
            if (item != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                JSObject o = new JSObject(); o.put("ev", "advance"); o.put("k", item.mediaId); emit(o);
                if (mc != null && mc.getCurrentMediaItemIndex() > 0) mc.removeMediaItems(0, mc.getCurrentMediaItemIndex());
            }
        }
        @Override public void onPlayerError(@NonNull PlaybackException e) {
            JSObject o = new JSObject(); o.put("ev", "err"); o.put("msg", e.getErrorCodeName()); emit(o);
        }
    };

    /** Connects to the playback service (starting it). Resolves {ok:true} when the player is ready. */
    @PluginMethod
    public void ready(PluginCall call) {
        h.post(() -> {
            if (mc != null) { JSObject r = new JSObject(); r.put("ok", true); call.resolve(r); return; }
            if (connecting) { call.reject("busy"); return; }
            connecting = true;
            try {
                SessionToken token = new SessionToken(getContext(), new ComponentName(getContext(), MiloPlayService.class));
                final ListenableFuture<MediaController> f = new MediaController.Builder(getContext(), token).buildAsync();
                f.addListener(() -> {
                    connecting = false;
                    try {
                        mc = f.get();
                        mc.addListener(listener);
                        mc.setRepeatMode(Player.REPEAT_MODE_OFF);
                        for (Runnable r : pending) r.run();
                        pending.clear();
                        JSObject r = new JSObject(); r.put("ok", true); call.resolve(r);
                    } catch (Exception e) { call.reject("no player"); }
                }, ContextCompat.getMainExecutor(getContext()));
            } catch (Exception e) { connecting = false; call.reject("no service"); }
        });
    }

    private void run(Runnable r) { h.post(() -> { if (mc == null) pending.add(r); else r.run(); }); }

    private MediaItem item(JSObject o) {
        Uri u;
        long did = o.optLong("did", 0);
        if (did > 0) u = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, did);
        else u = Uri.parse(o.getString("uri", ""));
        MediaMetadata.Builder m = new MediaMetadata.Builder()
                .setTitle(o.getString("title", ""))
                .setArtist(o.getString("artist", ""))
                .setAlbumTitle(o.getString("album", ""));
        String art = o.getString("art", "");
        if (art != null && !art.isEmpty()) m.setArtworkUri(Uri.parse(art));
        return new MediaItem.Builder().setMediaId(o.getString("key", "")).setUri(u).setMediaMetadata(m.build()).build();
    }

    /** load({key,did,title,artist,album,art,pos(ms),play,rate,vol}) */
    @PluginMethod
    public void load(PluginCall call) {
        final JSObject o = call.getData();
        run(() -> {
            try {
                mc.setMediaItem(item(o), (long) o.optDouble("pos", 0));
                mc.prepare();
                mc.setPlaybackSpeed((float) o.optDouble("rate", 1));
                mc.setVolume((float) o.optDouble("vol", 1));
                mc.setPlayWhenReady(o.optBoolean("play", true));
                call.resolve();
            } catch (Exception e) { call.reject("load failed"); }
        });
    }

    /** next({...item}) queues the following song so it starts gaplessly; next({}) clears it. */
    @PluginMethod
    public void next(PluginCall call) {
        final JSObject o = call.getData();
        run(() -> {
            try {
                int ci = mc.getCurrentMediaItemIndex(), n = mc.getMediaItemCount();
                if (n > ci + 1) mc.removeMediaItems(ci + 1, n);
                if (o.has("did") || o.has("uri")) mc.addMediaItem(item(o));
                call.resolve();
            } catch (Exception e) { call.reject("next failed"); }
        });
    }

    @PluginMethod public void play(PluginCall call) { run(() -> { mc.setPlayWhenReady(true); if (mc.getPlaybackState() == Player.STATE_IDLE) mc.prepare(); call.resolve(); }); }
    @PluginMethod public void pause(PluginCall call) { run(() -> { mc.setPlayWhenReady(false); call.resolve(); }); }
    @PluginMethod public void seek(PluginCall call) { final double p = call.getDouble("pos", 0.0); run(() -> { mc.seekTo((long) (p * 1000)); call.resolve(); }); }
    @PluginMethod public void rate(PluginCall call) { final double v = call.getDouble("v", 1.0); run(() -> { mc.setPlaybackSpeed((float) v); call.resolve(); }); }
    @PluginMethod public void volume(PluginCall call) { final double v = call.getDouble("v", 1.0); run(() -> { mc.setVolume((float) Math.max(0, Math.min(1, v))); call.resolve(); }); }
    @PluginMethod public void stop(PluginCall call) { run(() -> { mc.setPlayWhenReady(false); mc.stop(); mc.clearMediaItems(); call.resolve(); }); }
    @PluginMethod public void state(PluginCall call) { run(() -> { emitState(); call.resolve(); }); }
}
