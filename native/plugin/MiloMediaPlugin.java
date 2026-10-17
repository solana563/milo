package com.ubayanda.milo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.File;
import java.io.FileOutputStream;

/** Reads the phone's music library (MediaStore) so milo. can play files in place, like Poweramp. */
@CapacitorPlugin(
    name = "MiloMedia",
    permissions = {
        @Permission(alias = "audio", strings = { Manifest.permission.READ_MEDIA_AUDIO }),
        @Permission(alias = "storage", strings = { Manifest.permission.READ_EXTERNAL_STORAGE })
    }
)
public class MiloMediaPlugin extends Plugin {

    private String alias() {
        return Build.VERSION.SDK_INT >= 33 ? "audio" : "storage";
    }

    private boolean granted() {
        return getPermissionState(alias()) == PermissionState.GRANTED;
    }

    private Uri collection() {
        return Build.VERSION.SDK_INT >= 29
            ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    @PluginMethod
    public void check(PluginCall call) {
        JSObject o = new JSObject();
        o.put("granted", granted());
        call.resolve(o);
    }

    @PluginMethod
    public void ask(PluginCall call) {
        if (granted()) {
            check(call);
        } else {
            requestPermissionForAlias(alias(), call, "askDone");
        }
    }

    @PermissionCallback
    private void askDone(PluginCall call) {
        check(call);
    }

    @PluginMethod
    public void scan(final PluginCall call) {
        if (!granted()) {
            call.reject("permission");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSArray out = new JSArray();
                    String[] proj = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.TRACK,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.DATA
                    };
                    String sel = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= 20000";
                    ContentResolver cr = getContext().getContentResolver();
                    Uri base = collection();
                    Cursor c = cr.query(base, proj, sel, null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC");
                    if (c != null) {
                        try {
                            int iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                            int iT = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                            int iA = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                            int iAl = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                            int iAi = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                            int iD = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                            int iY = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);
                            int iTr = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);
                            int iN = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                            int iAd = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                            int iP = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                            while (c.moveToNext()) {
                                long id = c.getLong(iId);
                                JSObject o = new JSObject();
                                o.put("id", id);
                                o.put("t", c.getString(iT));
                                String ar = c.getString(iA);
                                o.put("a", (ar == null || ar.equals("<unknown>")) ? "" : ar);
                                String al = c.getString(iAl);
                                o.put("al", (al == null || al.equals("<unknown>")) ? "" : al);
                                o.put("aid", c.getLong(iAi));
                                o.put("d", c.getLong(iD) / 1000.0);
                                o.put("y", c.getInt(iY));
                                o.put("n", c.getInt(iTr) % 1000);
                                o.put("f", c.getString(iN));
                                o.put("add", c.getLong(iAd));
                                o.put("p", c.getString(iP));
                                o.put("u", ContentUris.withAppendedId(base, id).toString());
                                out.put(o);
                            }
                        } finally {
                            c.close();
                        }
                    }
                    JSObject res = new JSObject();
                    res.put("tracks", out);
                    call.resolve(res);
                } catch (Exception e) {
                    call.reject("scan: " + e.getMessage());
                }
            }
        }).start();
    }

    /** Saves the cover art for one song to the app cache and returns its path ({} when it has none). */
    @PluginMethod
    public void art(final PluginCall call) {
        final long id = call.getLong("id", 0L);
        final long aid = call.getLong("aid", 0L);
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSObject res = new JSObject();
                try {
                    File dir = new File(getContext().getCacheDir(), "art");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, (aid > 0 ? "a" + aid : "t" + id) + ".jpg");
                    if (f.exists() && f.length() > 0) {
                        res.put("path", f.getAbsolutePath());
                        call.resolve(res);
                        return;
                    }
                    Uri item = ContentUris.withAppendedId(collection(), id);
                    Bitmap bm = null;
                    if (Build.VERSION.SDK_INT >= 29) {
                        try {
                            bm = getContext().getContentResolver().loadThumbnail(item, new Size(640, 640), null);
                        } catch (Exception e) {
                            bm = null;
                        }
                    }
                    if (bm == null) {
                        MediaMetadataRetriever r = new MediaMetadataRetriever();
                        try {
                            r.setDataSource(getContext(), item);
                            byte[] b = r.getEmbeddedPicture();
                            if (b != null) bm = BitmapFactory.decodeByteArray(b, 0, b.length);
                        } catch (Exception e) {
                            bm = null;
                        } finally {
                            try { r.release(); } catch (Exception e) { }
                        }
                    }
                    if (bm != null) {
                        FileOutputStream fo = new FileOutputStream(f);
                        try {
                            bm.compress(Bitmap.CompressFormat.JPEG, 88, fo);
                        } finally {
                            fo.close();
                        }
                        res.put("path", f.getAbsolutePath());
                    }
                    call.resolve(res);
                } catch (Exception e) {
                    call.resolve(res);
                }
            }
        }).start();
    }
}
