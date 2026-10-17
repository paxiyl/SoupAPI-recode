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
     * How the first-person placement was worked out, so the settings screen can say so.
     *
     * <p>Two fits, because the answer depends on whether the pack's arms are drawn. With the weapon
     * alone the blade sets the scale and sits where a held knife sits. With the arms the whole
     * assembly has to be framed and pushed out, because the arms reach back past the camera and
     * close up you are looking at the inside of a sleeve.
     */
    public static final class Fit {
        public final float scale;
        public final float offsetX;
        public final float offsetY;
        public final float offsetZ;
        /** The weapon's own size in blocks, for the picker's size column. */
        public final float span;
        /** The same, fitted to the whole model so the arms are in frame too. */
        public final float armScale;
        public final float armOffsetX;
        public final float armOffsetY;
        public final float armOffsetZ;
        /** The turn that points this blade the same way as every other, as an axis and an angle. */
        public final float alignDegrees;
        public final float alignX;
        public final float alignY;
        public final float alignZ;

        Fit(float scale, float offsetX, float offsetY, float offsetZ, float span,
            float armScale, float armOffsetX, float armOffsetY, float armOffsetZ,
            float alignDegrees, float alignX, float alignY, float alignZ) {
            this.scale = scale;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.span = span;
            this.armScale = armScale;
            this.armOffsetX = armOffsetX;
            this.armOffsetY = armOffsetY;
            this.armOffsetZ = armOffsetZ;
            this.alignDegrees = alignDegrees;
            this.alignX = alignX;
            this.alignY = alignY;
            this.alignZ = alignZ;
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
     * Works out where the model sits in front of the eye.
     *
     * <p>A pack's models carry their own view anchors, and those anchors disagree between weapons —
     * in this one pack the same bone names sit sixty units apart — so taking one of them as the eye
     * puts half the weapons out of frame. The idle pose is measured instead.
     *
     * <p>What is measured is the <em>weapon</em>, not the whole model. Measuring everything meant
     * the arms decided the size, and the arms are a metre of forearm reaching back toward the
     * camera: normalising that to a block left the blade small, the sleeves enormous, and the
     * assembly sprawling off every edge of the screen. A knife should be about half a block of
     * blade, however long the arms holding it are, so the blade is what sets the scale and what
     * gets centred; the arms land wherever they fall, which is behind it, where they belong.
     */
    private Fit measure(GeoModel model, GeoAnimations animations, GeoPose pose) {
        GeoClip idle = animations.first("static_idle", "idle", "draw");
        pose.reset();
        if (idle != null) pose.sample(model, idle, 0f, 1f);

        float[] weapon = model.bounds(pose, null, ARM_MESH);
        float[] whole = model.bounds(pose, null);
        if (weapon == null) weapon = whole;
        if (weapon == null) return new Fit(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f);

        // A weapon this long in blocks at Size 100: half a block of blade reads as a knife in the
        // hand, a whole block reads as a lamppost.
        float[] blade = place(weapon, 0.55f);
        // With the arms in, the whole assembly is framed to about a block and pushed a third of a
        // block further out, so the sleeves are seen from outside rather than from within.
        float[] withArms = whole == null ? blade : place(whole, 1.0f);

        float[] align = align(model, pose, weapon);
        return new Fit(blade[0], blade[1], blade[2], blade[3], blade[4],
                withArms[0], withArms[1], withArms[2], withArms[3] - 0.35f,
                align[0], align[1], align[2], align[3]);
    }

    /** The direction a held blade should point, in the space the weapon is drawn in. */
    private static final float[] BLADE_TARGET = normalise(-0.40f, 0.86f, -0.32f);

    /**
     * The turn that points this weapon's blade where every other weapon's blade points.
     *
     * <p>Fitting a weapon moves it and sizes it, and for the first build that was all it did — which
     * is why some of them came out plainly wrong while others looked fine. The packs do not agree
     * on which way a weapon faces in its own model: measured here, seven of the eleven point their
     * blade along -X, Dragon's Fang and Creed point up and away, Sea Gleam points <em>down</em>, and
     * Mercy points back at the camera. Whatever the mod they were built for does about that, it is
     * not something this port can read out of the files.
     *
     * <p>So the blade is found — the mesh vertex furthest from the weapon's centre, which on a knife
     * is the tip — and the model is turned so that direction matches {@link #BLADE_TARGET}: up, a
     * little to the left, a little away. Every weapon then sits in the hand the same way, and the
     * animation still plays on top of it because this is one fixed turn applied around the weapon's
     * own centre, not a change to any bone.
     */
    private static float[] align(GeoModel model, GeoPose pose, float[] box) {
        float[] centre = {
                (box[0] + box[3]) / 2f, (box[1] + box[4]) / 2f, (box[2] + box[5]) / 2f,
        };
        float[] tip = model.furthestFrom(pose, centre, ARM_MESH);
        if (tip == null) return new float[]{0f, 0f, 1f, 0f};

        // The model is mirrored in X when drawn, so the direction is measured mirrored too.
        float[] direction = normalise(-(tip[0] - centre[0]), tip[1] - centre[1], tip[2] - centre[2]);
        if (direction == null) return new float[]{0f, 0f, 1f, 0f};

        float dot = direction[0] * BLADE_TARGET[0] + direction[1] * BLADE_TARGET[1]
                + direction[2] * BLADE_TARGET[2];
        dot = Math.max(-1f, Math.min(1f, dot));
        float[] axis = {
                direction[1] * BLADE_TARGET[2] - direction[2] * BLADE_TARGET[1],
                direction[2] * BLADE_TARGET[0] - direction[0] * BLADE_TARGET[2],
                direction[0] * BLADE_TARGET[1] - direction[1] * BLADE_TARGET[0],
        };
        float length = (float) Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        if (length < 1.0E-5f) {
            // Already aligned, or pointing exactly the other way; turn about any perpendicular.
            if (dot > 0f) return new float[]{0f, 0f, 1f, 0f};
            return new float[]{180f, 0f, 0f, 1f};
        }
        float degrees = (float) Math.toDegrees(Math.acos(dot));
        return new float[]{degrees, axis[0] / length, axis[1] / length, axis[2] / length};
    }

    private static float[] normalise(float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-6f) return null;
        return new float[]{x / length, y / length, z / length};
    }

    /** Returns {scale, offsetX, offsetY, offsetZ, span} for a box and a target size in blocks. */
    private static float[] place(float[] box, float target) {
        float spanX = (box[3] - box[0]) / 16f;
        float spanY = (box[4] - box[1]) / 16f;
        float spanZ = (box[5] - box[2]) / 16f;
        float span = Math.max(spanX, Math.max(spanY, spanZ));
        float scale = span > 1.0E-4f ? target / span : 1f;
        // The model is mirrored in X when it is drawn, so its centre offset is mirrored too.
        float centreX = -(box[0] + box[3]) / 32f * scale;
        float centreY = (box[1] + box[4]) / 32f * scale;
        float centreZ = (box[2] + box[5]) / 32f * scale;
        return new float[]{scale, -centreX, -centreY, -centreZ, span};
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
        return fit == null
                ? new Fit(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
                : fit;
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
