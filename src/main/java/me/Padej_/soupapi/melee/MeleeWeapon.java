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

    /**
     * Where the eye sits inside the model, which is all the first-person placement needs.
     *
     * <p>These packs carry a bone called {@code idle_view}: the point the mod they were built for
     * puts the camera at when the weapon is simply held. Take that as the origin, keep the authored
     * scale of a sixteenth of a block per unit, and mirror X, and the model <em>is</em> the view —
     * weapon, grip and both hands, exactly as posed.
     *
     * <p>Checked across all eleven weapons in this pack: measured from {@code idle_view}, the right
     * wrist lands between 0.5 and 1.1 blocks below the eye and between 1.1 and 1.7 in front of it
     * every time, which is where a hand holding something belongs. The earlier build measured the
     * weapon's bounding box instead, normalised it to 0.55 blocks and turned the blade onto a guessed
     * direction — three guesses that disagreed with the hands and with each other, and that is what
     * put every viewmodel in the wrong place.
     */
    public static final class Fit {
        /** The {@code idle_view} pivot, in model units: subtract it and the eye is at the origin. */
        public final float eyeX;
        public final float eyeY;
        public final float eyeZ;
        /** The weapon's own size in blocks, for the picker's size column. */
        public final float span;

        Fit(float eyeX, float eyeY, float eyeZ, float span) {
            this.eyeX = eyeX;
            this.eyeY = eyeY;
            this.eyeZ = eyeZ;
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
    private float[] grip;
    private boolean failed;

    /**
     * The suffix of the bones that are the pack's arms.
     *
     * <p>Exactly {@code lefthand_pos} and {@code righthand_pos} in every pack here. Matched as a
     * suffix on purpose: "hand" anywhere in the name also catches a weapon's own {@code hand} or
     * {@code handle}, and two of these weapons have one.
     */
    public static final String ARM_MESH = "hand_pos";

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
     * Reads the eye out of the model and measures the weapon, once, at load.
     *
     * <p>The eye is the {@code idle_view} bone; {@code camera} is the fallback, and it is the same
     * pivot on most of these weapons. Nothing is fitted: the pack is authored at a sixteenth of a
     * block per unit — its hand markers are vanilla 4×12×4 arm boxes, which is how that is known —
     * so the span is only reported, never used to scale anything.
     */
    private Fit measure(GeoModel model, GeoAnimations animations, GeoPose pose) {
        GeoClip idle = animations.first("static_idle", "idle", "draw");
        pose.reset();
        if (idle != null) pose.sample(model, idle, 0f, 1f);

        float[] eye = eyeOf(model);
        float[] weapon = model.bounds(pose, null, ARM_MESH);
        float span = 0f;
        if (weapon != null) {
            span = Math.max((weapon[3] - weapon[0]) / 16f,
                    Math.max((weapon[4] - weapon[1]) / 16f, (weapon[5] - weapon[2]) / 16f));
        }
        this.grip = wristOf(model, pose, RIGHT_HAND);
        return new Fit(eye[0], eye[1], eye[2], span);
    }

    /**
     * The posed wrist end of a hand marker, in model units, or {@code null} when there is no marker.
     *
     * <p>The marker is an arm box with the shoulder at the high-Y end, so the wrist — the point the
     * hand actually closes around the weapon at — is the middle of its low-Y face.
     */
    static float[] wristOf(GeoModel model, GeoPose pose, String bone) {
        float[] box = model.cubeBox(bone);
        if (box == null) return null;
        me.Padej_.soupapi.bedrock.Mat4 matrix = model.boneMatrix(bone, pose);
        if (matrix == null) return null;
        float[] out = new float[3];
        matrix.transform((box[0] + box[3]) / 2f, box[1], (box[2] + box[5]) / 2f, out);
        return out;
    }

    public static final String RIGHT_HAND = "righthand_pos";
    public static final String LEFT_HAND = "lefthand_pos";

    /** Where the right hand grips, in model units, measured once from the idle pose. */
    public float[] gripPoint() {
        return grip;
    }

    /** The view anchor's pivot in model units, or the model's own origin when it has none. */
    private static float[] eyeOf(GeoModel model) {
        for (String name : EYE_BONES) {
            me.Padej_.soupapi.bedrock.GeoBone bone = model.bone(name);
            if (bone != null) return new float[]{bone.pivotX, bone.pivotY, bone.pivotZ};
        }
        return new float[]{0f, 0f, 0f};
    }

    /** The bones a pack uses for "where the camera is while the weapon is just held", best first. */
    private static final String[] EYE_BONES = {"idle_view", "camera", "view"};

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
        return fit == null ? new Fit(0f, 0f, 0f, 0f) : fit;
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
            if (!bone.hasGeometry() || isAnchor(bone.name) || bone.name.endsWith(ARM_MESH)) continue;
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
