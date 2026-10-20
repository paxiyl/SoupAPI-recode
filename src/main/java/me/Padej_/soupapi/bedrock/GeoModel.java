package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Bedrock geometry model, posed and drawn the way the pack's own 1.8.9 renderer draws it.
 *
 * <p>Geometry is baked into Blockbench/GeckoLib "Java" space at load (see {@link GeoCube} and
 * {@link GeoBone}): mirrored in X, cube and bone rotations negated on X and Y. A pose is then a set
 * of per-bone global matrices, computed in topological order, and the whole model is placed by one
 * {@code view} matrix — {@code scale(1/16) · inverse(global(idle_view) · T(pivot))} — so the pack's
 * own view anchor sits at the eye. Nothing here applies a runtime X flip.
 *
 * <p>Each bone's cubes compile into one display list, so a pose costs one matrix per bone and one
 * {@code glCallList}, which is what keeps a thousand-cube weapon cheap on gl4es.
 */
public final class GeoModel {

    public final float textureWidth;
    public final float textureHeight;
    private final List<GeoBone> bones;
    private final Map<String, GeoBone> byName;
    /** Bone indices with every parent before its children. */
    private final int[] order;
    /** Posed global transform per bone, refreshed by {@link #computeGlobals}. */
    private final Mat4[] globals;
    private boolean compiled;
    private int listBase = -1;

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

    private GeoModel(float textureWidth, float textureHeight, List<GeoBone> bones) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bones = bones;
        this.byName = new HashMap<String, GeoBone>();
        this.globals = new Mat4[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            GeoBone bone = bones.get(i);
            bone.index = i;
            byName.put(bone.name, bone);
            globals[i] = new Mat4();
        }
        for (GeoBone bone : bones) {
            GeoBone parent = bone.parentName == null ? null : byName.get(bone.parentName);
            bone.parent = parent;
            if (parent != null) parent.children.add(bone);
        }
        this.order = topological(bones);
    }

    private static int[] topological(List<GeoBone> bones) {
        int n = bones.size();
        int[] out = new int[n];
        boolean[] done = new boolean[n];
        int filled = 0;
        while (filled < n) {
            boolean progressed = false;
            for (int i = 0; i < n; i++) {
                if (done[i]) continue;
                GeoBone b = bones.get(i);
                if (b.parent == null || done[b.parent.index]) {
                    done[i] = true;
                    out[filled++] = i;
                    progressed = true;
                }
            }
            if (!progressed) { // a cycle: break it so loading still succeeds
                for (int i = 0; i < n; i++) {
                    if (!done[i]) bones.get(i).parent = null;
                }
            }
        }
        return out;
    }

    public static GeoModel parse(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.size() == 0) {
            throw new IllegalArgumentException("no minecraft:geometry in the model");
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.has("description")
                ? geometry.getAsJsonObject("description") : new JsonObject();
        float texWidth = description.has("texture_width") ? description.get("texture_width").getAsFloat() : 16f;
        float texHeight = description.has("texture_height") ? description.get("texture_height").getAsFloat() : 16f;
        if (texWidth <= 0f) texWidth = 16f;
        if (texHeight <= 0f) texHeight = 16f;

        List<GeoBone> bones = new ArrayList<GeoBone>();
        if (geometry.has("bones") && geometry.get("bones").isJsonArray()) {
            for (JsonElement bone : geometry.getAsJsonArray("bones")) {
                if (bone.isJsonObject()) bones.add(new GeoBone(bone.getAsJsonObject(), texWidth, texHeight));
            }
        }
        return new GeoModel(texWidth, texHeight, bones);
    }

    public int boneCount() {
        return bones.size();
    }

    public List<GeoBone> bones() {
        return bones;
    }

    public GeoBone bone(String name) {
        return byName.get(name);
    }

    public int indexOf(String name) {
        GeoBone b = byName.get(name);
        return b == null ? -1 : b.index;
    }

    public int cubeCount() {
        int count = 0;
        for (GeoBone bone : bones) count += bone.cubes.size();
        return count;
    }

    /** Hides or shows every bone whose name ends with {@code suffix} (and, by descent, its cubes). */
    public void setHidden(String suffix, boolean hidden) {
        for (GeoBone bone : bones) {
            if (bone.name.endsWith(suffix)) bone.hidden = hidden;
        }
    }

    public GeoPose newPose() {
        return new GeoPose(bones.size());
    }

    // ---- posing ----------------------------------------------------------------------------

    /**
     * Recomputes every bone's global transform for a pose, in model units.
     *
     * <p>The pose arrays hold raw Bedrock values; the Bedrock → Java negation (position X, rotation
     * X and Y) is applied here so the clips and {@link GeoPose} can stay in the authored space.
     */
    public void computeGlobals(GeoPose pose) {
        for (int bi : order) {
            GeoBone b = bones.get(bi);
            Mat4 g = globals[bi];
            if (b.parent != null) g.set(globals[b.parent.index]);
            else g.identity();
            int i = b.index;
            g.translate(-pose.posX(i), pose.posY(i), pose.posZ(i));
            g.translate(b.pivotX, b.pivotY, b.pivotZ);
            g.rotateZYX(b.rotX - pose.rotX(i), b.rotY - pose.rotY(i), b.rotZ + pose.rotZ(i));
            g.scale(pose.scaleX(i), pose.scaleY(i), pose.scaleZ(i));
            g.translate(-b.pivotX, -b.pivotY, -b.pivotZ);
        }
    }

    /** The posed global transform of a bone (after {@link #computeGlobals}); do not mutate. */
    public Mat4 global(int index) {
        return globals[index];
    }

    /**
     * The view transform that puts a bone's pivot at the eye: {@code scale(1/16) · inverse(global ·
     * T(pivot))}. Call after {@link #computeGlobals}. Falls back to {@code scale(1/16)} if the bone
     * is missing or the matrix is singular.
     */
    public Mat4 viewFromBone(String name) {
        Mat4 view = new Mat4().scale(1f / 16f, 1f / 16f, 1f / 16f);
        GeoBone b = byName.get(name);
        if (b == null) return view;
        Mat4 inner = globals[b.index].copy().translate(b.pivotX, b.pivotY, b.pivotZ);
        if (!inner.invert()) return view;
        return view.multiply(inner);
    }

    // ---- drawing ---------------------------------------------------------------------------

    /** Draws the model with a pre-set GL matrix, in model units (view = identity). */
    public void render(GeoPose pose) {
        computeGlobals(pose);
        drawPosed(IDENTITY);
    }

    private static final Mat4 IDENTITY = new Mat4();

    /**
     * Draws every visible bone at {@code view · global(bone)} via {@code glMultMatrix}. The globals
     * must already be current ({@link #computeGlobals}); the view carries the model-units-to-eye
     * transform (or identity to draw in model units).
     */
    public void drawPosed(Mat4 view) {
        if (!compiled) compile();
        Mat4 m = new Mat4();
        for (GeoBone bone : bones) {
            if (bone.displayList == 0 || hiddenWithAncestors(bone)) continue;
            m.set(view).multiply(globals[bone.index]);
            GlStateManager.pushMatrix();
            m.store(MATRIX);
            GlStateManager.multMatrix(MATRIX);
            GlStateManager.callList(bone.displayList);
            GlStateManager.popMatrix();
        }
    }

    /**
     * Draws only the bones whose name contains {@code marker} (with {@code include}), or only those
     * that do not (without). The emissive pass uses this to redraw a weapon's {@code _illuminated}
     * parts - eyes, runes, gems - on their own, lit by themselves.
     */
    public void drawPosed(Mat4 view, String marker, boolean include) {
        if (!compiled) compile();
        Mat4 m = new Mat4();
        for (GeoBone bone : bones) {
            if (bone.displayList == 0 || hiddenWithAncestors(bone)) continue;
            if (bone.name.contains(marker) != include) continue;
            m.set(view).multiply(globals[bone.index]);
            GlStateManager.pushMatrix();
            m.store(MATRIX);
            GlStateManager.multMatrix(MATRIX);
            GlStateManager.callList(bone.displayList);
            GlStateManager.popMatrix();
        }
    }

    /** True when any bone's name contains {@code marker}. */
    public boolean hasBone(String marker) {
        for (GeoBone bone : bones) {
            if (bone.name.contains(marker)) return true;
        }
        return false;
    }

    private static boolean hiddenWithAncestors(GeoBone bone) {
        for (GeoBone b = bone; b != null; b = b.parent) {
            if (b.hidden) return true;
        }
        return false;
    }

    /** True when a bone, or any ancestor, is hidden — so its arm marker should not draw either. */
    public boolean hiddenWithAncestors(int index) {
        return hiddenWithAncestors(bones.get(index));
    }

    /**
     * True when a bone's <em>parent chain</em> is hidden — not the bone itself.
     *
     * <p>The hand markers are hidden so their placeholder cubes never draw, but the player's arm is
     * still drawn into them; it must be suppressed only when an actual ancestor is hidden (a put-away
     * variant), never because the marker itself is.
     */
    public boolean ancestorHidden(int index) {
        GeoBone parent = bones.get(index).parent;
        return parent != null && hiddenWithAncestors(parent);
    }

    private void compile() {
        compiled = true;
        for (GeoBone bone : bones) {
            if (bone.cubes.isEmpty()) continue;
            int list = GLAllocation.generate();
            if (list == 0) continue;
            GL11.glNewList(list, GL11.GL_COMPILE);
            GL11.glBegin(GL11.GL_QUADS);
            for (GeoCube cube : bone.cubes) {
                for (float[] q : cube.quads) {
                    for (int i = 0; i < 4; i++) {
                        int o = i * 8;
                        GL11.glNormal3f(q[o + 5], q[o + 6], q[o + 7]);
                        GL11.glTexCoord2f(q[o + 3], q[o + 4]);
                        GL11.glVertex3f(q[o], q[o + 1], q[o + 2]);
                    }
                }
            }
            GL11.glEnd();
            GL11.glEndList();
            bone.displayList = list;
        }
    }

    /** Frees the compiled lists; called when a weapon is unloaded. */
    public void release() {
        for (GeoBone bone : bones) {
            if (bone.displayList != 0) {
                GLAllocation.free(bone.displayList);
                bone.displayList = 0;
            }
        }
        compiled = false;
    }

    // ---- measuring (pure maths, no GL) -----------------------------------------------------

    public float[] bounds(GeoPose pose, String nameFilter) {
        return bounds(pose, nameFilter, null);
    }

    /**
     * Axis-aligned bounds of a pose in model units, as {@code {minX, minY, minZ, maxX, maxY, maxZ}}.
     *
     * @param nameFilter  when set, only bones whose name contains it are measured
     * @param nameExclude when set, bones whose name ends with it are left out (e.g. the hand markers)
     */
    public float[] bounds(GeoPose pose, String nameFilter, String nameExclude) {
        computeGlobals(pose);
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        float[] out = new float[3];
        for (GeoBone bone : bones) {
            if (nameFilter != null && !bone.name.contains(nameFilter)) continue;
            if (nameExclude != null && bone.name.endsWith(nameExclude)) continue;
            Mat4 g = globals[bone.index];
            for (GeoCube cube : bone.cubes) {
                for (float[] q : cube.quads) {
                    for (int i = 0; i < 4; i++) {
                        g.transform(q[i * 8], q[i * 8 + 1], q[i * 8 + 2], out);
                        for (int k = 0; k < 3; k++) {
                            if (out[k] < box[k]) box[k] = out[k];
                            if (out[k] > box[k + 3]) box[k + 3] = out[k];
                        }
                    }
                }
            }
        }
        if (box[0] > box[3]) return null;
        return box;
    }

    /** The posed mesh vertex furthest from {@code from}, in model units — the blade tip on a knife. */
    public float[] furthestFrom(GeoPose pose, float[] from, String nameExclude) {
        computeGlobals(pose);
        float[] best = null;
        float bestDistance = -1f;
        float[] out = new float[3];
        for (GeoBone bone : bones) {
            if (nameExclude != null && bone.name.endsWith(nameExclude)) continue;
            Mat4 g = globals[bone.index];
            for (GeoCube cube : bone.cubes) {
                for (float[] q : cube.quads) {
                    for (int i = 0; i < 4; i++) {
                        g.transform(q[i * 8], q[i * 8 + 1], q[i * 8 + 2], out);
                        float dx = out[0] - from[0], dy = out[1] - from[1], dz = out[2] - from[2];
                        float d = dx * dx + dy * dy + dz * dz;
                        if (d > bestDistance) {
                            bestDistance = d;
                            best = new float[]{out[0], out[1], out[2]};
                        }
                    }
                }
            }
        }
        return best;
    }

    /** The posed global transform of a bone, as a fresh matrix. */
    public Mat4 boneMatrix(String name, GeoPose pose) {
        GeoBone bone = byName.get(name);
        if (bone == null) return null;
        computeGlobals(pose);
        return globals[bone.index].copy();
    }

    /** The pivot of a bone in its own (already mirrored) model space, unposed. */
    public float[] bonePivot(String name) {
        GeoBone bone = byName.get(name);
        return bone == null ? null : new float[]{bone.pivotX, bone.pivotY, bone.pivotZ};
    }

    /** The posed position of a bone's pivot, in model units — used to hang effects off a blade. */
    public boolean pivotOf(String name, GeoPose pose, float[] out) {
        GeoBone bone = byName.get(name);
        if (bone == null) return false;
        computeGlobals(pose);
        globals[bone.index].transform(bone.pivotX, bone.pivotY, bone.pivotZ, out);
        return true;
    }

    /**
     * A bone's first cube as {@code {minX, minY, minZ, maxX, maxY, maxZ}}, unposed, in model units.
     * For the hand markers this is the vanilla 4×12×4 arm box the player's arm is drawn into.
     */
    public float[] cubeBox(String name) {
        GeoBone bone = byName.get(name);
        if (bone == null || bone.cubes.isEmpty()) return null;
        GeoCube cube = bone.cubes.get(0);
        if (cube.quads.isEmpty()) return null;
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (float[] q : cube.quads) {
            for (int i = 0; i < 4; i++) {
                for (int k = 0; k < 3; k++) {
                    float v = q[i * 8 + k];
                    if (v < box[k]) box[k] = v;
                    if (v > box[k + 3]) box[k + 3] = v;
                }
            }
        }
        return box;
    }
}
