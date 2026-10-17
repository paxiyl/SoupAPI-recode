package me.Padej_.soupapi.melee;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Padej_.soupapi.bedrock.GeoAnimations;
import me.Padej_.soupapi.bedrock.GeoClip;
import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * One weapon from a pack: its model, its animations, its textures and where it sits in the hand.
 *
 * <p>The heavy parts — a model of up to a thousand cubes and half a megabyte of keyframes — are
 * loaded on demand and dropped when the weapon is put away, so switching through a pack of eleven
 * does not hold eleven models and twenty-two textures in memory at once.
 */
public final class MeleeWeapon {

    /** How the first-person placement was worked out, so the settings screen can say so. */
    public static final class Fit {
        public final float scale;
        public final float offsetX;
        public final float offsetY;
        public final float offsetZ;
        /** The idle pose's size in blocks, for the picker's size column. */
        public final float span;

        Fit(float scale, float offsetX, float offsetY, float offsetZ, float span) {
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.span = span;
        }
    }

    public final MeleePack pack;
    public final String id;
    /** The pack's own display name, colour codes and all. */
    public final String displayName;

    private final String modelPath;
    private final String animationPath;
    private final String texturePath;
    private final String specularPath;
    private final String slotPath;

    private GeoModel model;
    private GeoAnimations animations;
    private GeoPose pose;
    private Fit fit;
    private boolean failed;

    /** Bones that are anchors rather than geometry; never used as a blade tip. */
    private static final String[] ANCHORS = {
            "camera", "view", "idle_view", "refit_view", "iron_view", "positioning",
            "ground", "thirdperson_hand", "fixed", "constraint", "root",
    };

    MeleeWeapon(MeleePack pack, String id, String displayName, JsonObject display) {
        this.pack = pack;
        this.id = id;
        this.displayName = displayName;
        this.modelPath = assetPath(display, "model", "geo_models", "_geo");
        this.animationPath = animationPath(display);
        this.texturePath = assetPath(display, "texture", "textures", "");
        this.specularPath = texturePath == null ? null
                : texturePath.substring(0, texturePath.length() - 4) + "_s.png";
        this.slotPath = assetPath(display, "slot_texture", "textures", "");
    }

    /** Turns {@code "delta_wt:melee/annihil_geo"} into {@code "assets/delta_wt/geo_models/melee/annihil_geo.json"}. */
    private String assetPath(JsonObject display, String key, String folder, String suffix) {
        if (display == null || !display.has(key)) return null;
        JsonElement element = display.get(key);
        if (!element.isJsonPrimitive()) return null;
        String value = element.getAsString();
        int colon = value.indexOf(':');
        String namespace = colon < 0 ? pack.namespace : value.substring(0, colon);
        String path = colon < 0 ? value : value.substring(colon + 1);
        String extension = "geo_models".equals(folder) ? ".json" : ".png";
        if (!path.endsWith(suffix) && !suffix.isEmpty()) path = path + suffix;
        return "assets/" + namespace + "/" + folder + "/" + path + extension;
    }

    private String animationPath(JsonObject display) {
        if (display == null || !display.has("animation")) return null;
        String value = display.get("animation").getAsString();
        int colon = value.indexOf(':');
        String namespace = colon < 0 ? pack.namespace : value.substring(0, colon);
        String path = colon < 0 ? value : value.substring(colon + 1);
        return "assets/" + namespace + "/animations/" + path + ".animation.json";
    }

    // ---- loading ---------------------------------------------------------------------------

    /** True once the model and animations are in memory. */
    public boolean loaded() {
        return model != null && animations != null;
    }

    public boolean failed() {
        return failed;
    }

    /** Reads the model and the animations. Safe off the render thread. */
    public synchronized boolean load() {
        if (loaded()) return true;
        if (failed) return false;
        try {
            JsonObject modelJson = pack.readJson(modelPath);
            JsonObject animationJson = pack.readJson(animationPath);
            if (modelJson == null || animationJson == null) {
                failed = true;
                return false;
            }
            GeoModel parsedModel = GeoModel.parse(modelJson);
            GeoAnimations parsedAnimations = GeoAnimations.parse(animationJson);
            GeoPose parsedPose = parsedModel.newPose();
            this.fit = measure(parsedModel, parsedAnimations, parsedPose);
            this.model = parsedModel;
            this.animations = parsedAnimations;
            this.pose = parsedPose;
            return true;
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.warn("[SoupAPI] could not load the melee weapon " + id, t);
            failed = true;
            return false;
        }
    }

    /**
     * Works out where the model sits in front of the eye.
     *
     * <p>A pack's models carry their own view anchors, and those anchors disagree between weapons —
     * in this one pack the same bone names sit sixty units apart — so taking one of them as the eye
     * puts half the weapons out of frame. The idle pose is measured instead and scaled to fill a
     * chosen share of the view at a chosen distance, which lands every weapon of every pack in the
     * same place. The settings then nudge it from there.
     */
    private Fit measure(GeoModel model, GeoAnimations animations, GeoPose pose) {
        GeoClip idle = animations.first("static_idle", "idle", "draw");
        pose.reset();
        if (idle != null) pose.sample(model, idle, 0f, 1f);
        float[] box = model.bounds(pose, null);
        if (box == null) return new Fit(1f, 0f, 0f, 0f, 0f);

        float spanX = (box[3] - box[0]) / 16f;
        float spanY = (box[4] - box[1]) / 16f;
        float span = Math.max(spanX, spanY);
        float scale = span > 1.0E-4f ? 1f / span : 1f;
        // The centre is mirrored in X with the model itself, so the offset is too.
        float centreX = -(box[0] + box[3]) / 32f * scale;
        float centreY = (box[1] + box[4]) / 32f * scale;
        float centreZ = (box[2] + box[5]) / 32f * scale;
        return new Fit(scale, -centreX, -centreY, -centreZ, span);
    }

    public GeoModel model() {
        return model;
    }

    public GeoAnimations animations() {
        return animations;
    }

    public GeoPose pose() {
        return pose;
    }

    public Fit fit() {
        return fit == null ? new Fit(1f, 0f, 0f, 0f, 0f) : fit;
    }

    /** Frees the model's display lists and textures; the JSON stays parsed for a fast re-draw. */
    public synchronized void unload() {
        if (model != null) model.release();
        pack.releaseTexture(texturePath);
        pack.releaseTexture(specularPath);
        model = null;
        animations = null;
        pose = null;
    }

    // ---- textures --------------------------------------------------------------------------

    public ResourceLocation texture() {
        return pack.texture(texturePath);
    }

    /**
     * The pack's specular map, used here as the glow mask.
     *
     * <p>It is a gun-pack convention for metal and emissive detail, which makes it exactly the mask
     * an additive pass wants: the parts the artist marked as shiny are the parts that light up.
     */
    public ResourceLocation glowMask() {
        return pack.texture(specularPath);
    }

    public ResourceLocation slotIcon() {
        return pack.texture(slotPath);
    }

    // ---- animation names ---------------------------------------------------------------------

    public GeoClip idle() {
        return animations == null ? null : animations.first("static_idle", "idle");
    }

    public GeoClip draw() {
        return animations == null ? null : animations.first("draw", "draw_1", "deploy");
    }

    public GeoClip inspect() {
        return animations == null ? null : animations.first("inspect", "inspect_1");
    }

    public GeoClip run() {
        return animations == null ? null : animations.get("run");
    }

    public GeoClip runStart() {
        return animations == null ? null : animations.get("run_start");
    }

    public GeoClip runEnd() {
        return animations == null ? null : animations.get("run_end");
    }

    /** The attack clips, in the order the pack means them to cycle. */
    public List<GeoClip> attacks() {
        List<GeoClip> out = new ArrayList<GeoClip>();
        if (animations == null) return out;
        for (int i = 1; i <= 6; i++) {
            GeoClip clip = animations.get("melee_" + i);
            if (clip != null) out.add(clip);
        }
        if (out.isEmpty()) {
            GeoClip clip = animations.first("attack", "melee");
            if (clip != null) out.add(clip);
        }
        return out;
    }

    /** The bone an effect should hang off: the farthest mesh bone from the hands. */
    public String bladeBone() {
        if (model == null) return null;
        String best = null;
        float bestDistance = -1f;
        float[] out = new float[3];
        for (me.Padej_.soupapi.bedrock.GeoBone bone : model.bones()) {
            if (!bone.hasGeometry() || isAnchor(bone.name) || bone.name.contains("hand")) continue;
            if (!model.pivotOf(bone.name, pose, out)) continue;
            float distance = out[0] * out[0] + out[1] * out[1] + out[2] * out[2];
            if (distance > bestDistance) {
                bestDistance = distance;
                best = bone.name;
            }
        }
        return best;
    }

    private static boolean isAnchor(String name) {
        for (String anchor : ANCHORS) {
            if (anchor.equals(name)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}
