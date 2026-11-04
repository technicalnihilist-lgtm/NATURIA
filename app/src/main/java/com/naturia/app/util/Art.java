package com.naturia.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

/** Category artwork bundled with the app (CC-licensed nature photography). */
public class Art {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(14);

    public static String mood(String group) {
        String g = group == null ? "" : group.toLowerCase();
        switch (g) {
            case "forest": return "forest";
            case "wind": return "wind";
            case "water": return "water";
            case "birds": return "birds";
            case "wildlife": return "wildlife";
            case "fire": return "fire";
            case "ocean": return "ocean";
            case "rain": return "rain";
            case "storm": return "storm";
            case "night": return "night";
            case "insects": return "birds";
            case "frogs": return "water";
            case "ambience": return "ambience";
            default: return "forest";
        }
    }

    public static Bitmap get(Context c, String group) {
        String mood = mood(group);
        Bitmap b = CACHE.get(mood);
        if (b != null) return b;
        try (InputStream in = c.getAssets().open("art/" + mood + ".jpg")) {
            b = BitmapFactory.decodeStream(in);
            if (b != null) CACHE.put(mood, b);
            return b;
        } catch (IOException e) {
            return null;
        }
    }

    public static void load(Context c, ImageView iv, String group) {
        Bitmap b = get(c, group);
        if (b != null) iv.setImageBitmap(b);
    }

    public static String clock(long ms) {
        int total = (int) Math.max(0, ms / 1000);
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }
}
