package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Every clip in one {@code .animation.json}. */
public final class GeoAnimations {

    private final Map<String, GeoClip> clips;

    private GeoAnimations(Map<String, GeoClip> clips) {
        this.clips = clips;
    }

    public static GeoAnimations parse(JsonObject root) {
        Map<String, GeoClip> clips = new LinkedHashMap<String, GeoClip>();
        if (root.has("animations") && root.get("animations").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("animations").entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                clips.put(entry.getKey(), GeoClip.parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
        }
        return new GeoAnimations(clips);
    }

    public GeoClip get(String name) {
        return clips.get(name);
    }

    /** The first clip that exists, by preference order; {@code null} when none do. */
    public GeoClip first(String... names) {
        for (String name : names) {
            GeoClip clip = clips.get(name);
            if (clip != null) return clip;
        }
        return null;
    }

    public boolean has(String name) {
        return clips.containsKey(name);
    }

    public Map<String, GeoClip> all() {
        return Collections.unmodifiableMap(clips);
    }

    public int size() {
        return clips.size();
    }
}
