package com.naturia.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.naturia.app.model.Sound;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline cache: downloads the compressed CDN file into app-private storage. */
public class DownloadStore {

    public interface Listener {
        void onProgress(String soundId, int percent);
        void onDone(String soundId, File file);
        void onError(String soundId, String message);
    }

    private static DownloadStore instance;
    private final Context ctx;
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Integer> progress = new HashMap<>();
    private final Map<String, Listener> listeners = new HashMap<>();

    public static synchronized DownloadStore get(Context c) {
        if (instance == null) instance = new DownloadStore(c.getApplicationContext());
        return instance;
    }

    private DownloadStore(Context c) {
        this.ctx = c;
    }

    public File dir() {
        File d = new File(ctx.getFilesDir(), "audio");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public File fileFor(Sound s) {
        return new File(dir(), s.id + ".mp3");
    }

    public boolean isDownloaded(Sound s) {
        return fileFor(s).exists() && fileFor(s).length() > 10000;
    }

    public int progressOf(String id) {
        Integer p = progress.get(id);
        return p == null ? -1 : p;
    }

    public void addListener(String id, Listener l) {
        listeners.put(id, l);
    }

    public void removeListener(String id) {
        listeners.remove(id);
    }

    public void download(Sound s, String url) {
        if (isDownloaded(s)) {
            notifyDone(s.id);
            return;
        }
        progress.put(s.id, 0);
        pool.execute(() -> {
            HttpURLConnection conn = null;
            File tmp = new File(dir(), s.id + ".part");
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", "NATURIA/1.0");
                conn.connect();
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("HTTP " + code);
                int total = conn.getContentLength();
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[16 * 1024];
                    long read = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        read += n;
                        int pct = total > 0 ? (int) (read * 100 / total) : Math.min(95, (int) (read / 8000));
                        progress.put(s.id, pct);
                        notifyProgress(s.id, pct);
                    }
                }
                File dest = fileFor(s);
                if (dest.exists()) dest.delete();
                if (!tmp.renameTo(dest)) throw new Exception("cannot store file");
                AppDb.get(ctx).addDownload(s.id, dest.getAbsolutePath(), dest.length());
                progress.put(s.id, 100);
                notifyDone(s.id);
            } catch (Exception e) {
                progress.put(s.id, -1);
                if (tmp.exists()) tmp.delete();
                notifyError(s.id, e.getMessage() == null ? "download failed" : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void delete(Sound s) {
        File f = fileFor(s);
        if (f.exists()) f.delete();
        AppDb.get(ctx).removeDownload(s.id);
        progress.remove(s.id);
    }

    private void notifyProgress(String id, int pct) {
        Listener l = listeners.get(id);
        if (l != null) main.post(() -> l.onProgress(id, pct));
    }

    private void notifyDone(String id) {
        Listener l = listeners.get(id);
        if (l != null) main.post(() -> l.onDone(id, null));
    }

    private void notifyError(String id, String msg) {
        Listener l = listeners.get(id);
        if (l != null) main.post(() -> l.onError(id, msg));
    }
}
