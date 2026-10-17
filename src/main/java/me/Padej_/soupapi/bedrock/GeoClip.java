package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One animation: the channels for every bone it moves, and how long it runs. */
public final class GeoClip {

    /** The channels for one bone. Any of the three may be absent. */
    public static final class BoneTrack {
        public final String bone;
        public final GeoChannel position;
        public final GeoChannel rotation;
        public final GeoChannel scale;

        BoneTrack(String bone, GeoChannel position, GeoChannel rotation, GeoChannel scale) {
            this.bone = bone;
            this.position = position;
            this.rotation = rotation;
            this.scale = scale;
        }
    }

    private static final float[] ZERO = {0f, 0f, 0f};
    private static final float[] ONE = {1f, 1f, 1f};

    public final String name;
    public final float length;
    public final boolean loop;
    public final List<BoneTrack> tracks;
    private final Map<String, BoneTrack> byBone;

    private GeoClip(String name, float length, boolean loop, List<BoneTrack> tracks) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.tracks = tracks;
        this.byBone = new HashMap<String, BoneTrack>();
        for (BoneTrack track : tracks) {
            byBone.put(track.bone, track);
        }
    }

    public static GeoClip parse(String name, JsonObject root) {
        List<BoneTrack> tracks = new ArrayList<BoneTrack>();
        float longest = 0f;
        if (root.has("bones") && root.get("bones").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("bones").entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject channels = entry.getValue().getAsJsonObject();
                GeoChannel position = GeoChannel.parse(channels.get("position"), ZERO);
                GeoChannel rotation = GeoChannel.parse(channels.get("rotation"), ZERO);
                GeoChannel scale = GeoChannel.parse(channels.get("scale"), ONE);
                if (position == null && rotation == null && scale == null) continue;
                tracks.add(new BoneTrack(entry.getKey(), position, rotation, scale));
                longest = Math.max(longest, longest(position, rotation, scale));
            }
        }

        float length = 0f;
        if (root.has("animation_length") && !root.get("animation_length").isJsonNull()) {
            length = root.get("animation_length").getAsFloat();
        }
        // A clip with no declared length runs as long as its longest channel; a still pose with a
        // single keyframe then has length 0, which the player reads as "hold this frame".
        if (length <= 0f) length = longest;

        boolean loop = root.has("loop") && root.get("loop").isJsonPrimitive()
                && root.get("loop").getAsJsonPrimitive().isBoolean()
                && root.get("loop").getAsBoolean();

        return new GeoClip(name, length, loop, tracks);
    }

    private static float longest(GeoChannel... channels) {
        float out = 0f;
        for (GeoChannel channel : channels) {
            if (channel != null) out = Math.max(out, channel.length());
        }
        return out;
    }

    public BoneTrack track(String bone) {
        return byBone.get(bone);
    }

    /** True when this clip touches the given bone, which is what lets one clip layer over another. */
    public boolean animates(String bone) {
        return byBone.containsKey(bone);
    }
}
