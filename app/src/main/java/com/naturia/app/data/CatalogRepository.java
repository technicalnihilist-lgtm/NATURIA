package com.naturia.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.naturia.app.model.Sound;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Online catalogue. Starts from the copy bundled in assets so the first screen
 * paints instantly, then pulls the live catalogue from our CDN and swaps it in.
 * New recordings added to the CDN appear in the app without an app update.
 */
public class CatalogRepository {

    /** Our own storage / CDN. jsDelivr mirrors the repo on a global edge network. */
    public static final String CDN_BASE =
            "https://cdn.jsdelivr.net/gh/technicalnihilist-lgtm/NATURIA@main/";
    /** Direct GitHub raw — used for the catalogue and as the audio fallback. */
    public static final String CDN_MIRROR =
            "https://raw.githubusercontent.com/technicalnihilist-lgtm/NATURIA/main/";
    private static final String CATALOG_PATH = "catalog/catalog.json";
    private static final String CACHE_FILE = "catalog-live.json";
    private static final String TAG = "NaturiaCatalog";

    public interface OnCatalogChanged {
        /** @param added how many sounds appeared in this refresh (0 when unchanged) */
        void onCatalogChanged(int total, int added, boolean fromNetwork);
    }

    private static CatalogRepository instance;

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Sound> sounds = new ArrayList<>();
    private final Map<String, Sound> byId = new HashMap<>();
    private final List<OnCatalogChanged> listeners = new CopyOnWriteArrayList<>();

    private String generated = "";
    private volatile boolean refreshing = false;
    private volatile boolean offline = false;

    public static synchronized CatalogRepository get(Context c) {
        if (instance == null) instance = new CatalogRepository(c.getApplicationContext());
        return instance;
    }

    private CatalogRepository(Context ctx) {
        this.ctx = ctx;
        // 1. bundled seed — always available, even with no network
        parse(readAsset(), false);
        // 2. last live snapshot, if it is bigger/newer than the seed
        File cache = new File(ctx.getFilesDir(), CACHE_FILE);
        if (cache.exists()) {
            String json = readFile(cache);
            if (json != null && countOf(json) > sounds.size()) parse(json, false);
        }
    }

    /* ---------------- listeners ---------------- */

    public void addListener(OnCatalogChanged l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(OnCatalogChanged l) {
        listeners.remove(l);
    }

    /* ---------------- live refresh ---------------- */

    public boolean isOffline() {
        return offline;
    }

    public String generated() {
        return generated;
    }

    /** Pull the live catalogue from the CDN. Safe to call on every app start. */
    public void refresh() {
        if (refreshing) return;
        refreshing = true;
        new Thread(() -> {
            String json = null;
            try {
                json = fetch(CDN_MIRROR + CATALOG_PATH);
            } catch (Exception e) {
                Log.w(TAG, "catalogue refresh failed: " + e.getMessage());
            }
            final String payload = json;
            main.post(() -> {
                refreshing = false;
                if (payload == null) {
                    offline = true;
                    notifyListeners(sounds.size(), 0, false);
                    return;
                }
                offline = false;
                int before = sounds.size();
                parse(payload, true);
                writeCache(payload);
                notifyListeners(sounds.size(), Math.max(0, sounds.size() - before), true);
            });
        }).start();
    }

    private void notifyListeners(int total, int added, boolean network) {
        for (OnCatalogChanged l : listeners) l.onCatalogChanged(total, added, network);
    }

    private String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "NATURIA/1.1");
        c.setRequestProperty("Accept", "application/json");
        try {
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                return new String(in.readAllBytes(), "UTF-8");
            }
        } finally {
            c.disconnect();
        }
    }

    private synchronized void parse(String json, boolean replace) {
        if (json == null) return;
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("sounds");
            if (arr == null || arr.length() == 0) return;
            List<Sound> parsed = new ArrayList<>();
            Map<String, Sound> map = new HashMap<>();
            for (int i = 0; i < arr.length(); i++) {
                Sound s = Sound.from(arr.getJSONObject(i));
                if (s.id == null || s.id.isEmpty() || s.cdnPath == null) continue;
                parsed.add(s);
                map.put(s.id, s);
            }
            if (parsed.isEmpty()) return;
            if (!replace && !sounds.isEmpty()) return;
            sounds.clear();
            sounds.addAll(parsed);
            byId.clear();
            byId.putAll(map);
            generated = root.optString("generated", "");
        } catch (Exception e) {
            Log.w(TAG, "bad catalogue payload: " + e.getMessage());
        }
    }

    private int countOf(String json) {
        try {
            return new JSONObject(json).optJSONArray("sounds").length();
        } catch (Exception e) {
            return 0;
        }
    }

    private String readAsset() {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open("catalog.json"), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String readFile(File f) {
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private void writeCache(String json) {
        try (FileOutputStream out = new FileOutputStream(new File(ctx.getFilesDir(), CACHE_FILE))) {
            out.write(json.getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }

    /* ---------------- queries ---------------- */

    public List<Sound> all() {
        return Collections.unmodifiableList(sounds);
    }

    public Sound byId(String id) {
        return byId.get(id);
    }

    /** Every sound streams straight from the CDN — nothing is stored on the device. */
    public String audioUrl(Sound s) {
        return CDN_BASE + s.cdnPath;
    }

    /** Second source, tried once if the primary CDN fails. */
    public String audioUrlMirror(Sound s) {
        return CDN_MIRROR + s.cdnPath;
    }

    public List<Sound> search(String q) {
        if (q == null || q.trim().isEmpty()) return all();
        String needle = q.trim().toLowerCase(Locale.ROOT);
        List<Sound> out = new ArrayList<>();
        for (Sound s : sounds) {
            String hay = (s.name + " " + s.group + " " + s.letter + " " + s.tags + " " + s.description)
                    .toLowerCase(Locale.ROOT);
            if (hay.contains(needle)) out.add(s);
        }
        return out;
    }

    public List<Sound> byLetter(String letter) {
        List<Sound> out = new ArrayList<>();
        for (Sound s : sounds) if (letter.equalsIgnoreCase(s.letter)) out.add(s);
        return out;
    }

    /** Deterministic pick of the day so every device shows the same daily sound. */
    public Sound daily() {
        if (sounds.isEmpty()) return null;
        int day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) * 366;
        return sounds.get(Math.abs(day) % sounds.size());
    }

    /** Newest recordings first — this is what makes a live catalogue visible. */
    public List<Sound> newest(int limit) {
        List<Sound> copy = new ArrayList<>(sounds);
        Collections.sort(copy, new Comparator<Sound>() {
            @Override
            public int compare(Sound a, Sound b) {
                int c = b.added.compareTo(a.added);
                return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
            }
        });
        return copy.size() > limit ? copy.subList(0, limit) : copy;
    }

    public List<Sound> featured() {
        List<Sound> out = new ArrayList<>();
        String[] ids = {"gentle-rain", "ocean-waves", "forest-ambience", "campfire",
                "birds-chirping", "thunderstorm", "roaring-waterfall", "night-crickets"};
        for (String id : ids) if (byId.containsKey(id)) out.add(byId.get(id));
        for (Sound s : sounds) {
            if (out.size() >= 10) break;
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }

    public List<Sound> popular() {
        List<Sound> out = new ArrayList<>();
        String[] ids = {"heavy-rain", "sea-waves", "morning-birds", "crickets", "wind-in-trees",
                "stream", "rainforest", "fire", "night-forest", "monsoon-rain"};
        for (String id : ids) if (byId.containsKey(id)) out.add(byId.get(id));
        for (Sound s : sounds) {
            if (out.size() >= 10) break;
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }

    /** A–Z index with the number of available recordings per letter. */
    public Map<String, Integer> letterIndex() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (char ch = 'A'; ch <= 'Z'; ch++) map.put(String.valueOf(ch), 0);
        for (Sound s : sounds) {
            if (s.letter != null && map.containsKey(s.letter)) {
                map.put(s.letter, map.get(s.letter) + 1);
            }
        }
        return map;
    }

    public List<String> groups() {
        List<String> out = new ArrayList<>();
        for (Sound s : sounds) if (!out.contains(s.group)) out.add(s.group);
        Collections.sort(out);
        return out;
    }
}
