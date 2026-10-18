package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Bedrock geometry model, posed and drawn.
 *
 * <p>Everything is in the model's own units — sixteenths of a block, +Y up, the model's own origin —
 * exactly as authored. The caller sets up the matrix that puts it in the world (or in the hand), so
 * nothing here has to know whether it is being drawn in a hand, on a player, or in a menu.
 *
 * <p>Each bone's cubes compile into one display list. A single weapon here reaches nine hundred and
 * fifty cubes, which is nearly six thousand quads: pushed through the tessellator every frame that
 * is enough to cost real frames on PojavLauncher, where gl4es turns each immediate-mode quad into
 * work on the CPU. Compiled once, a bone costs one {@code glCallList} and the animation only has to
 * move forty-odd matrices.
 */
public final class GeoModel {

    public final float textureWidth;
    public final float textureHeight;
    private final List<GeoBone> bones;
    private final List<GeoBone> roots;
    private final Map<String, GeoBone> byName;
    private boolean compiled;

    private GeoModel(float textureWidth, float textureHeight, List<GeoBone> bones) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bones = bones;
        this.roots = new ArrayList<GeoBone>();
        this.byName = new HashMap<String, GeoBone>();
        for (int i = 0; i < bones.size(); i++) {
            GeoBone bone = bones.get(i);
            bone.index = i;
            byName.put(bone.name, bone);
        }
        for (GeoBone bone : bones) {
            GeoBone parent = bone.parentName == null ? null : byName.get(bone.parentName);
            bone.parent = parent;
            if (parent == null) {
                roots.add(bone);
            } else {
                parent.children.add(bone);
            }
        }
    }

    public static GeoModel parse(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.size() == 0) {
            throw new IllegalArgumentException("no minecraft:geometry in the model");
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.has("description")
                ? geometry.getAsJsonObject("description") : new JsonObject();
        float texWidth = description.has("texture_width")
                ? description.get("texture_width").getAsFloat() : 16f;
        float texHeight = description.has("texture_height")
                ? description.get("texture_height").getAsFloat() : 16f;
        if (texWidth <= 0f) texWidth = 16f;
        if (texHeight <= 0f) texHeight = 16f;

        List<GeoBone> bones = new ArrayList<GeoBone>();
        if (geometry.has("bones") && geometry.get("bones").isJsonArray()) {
            for (JsonElement bone : geometry.getAsJsonArray("bones")) {
                if (bone.isJsonObject()) {
                    bones.add(new GeoBone(bone.getAsJsonObject(), texWidth, texHeight));
                }
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

    public int cubeCount() {
        int count = 0;
        for (GeoBone bone : bones) count += bone.cubes.size();
        return count;
    }

    /**
     * Hides or shows every bone whose name <em>ends with</em> {@code suffix}.
     *
     * <p>A suffix and not a substring, and that distinction was a real bug: the arms in these packs
     * are the bones {@code lefthand_pos} and {@code righthand_pos}, but matching "hand" anywhere in
     * the name also caught Desmoulins' Edge's {@code hand} (156 cubes — its grip) and the Executor's
     * {@code handle} (84 cubes). Both were hidden and left out of the fit, so those two weapons were
     * drawn with their largest piece missing and sized as though it were not there.
     */
    public void setHidden(String suffix, boolean hidden) {
        for (GeoBone bone : bones) {
            if (bone.name.endsWith(suffix)) bone.hidden = hidden;
        }
    }

    public GeoPose newPose() {
        return new GeoPose(bones.size());
    }

    // ---- drawing ---------------------------------------------------------------------------

    /** Draws the model in model units. Needs a GL context; compiles the display lists once. */
    public void render(GeoPose pose) {
        if (!compiled) compile();
        for (GeoBone root : roots) {
            renderBone(root, pose);
        }
    }

    private void renderBone(GeoBone bone, GeoPose pose) {
        int i = bone.index;
        float sx = pose.scaleX(i);
        float sy = pose.scaleY(i);
        float sz = pose.scaleZ(i);
        // A zero scale is how these packs hide a group - the two faces of the Dark Star, for one -
        // so the whole subtree can be skipped rather than drawn collapsed into a plane.
        if (Math.abs(sx) < 1.0E-4f && Math.abs(sy) < 1.0E-4f && Math.abs(sz) < 1.0E-4f) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(pose.posX(i), pose.posY(i), pose.posZ(i));
        GlStateManager.translate(bone.pivotX, bone.pivotY, bone.pivotZ);
        float rz = bone.rotZ + pose.rotZ(i);
        float ry = bone.rotY + pose.rotY(i);
        float rx = bone.rotX + pose.rotX(i);
        if (rz != 0f) GlStateManager.rotate(rz, 0f, 0f, 1f);
        if (ry != 0f) GlStateManager.rotate(ry, 0f, 1f, 0f);
        if (rx != 0f) GlStateManager.rotate(rx, 1f, 0f, 0f);
        if (sx != 1f || sy != 1f || sz != 1f) GlStateManager.scale(sx, sy, sz);
        GlStateManager.translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);

        if (bone.displayList != 0 && !bone.hidden) {
            GlStateManager.callList(bone.displayList);
        }
        for (int c = 0; c < bone.children.size(); c++) {
            renderBone(bone.children.get(c), pose);
        }
        GlStateManager.popMatrix();
    }

    private void compile() {
        compiled = true;
        float[] corners = new float[12];
        for (GeoBone bone : bones) {
            if (bone.cubes.isEmpty()) continue;
            int list = GLAllocation.generate();
            if (list == 0) continue;
            GL11.glNewList(list, GL11.GL_COMPILE);
            GL11.glBegin(GL11.GL_QUADS);
            for (GeoCube cube : bone.cubes) {
                for (int face = 0; face < GeoCube.FACES.length; face++) {
                    if (!cube.hasFace(face)) continue;
                    float[] uv = cube.uv(face);
                    float[] normal = cube.normal(face);
                    cube.corners(face, corners);
                    GL11.glNormal3f(normal[0], normal[1], normal[2]);
                    emit(corners, 0, uv[0], uv[1]);
                    emit(corners, 1, uv[2], uv[1]);
                    emit(corners, 2, uv[2], uv[3]);
                    emit(corners, 3, uv[0], uv[3]);
                }
            }
            GL11.glEnd();
            GL11.glEndList();
            bone.displayList = list;
        }
    }

    private static void emit(float[] corners, int corner, float u, float v) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex3f(corners[corner * 3], corners[corner * 3 + 1], corners[corner * 3 + 2]);
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

    // ---- measuring -------------------------------------------------------------------------

    /**
     * The axis-aligned bounds of a pose, in model units, as {@code {minX, minY, minZ, maxX, maxY,
     * maxZ}}.
     *
     * <p>Pure maths, no GL: this runs at load time so the first-person placement can be fitted to
     * the model rather than guessed per weapon.
     */
    public float[] bounds(GeoPose pose, String nameFilter) {
        return bounds(pose, nameFilter, null);
    }

    /**
     * @param nameFilter  when set, only bones whose name contains it are measured
     * @param nameExclude when set, bones whose name <em>ends with</em> it are left out — which is
     *                    how the weapon is measured without the arms it is held in. A suffix,
     *                    because two of these weapons have their own parts named "hand" and
     *                    "handle"
     */
    public float[] bounds(GeoPose pose, String nameFilter, String nameExclude) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (GeoBone root : roots) {
            measure(root, pose, new Mat4(), box, nameFilter, nameExclude);
        }
        if (box[0] > box[3]) return null;
        return box;
    }

    private void measure(GeoBone bone, GeoPose pose, Mat4 parent, float[] box, String nameFilter,
                         String nameExclude) {
        int i = bone.index;
        float sx = pose.scaleX(i);
        float sy = pose.scaleY(i);
        float sz = pose.scaleZ(i);
        if (Math.abs(sx) < 1.0E-4f && Math.abs(sy) < 1.0E-4f && Math.abs(sz) < 1.0E-4f) return;

        Mat4 m = parent.copy()
                .translate(pose.posX(i), pose.posY(i), pose.posZ(i))
                .translate(bone.pivotX, bone.pivotY, bone.pivotZ)
                .rotateDeg(bone.rotZ + pose.rotZ(i), 0f, 0f, 1f)
                .rotateDeg(bone.rotY + pose.rotY(i), 0f, 1f, 0f)
                .rotateDeg(bone.rotX + pose.rotX(i), 1f, 0f, 0f)
                .scale(sx, sy, sz)
                .translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);

        boolean included = nameFilter == null || bone.name.contains(nameFilter);
        if (nameExclude != null && bone.name.endsWith(nameExclude)) included = false;
        if (included) {
            float[] out = new float[3];
            float[] corner = new float[3];
            for (GeoCube cube : bone.cubes) {
                for (int c = 0; c < 8; c++) {
                    cube.corner(c, corner);
                    m.transform(corner[0], corner[1], corner[2], out);
                    for (int k = 0; k < 3; k++) {
                        if (out[k] < box[k]) box[k] = out[k];
                        if (out[k] > box[k + 3]) box[k + 3] = out[k];
                    }
                }
            }
        }
        for (GeoBone child : bone.children) {
            measure(child, pose, m, box, nameFilter, nameExclude);
        }
    }

    /**
     * The posed mesh vertex furthest from {@code from}, in model units.
     *
     * <p>On a weapon measured from its own centre that is the tip of the blade, which is what tells
     * the renderer which way the thing points — something the model files never say.
     */
    public float[] furthestFrom(GeoPose pose, float[] from, String nameExclude) {
        float[] best = null;
        float bestDistance = -1f;
        float[] out = new float[3];
        for (GeoBone root : roots) {
            float[] found = furthest(root, pose, new Mat4(), from, nameExclude, out);
            if (found == null) continue;
            float dx = found[0] - from[0];
            float dy = found[1] - from[1];
            float dz = found[2] - from[2];
            float distance = dx * dx + dy * dy + dz * dz;
            if (distance > bestDistance) {
                bestDistance = distance;
                best = new float[]{found[0], found[1], found[2]};
            }
        }
        return best;
    }

    private float[] furthest(GeoBone bone, GeoPose pose, Mat4 parent, float[] from,
                             String nameExclude, float[] scratch) {
        int i = bone.index;
        float sx = pose.scaleX(i);
        float sy = pose.scaleY(i);
        float sz = pose.scaleZ(i);
        if (Math.abs(sx) < 1.0E-4f && Math.abs(sy) < 1.0E-4f && Math.abs(sz) < 1.0E-4f) return null;

        Mat4 m = parent.copy()
                .translate(pose.posX(i), pose.posY(i), pose.posZ(i))
                .translate(bone.pivotX, bone.pivotY, bone.pivotZ)
                .rotateDeg(bone.rotZ + pose.rotZ(i), 0f, 0f, 1f)
                .rotateDeg(bone.rotY + pose.rotY(i), 0f, 1f, 0f)
                .rotateDeg(bone.rotX + pose.rotX(i), 1f, 0f, 0f)
                .scale(sx, sy, sz)
                .translate(-bone.pivotX, -bone.pivotY, -bone.pivotZ);

        float[] best = null;
        float bestDistance = -1f;
        boolean skip = nameExclude != null && bone.name.endsWith(nameExclude);
        if (!skip) {
            float[] corner = new float[3];
            for (GeoCube cube : bone.cubes) {
                for (int c = 0; c < 8; c++) {
                    cube.corner(c, corner);
                    m.transform(corner[0], corner[1], corner[2], scratch);
                    float dx = scratch[0] - from[0];
                    float dy = scratch[1] - from[1];
                    float dz = scratch[2] - from[2];
                    float distance = dx * dx + dy * dy + dz * dz;
                    if (distance > bestDistance) {
                        bestDistance = distance;
                        best = new float[]{scratch[0], scratch[1], scratch[2]};
                    }
                }
            }
        }
        for (GeoBone child : bone.children) {
            float[] found = furthest(child, pose, m, from, nameExclude, scratch);
            if (found == null) continue;
            float dx = found[0] - from[0];
            float dy = found[1] - from[1];
            float dz = found[2] - from[2];
            float distance = dx * dx + dy * dy + dz * dz;
            if (distance > bestDistance) {
                bestDistance = distance;
                best = found;
            }
        }
        return best;
    }

    /**
     * The posed transform of a bone, in model units.
     *
     * <p>This is what lets the player's own arm be hung off the pack's hand bone: the pack animates
     * {@code righthand_pos} whether or not its mesh is drawn, so an arm placed on that matrix moves
     * with the animation exactly, instead of being a static prop under a moving weapon.
     */
    public Mat4 boneMatrix(String name, GeoPose pose) {
        GeoBone bone = byName.get(name);
        if (bone == null) return null;
        List<GeoBone> chain = new ArrayList<GeoBone>();
        for (GeoBone b = bone; b != null; b = b.parent) chain.add(0, b);
        Mat4 m = new Mat4();
        for (GeoBone b : chain) {
            int i = b.index;
            m.translate(pose.posX(i), pose.posY(i), pose.posZ(i))
                    .translate(b.pivotX, b.pivotY, b.pivotZ)
                    .rotateDeg(b.rotZ + pose.rotZ(i), 0f, 0f, 1f)
                    .rotateDeg(b.rotY + pose.rotY(i), 0f, 1f, 0f)
                    .rotateDeg(b.rotX + pose.rotX(i), 1f, 0f, 0f)
                    .scale(pose.scaleX(i), pose.scaleY(i), pose.scaleZ(i))
                    .translate(-b.pivotX, -b.pivotY, -b.pivotZ);
        }
        return m;
    }

    /** The pivot of a bone in its own model space, unposed. */
    public float[] bonePivot(String name) {
        GeoBone bone = byName.get(name);
        return bone == null ? null : new float[]{bone.pivotX, bone.pivotY, bone.pivotZ};
    }

    /**
     * A bone's first cube as {@code {minX, minY, minZ, maxX, maxY, maxZ}}, unposed, in model units.
     *
     * <p>For the hand markers that box is the whole point: each is a vanilla 4×12×4 arm, shoulder at
     * the high-Y end and wrist at the low-Y end, which is both where the player's arm goes and the
     * proof that the pack is authored at a sixteenth of a block per unit. Reading the bone's pivot
     * instead — which these packs leave at {@code [0, 8, 0]} on every weapon — puts the arm five
     * units off to one side of the hand it belongs to.
     */
    public float[] cubeBox(String name) {
        GeoBone bone = byName.get(name);
        if (bone == null || bone.cubes.isEmpty()) return null;
        GeoCube cube = bone.cubes.get(0);
        return new float[]{cube.x0, cube.y0, cube.z0, cube.x1, cube.y1, cube.z1};
    }

    /** The posed position of a bone's pivot, in model units — used to hang effects off a blade. */
    public boolean pivotOf(String name, GeoPose pose, float[] out) {
        GeoBone bone = byName.get(name);
        if (bone == null) return false;
        Mat4 m = new Mat4();
        List<GeoBone> chain = new ArrayList<GeoBone>();
        for (GeoBone b = bone; b != null; b = b.parent) chain.add(0, b);
        for (GeoBone b : chain) {
            int i = b.index;
            m.translate(pose.posX(i), pose.posY(i), pose.posZ(i))
                    .translate(b.pivotX, b.pivotY, b.pivotZ)
                    .rotateDeg(b.rotZ + pose.rotZ(i), 0f, 0f, 1f)
                    .rotateDeg(b.rotY + pose.rotY(i), 0f, 1f, 0f)
                    .rotateDeg(b.rotX + pose.rotX(i), 1f, 0f, 0f)
                    .scale(pose.scaleX(i), pose.scaleY(i), pose.scaleZ(i))
                    .translate(-b.pivotX, -b.pivotY, -b.pivotZ);
        }
        m.transform(bone.pivotX, bone.pivotY, bone.pivotZ, out);
        return true;
    }
}
