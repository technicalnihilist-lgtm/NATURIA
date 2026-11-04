package com.naturia.app.data;

import android.content.Context;

import com.naturia.app.model.Sound;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads the verified A-Z catalogue bundled in assets and exposes discovery helpers. */
public class CatalogRepository {

    /** Our own storage / CDN. Swap this for any object store without touching the app. */
    public static final String CDN_BASE =
            "https://raw.githubusercontent.com/technicallnihilist-lgtm/NATURIA/main/";

    private static CatalogRepository instance;

    private final List<Sound> sounds = new ArrayList<>();
    private final Map<String, Sound> byId = new HashMap<>();

    public static synchronized CatalogRepository get(Context c) {
        if (instance == null) instance = new CatalogRepository(c.getApplicationContext());
        return instance;
    }

    private CatalogRepository(Context ctx) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                ctx.getAssets().open("catalog.json"), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.getJSONArray("sounds");
            for (int i = 0; i < arr.length(); i++) {
                Sound s = Sound.from(arr.getJSONObject(i));
                sounds.add(s);
                byId.put(s.id, s);
            }
        } catch (Exception ignored) {
        }
    }

    public List<Sound> all() {
        return Collections.unmodifiableList(sounds);
    }

    public Sound byId(String id) {
        return byId.get(id);
    }

    public String audioUrl(Sound s) {
        return CDN_BASE + s.cdnPath;
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
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_YEAR) + c.get(Calendar.YEAR) * 366;
        return sounds.get(Math.abs(day) % sounds.size());
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
