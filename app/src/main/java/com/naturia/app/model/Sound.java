package com.naturia.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One nature recording from the verified catalogue. */
public class Sound {
    public String id, name, letter, group, provider, providerTitle, sourcePage,
            originalFile, creator, license, licenseCode, licenseUrl, cdnPath, description;
    public double duration;
    public long size;
    public boolean attributionRequired;
    public List<String> tags = new ArrayList<>();

    public static Sound from(JSONObject o) {
        Sound s = new Sound();
        s.id = o.optString("id");
        s.name = o.optString("name");
        s.letter = o.optString("letter");
        s.group = o.optString("group", "Ambience");
        s.provider = o.optString("provider");
        s.providerTitle = o.optString("provider_title");
        s.sourcePage = o.optString("source_page");
        s.originalFile = o.optString("original_file");
        s.creator = o.optString("creator", "Unknown");
        s.license = o.optString("license");
        s.licenseCode = o.optString("license_code");
        s.licenseUrl = o.optString("license_url");
        s.cdnPath = o.optString("cdn_path");
        s.description = o.optString("description");
        s.duration = o.optDouble("duration", 60);
        s.size = o.optLong("size");
        s.attributionRequired = o.optBoolean("attribution_required", true);
        JSONArray t = o.optJSONArray("tags");
        if (t != null) for (int i = 0; i < t.length(); i++) s.tags.add(t.optString(i));
        return s;
    }

    public String durationLabel() {
        int total = (int) Math.round(duration);
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }

    public String subtitle() {
        return group + " · " + letter + " · " + durationLabel();
    }
}
