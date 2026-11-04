package com.naturia.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.naturia.app.model.Mix;
import com.naturia.app.model.MixLayer;

import java.util.ArrayList;
import java.util.List;

/** Local store for favourites, play history and saved mixes. Audio itself is streamed. */
public class AppDb extends SQLiteOpenHelper {

    private static final String NAME = "naturia.db";
    private static final int VERSION = 1;
    private static AppDb instance;

    public static synchronized AppDb get(Context c) {
        if (instance == null) instance = new AppDb(c.getApplicationContext());
        return instance;
    }

    private AppDb(Context c) {
        super(c, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE favorites(sound_id TEXT PRIMARY KEY, ts INTEGER)");
        db.execSQL("CREATE TABLE recent(sound_id TEXT PRIMARY KEY, ts INTEGER)");
        db.execSQL("CREATE TABLE mixes(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, ts INTEGER)");
        db.execSQL("CREATE TABLE mix_layers(mix_id INTEGER, sound_id TEXT, volume REAL, pos INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) { }

    /* ---------------- favourites ---------------- */

    public boolean isFavorite(String id) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT 1 FROM favorites WHERE sound_id=?", new String[]{id})) {
            return c.moveToFirst();
        }
    }

    public void toggleFavorite(String id) {
        if (isFavorite(id)) {
            getWritableDatabase().delete("favorites", "sound_id=?", new String[]{id});
        } else {
            ContentValues v = new ContentValues();
            v.put("sound_id", id);
            v.put("ts", System.currentTimeMillis());
            getWritableDatabase().insertWithOnConflict("favorites", null, v,
                    SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public List<String> favorites() {
        return ids("SELECT sound_id FROM favorites ORDER BY ts DESC");
    }

    /* ---------------- history ---------------- */

    public void markPlayed(String id) {
        ContentValues v = new ContentValues();
        v.put("sound_id", id);
        v.put("ts", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("recent", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        // keep the history bounded
        getWritableDatabase().execSQL(
                "DELETE FROM recent WHERE sound_id NOT IN (SELECT sound_id FROM recent ORDER BY ts DESC LIMIT 60)");
    }

    public List<String> recent() {
        return ids("SELECT sound_id FROM recent ORDER BY ts DESC");
    }

    public void clearHistory() {
        getWritableDatabase().delete("recent", null, null);
    }

    /* ---------------- mixes ---------------- */

    public long saveMix(Mix mix) {
        ContentValues v = new ContentValues();
        v.put("name", mix.name);
        v.put("ts", System.currentTimeMillis());
        long id = getWritableDatabase().insert("mixes", null, v);
        for (int i = 0; i < mix.layers.size(); i++) {
            MixLayer l = mix.layers.get(i);
            ContentValues lv = new ContentValues();
            lv.put("mix_id", id);
            lv.put("sound_id", l.soundId);
            lv.put("volume", l.volume);
            lv.put("pos", i);
            getWritableDatabase().insert("mix_layers", null, lv);
        }
        return id;
    }

    public void deleteMix(long id) {
        getWritableDatabase().delete("mixes", "id=?", new String[]{String.valueOf(id)});
        getWritableDatabase().delete("mix_layers", "mix_id=?", new String[]{String.valueOf(id)});
    }

    public List<Mix> mixes() {
        List<Mix> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,name,ts FROM mixes ORDER BY ts DESC", null)) {
            while (c.moveToNext()) {
                Mix m = new Mix(c.getString(1));
                m.id = c.getLong(0);
                m.createdAt = c.getLong(2);
                try (Cursor lc = getReadableDatabase().rawQuery(
                        "SELECT sound_id,volume FROM mix_layers WHERE mix_id=? ORDER BY pos",
                        new String[]{String.valueOf(m.id)})) {
                    while (lc.moveToNext()) m.layers.add(new MixLayer(lc.getString(0), lc.getFloat(1)));
                }
                out.add(m);
            }
        }
        return out;
    }

    private List<String> ids(String sql) {
        List<String> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }
}
