package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * One animated property of one bone — its position, rotation or scale over time.
 *
 * <p>Bedrock writes a channel as a map of time to value, and a keyframe can be a bare vector, a
 * single number (used for a uniform scale), or an object carrying {@code pre}/{@code post} values
 * for a hard step and a {@code lerp_mode}. All four forms appear in the packs this reads, so all
 * four are handled here rather than at the call site.
 */
public final class GeoChannel {

    private final float[] times;
    private final float[][] pre;
    private final float[][] post;
    private final boolean[] smooth;
    private final float[] fallback;

    private GeoChannel(float[] times, float[][] pre, float[][] post, boolean[] smooth, float[] fallback) {
        this.times = times;
        this.pre = pre;
        this.post = post;
        this.smooth = smooth;
        this.fallback = fallback;
    }

    /** Builds a channel from whatever shape the JSON holds, or {@code null} when it holds nothing. */
    public static GeoChannel parse(JsonElement element, float[] fallback) {
        if (element == null || element.isJsonNull()) return null;

        if (element.isJsonArray() || element.isJsonPrimitive()) {
            // A constant: one value for the whole clip.
            float[] value = vector(element, fallback);
            return new GeoChannel(new float[]{0f}, new float[][]{value}, new float[][]{value},
                    new boolean[]{false}, fallback);
        }
        if (!element.isJsonObject()) return null;

        JsonObject object = element.getAsJsonObject();
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            keys.add(entry.getKey());
        }
        if (keys.isEmpty()) return null;
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Float.compare(time(a), time(b));
            }
        });

        float[] times = new float[keys.size()];
        float[][] pre = new float[keys.size()][];
        float[][] post = new float[keys.size()][];
        boolean[] smooth = new boolean[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            JsonElement frame = object.get(key);
            times[i] = time(key);
            if (frame.isJsonObject()) {
                JsonObject f = frame.getAsJsonObject();
                float[] after = vector(f.get("post"), fallback);
                float[] before = f.has("pre") ? vector(f.get("pre"), fallback) : after;
                pre[i] = before;
                post[i] = after;
                smooth[i] = f.has("lerp_mode")
                        && "catmullrom".equals(f.get("lerp_mode").getAsString());
            } else {
                float[] value = vector(frame, fallback);
                pre[i] = value;
                post[i] = value;
            }
        }
        return new GeoChannel(times, pre, post, smooth, fallback);
    }

    private static float time(String key) {
        try {
            return Float.parseFloat(key);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * Reads a keyframe value.
     *
     * <p>A number means all three components (a uniform scale); a short array is padded from the
     * fallback; a string component would be a Molang expression, which no pack here uses and which
     * reads as the fallback rather than failing the load.
     */
    private static float[] vector(JsonElement element, float[] fallback) {
        float[] out = new float[]{fallback[0], fallback[1], fallback[2]};
        if (element == null || element.isJsonNull()) return out;
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                float v = primitive.getAsFloat();
                return new float[]{v, v, v};
            }
            return out;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("post")) return vector(object.get("post"), fallback);
            if (object.has("pre")) return vector(object.get("pre"), fallback);
            return out;
        }
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < 3 && i < array.size(); i++) {
            JsonElement component = array.get(i);
            if (component.isJsonPrimitive() && component.getAsJsonPrimitive().isNumber()) {
                out[i] = component.getAsFloat();
            }
        }
        return out;
    }

    /** The last keyframe's time, which is how long this channel actually runs. */
    public float length() {
        return times.length == 0 ? 0f : times[times.length - 1];
    }

    /** Samples the channel, writing into {@code out}. */
    public void sample(float time, float[] out) {
        if (times.length == 0) {
            System.arraycopy(fallback, 0, out, 0, 3);
            return;
        }
        if (time <= times[0]) {
            System.arraycopy(post[0], 0, out, 0, 3);
            return;
        }
        int last = times.length - 1;
        if (time >= times[last]) {
            System.arraycopy(pre[last], 0, out, 0, 3);
            return;
        }

        int i = 0;
        while (i + 1 < times.length && times[i + 1] <= time) i++;
        float t0 = times[i];
        float t1 = times[i + 1];
        float progress = t1 <= t0 ? 0f : (time - t0) / (t1 - t0);
        float[] a = post[i];
        float[] b = pre[i + 1];

        if (smooth[i] || smooth[i + 1]) {
            float[] before = i > 0 ? post[i - 1] : a;
            float[] after = i + 2 < times.length ? pre[i + 2] : b;
            for (int k = 0; k < 3; k++) {
                out[k] = catmullRom(before[k], a[k], b[k], after[k], progress);
            }
            return;
        }
        for (int k = 0; k < 3; k++) {
            out[k] = a[k] + (b[k] - a[k]) * progress;
        }
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }
}
