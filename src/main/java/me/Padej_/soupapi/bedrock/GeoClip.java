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

    /** A sound the clip asks for, and when. */
    public static final class SoundCue {
        public final float time;
        /** The pack's own reference, such as {@code delta_wt:melee/mercy/draw}. */
        public final String effect;

        SoundCue(float time, String effect) {
            this.time = time;
            this.effect = effect;
        }
    }

    public final String name;
    public final float length;
    public final boolean loop;
    public final List<BoneTrack> tracks;
    public final List<SoundCue> sounds;
    private final Map<String, BoneTrack> byBone;

    private GeoClip(String name, float length, boolean loop, List<BoneTrack> tracks,
                    List<SoundCue> sounds) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.tracks = tracks;
        this.sounds = sounds;
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

        return new GeoClip(name, length, loop, tracks, sounds(root));
    }

    /**
     * The clip's sound cues.
     *
     * <p>Bedrock writes these as a timeline of {@code {"effect": "<namespace>:<path>"}}, which in a
     * gun pack points straight at an ogg file. That is the whole reason the pack's sounds are worth
     * bundling: with the files inside the mod the game's own sound engine can play them, so a swing
     * sounds like the pack meant it to rather than like nothing at all.
     */
    private static List<SoundCue> sounds(JsonObject root) {
        List<SoundCue> out = new ArrayList<SoundCue>();
        if (!root.has("sound_effects") || !root.get("sound_effects").isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("sound_effects").entrySet()) {
            float time;
            try {
                time = Float.parseFloat(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            JsonElement value = entry.getValue();
            if (value.isJsonObject() && value.getAsJsonObject().has("effect")) {
                out.add(new SoundCue(time, value.getAsJsonObject().get("effect").getAsString()));
            } else if (value.isJsonArray()) {
                for (JsonElement each : value.getAsJsonArray()) {
                    if (each.isJsonObject() && each.getAsJsonObject().has("effect")) {
                        out.add(new SoundCue(time,
                                each.getAsJsonObject().get("effect").getAsString()));
                    }
                }
            }
        }
        java.util.Collections.sort(out, new java.util.Comparator<SoundCue>() {
            @Override
            public int compare(SoundCue a, SoundCue b) {
                return Float.compare(a.time, b.time);
            }
        });
        return out;
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
