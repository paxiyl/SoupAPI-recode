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
        return values[bone * 9 + 6];
    }

    public float scaleY(int bone) {
        return values[bone * 9 + 7];
    }

    public float scaleZ(int bone) {
        return values[bone * 9 + 8];
    }

    /** Copies another pose of the same model into this one, for a trailing ghost copy. */
    public void copyFrom(GeoPose other) {
        if (other == null || other.bones != bones) return;
        System.arraycopy(other.values, 0, values, 0, values.length);
        System.arraycopy(other.touched, 0, touched, 0, touched.length);
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
