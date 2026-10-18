package com.ubayanda.milo;

import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * milo.'s background audio service. It owns the ExoPlayer and the media session, so playback keeps going with
 * the screen off or the app in the background, and the lock screen / notification / headset buttons work.
 * Next and previous are not decided here: they are forwarded to the app's queue (see MiloAudioPlugin).
 */
public class MiloPlayService extends MediaSessionService {
    public interface Skip { void on(String dir); }
    static volatile Skip skip;

    private MediaSession session;

    @Override
    public void onCreate() {
        super.onCreate();
        ExoPlayer player = new ExoPlayer.Builder(this)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(), true)          // pauses for calls, resumes when they end
                .setHandleAudioBecomingNoisy(true) // pauses when headphones are unplugged
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build();
        Player wrapped = new ForwardingPlayer(player) {
            private boolean skipCmd(int c) {
                return c == COMMAND_SEEK_TO_NEXT || c == COMMAND_SEEK_TO_PREVIOUS
                        || c == COMMAND_SEEK_TO_NEXT_MEDIA_ITEM || c == COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM;
            }
            @Override public Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon()
                        .add(COMMAND_SEEK_TO_NEXT).add(COMMAND_SEEK_TO_PREVIOUS)
                        .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build();
            }
            @Override public boolean isCommandAvailable(int command) {
                return skipCmd(command) || super.isCommandAvailable(command);
            }
            private void fire(String d) { Skip s = skip; if (s != null) s.on(d); }
            @Override public void seekToNext() { fire("next"); }
            @Override public void seekToNextMediaItem() { fire("next"); }
            @Override public void seekToPrevious() { fire("prev"); }
            @Override public void seekToPreviousMediaItem() { fire("prev"); }
        };
        session = new MediaSession.Builder(this, wrapped).build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return session;
    }

    /** Swiping the app away keeps music playing; if nothing is playing the service ends. */
    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        Player p = session != null ? session.getPlayer() : null;
        if (p == null || !p.getPlayWhenReady() || p.getMediaItemCount() == 0) stopSelf();
    }

    @Override
    public void onDestroy() {
        if (session != null) {
            session.getPlayer().release();
            session.release();
            session = null;
        }
        super.onDestroy();
    }
}
