package me.Padej_.soupapi.bedrock;

/**
 * One frame of animation: a position, rotation and scale per bone.
 *
 * <p>Flat arrays indexed by bone, not a map, because a pose is rebuilt every frame for every bone
 * of a model that can carry nine hundred cubes — and because layering one clip over another is then
 * just writing over the slots the upper clip touches.
 */
public final class GeoPose {

    private final float[] values;
    private final boolean[] touched;
    private final int bones;
    private final float[] scratch = new float[3];

    public GeoPose(int bones) {
        this.bones = bones;
        this.values = new float[bones * 9];
        this.touched = new boolean[bones];
        reset();
    }

    public void reset() {
        for (int i = 0; i < bones; i++) {
            int o = i * 9;
            values[o] = values[o + 1] = values[o + 2] = 0f;
            values[o + 3] = values[o + 4] = values[o + 5] = 0f;
            values[o + 6] = values[o + 7] = values[o + 8] = 1f;
            touched[i] = false;
        }
    }

    public boolean touched(int bone) {
        return touched[bone];
    }

    public float posX(int bone) {
        return values[bone * 9];
    }

    public float posY(int bone) {
        return values[bone * 9 + 1];
    }

    public float posZ(int bone) {
        return values[bone * 9 + 2];
    }

    public float rotX(int bone) {
        return values[bone * 9 + 3];
    }

    public float rotY(int bone) {
        return values[bone * 9 + 4];
    }

    public float rotZ(int bone) {
        return values[bone * 9 + 5];
    }

    public float scaleX(int bone) {
        return safeScale(values[bone * 9 + 6]);
    }

    public float scaleY(int bone) {
        return safeScale(values[bone * 9 + 7]);
    }

    public float scaleZ(int bone) {
        return safeScale(values[bone * 9 + 8]);
    }

    /**
     * A scale a matrix can survive.
     *
     * <p>Clips hide a part by scaling it to nearly nothing, and a smooth channel through such a key
     * does not stop there: a Catmull-Rom curve between a near-zero key and a full-size one
     * undershoots below zero on the way, and blending two clips can land on zero exactly. Either
     * gives a bone a matrix with no inverse, and the NaNs that come out of one spread through every
     * vertex drawn from that bone. On a desktop driver that is invisible geometry; on the GL
     * translation layer PojavLauncher runs on, NaN vertices take the process down.
     *
     * <p>Clamping here rather than in the clips covers procedural scaling and clip blending too,
     * and 0.02 of a part is as invisible as 0.
     */
    private static float safeScale(float v) {
        if (Float.isNaN(v)) return 1f;
        float a = Math.abs(v);
        if (a < MIN_SCALE) return v < 0f ? -MIN_SCALE : MIN_SCALE;
        if (a > MAX_SCALE) return v < 0f ? -MAX_SCALE : MAX_SCALE;
        return v;
    }

    private static final float MIN_SCALE = 0.02f;
    private static final float MAX_SCALE = 64f;

    /** Copies another pose of the same model into this one, for a trailing ghost copy. */
    public void copyFrom(GeoPose other) {
        if (other == null || other.bones != bones) return;
        System.arraycopy(other.values, 0, values, 0, values.length);
        System.arraycopy(other.touched, 0, touched, 0, touched.length);
    }

    /** Adds a procedural offset on top of whatever the clips wrote: position and rotation add. */
    public void add(int bone, float px, float py, float pz, float rx, float ry, float rz) {
        if (bone < 0 || bone >= bones) return;
        int o = bone * 9;
        values[o] += px;
        values[o + 1] += py;
        values[o + 2] += pz;
        values[o + 3] += rx;
        values[o + 4] += ry;
        values[o + 5] += rz;
    }

    /** Multiplies a bone's scale, for procedural pulses. */
    public void scaleBy(int bone, float sx, float sy, float sz) {
        if (bone < 0 || bone >= bones) return;
        int o = bone * 9;
        values[o + 6] *= sx;
        values[o + 7] *= sy;
        values[o + 8] *= sz;
    }

    /**
     * Samples a clip that is fading out.
     *
     * <p>A bone the incoming clip also animates takes the outgoing clip at full weight, so the
     * incoming one can blend over it from there. A bone only the outgoing clip touched fades by
     * {@code 1 - blend} back toward whatever is under it. Sampling the outgoing clip at full weight
     * everywhere, as before, held its last frame through the fade and then snapped.
     */
    public void sampleFading(GeoModel model, GeoClip outgoing, float time, GeoClip incoming, float blend) {
        if (outgoing == null) return;
        for (GeoClip.BoneTrack track : outgoing.tracks) {
            GeoBone bone = model.bone(track.bone);
            if (bone == null) continue;
            boolean shared = incoming != null && incoming.animates(track.bone);
            float w = shared ? 1f : 1f - blend;
            if (w <= 0f) continue;
            int o = bone.index * 9;
            if (track.position != null) {
                track.position.sample(time, scratch);
                blend(o, scratch, w);
            }
            if (track.rotation != null) {
                track.rotation.sample(time, scratch);
                blend(o + 3, scratch, w);
            }
            if (track.scale != null) {
                track.scale.sample(time, scratch);
                blend(o + 6, scratch, w);
            }
            touched[bone.index] = true;
        }
    }

    public int bones() {
        return bones;
    }

    /**
     * Samples a clip into this pose.
     *
     * @param weight 0 leaves the pose alone, 1 replaces it, between the two blends — which is what
     *               makes a clip fade in over the one under it instead of snapping.
     */
    public void sample(GeoModel model, GeoClip clip, float time, float weight) {
        if (clip == null || weight <= 0f) return;
        for (GeoClip.BoneTrack track : clip.tracks) {
            GeoBone bone = model.bone(track.bone);
            if (bone == null) continue;
            int o = bone.index * 9;
            if (track.position != null) {
                track.position.sample(time, scratch);
                blend(o, scratch, weight);
            }
            if (track.rotation != null) {
                track.rotation.sample(time, scratch);
                blend(o + 3, scratch, weight);
            }
            if (track.scale != null) {
                track.scale.sample(time, scratch);
                blend(o + 6, scratch, weight);
            }
            touched[bone.index] = true;
        }
    }

    private void blend(int offset, float[] value, float weight) {
        if (weight >= 1f) {
            values[offset] = value[0];
            values[offset + 1] = value[1];
            values[offset + 2] = value[2];
            return;
        }
        values[offset] += (value[0] - values[offset]) * weight;
        values[offset + 1] += (value[1] - values[offset + 1]) * weight;
        values[offset + 2] += (value[2] - values[offset + 2]) * weight;
    }
}
