package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * One box of a Bedrock model.
 *
 * <p>Coordinates are the model's own, in sixteenths of a block, and {@code origin} is absolute
 * rather than relative to the owning bone — which is why a bone can be animated by rotating around
 * its pivot while its cubes keep the coordinates they were authored with.
 *
 * <p>The per-face UV form is the only one these packs use ({@code uv} as an object of six faces,
 * each with its own {@code uv} and {@code uv_size}); the older "box UV" form, a single corner the
 * faces are unwrapped from, is handled too so other packs load.
 */
public final class GeoCube {

    /** The six faces, in the order {@link #FACE_CORNERS} indexes them. */
    public static final String[] FACES = {"north", "south", "west", "east", "up", "down"};

    /**
     * Each face's four corners in texture order — top-left, top-right, bottom-right, bottom-left
     * as seen from outside the box — given as indices into the eight box corners.
     *
     * <p>Corner i has x = (i &amp; 1) ? x1 : x0, y = (i &amp; 4) ? y1 : y0, z = (i &amp; 2) ? z1 : z0.
     */
    private static final int[][] FACE_CORNERS = {
            {5, 4, 0, 1},   // north  (-Z): up is +Y, texture-left is +X
            {6, 7, 3, 2},   // south  (+Z)
            {7, 4, 0, 3},   // west   (-X)
            {5, 6, 2, 1},   // east   (+X)
            {4, 5, 6, 7},   // up     (+Y): texture-up is -Z
            {3, 2, 1, 0},   // down   (-Y): texture-up is +Z
    };

    private static final float[][] FACE_NORMALS = {
            {0f, 0f, -1f}, {0f, 0f, 1f}, {-1f, 0f, 0f}, {1f, 0f, 0f}, {0f, 1f, 0f}, {0f, -1f, 0f},
    };

    public final float x0, y0, z0, x1, y1, z1;
    public final float pivotX, pivotY, pivotZ;
    public final float rotX, rotY, rotZ;
    public final boolean rotated;
    /** Per face: u0, v0, u1, v1, already normalised; {@code null} when the face is not textured. */
    private final float[][] faceUv = new float[6][];

    GeoCube(JsonObject raw, float texWidth, float texHeight) {
        float[] origin = vec3(raw.get("origin"), 0f);
        float[] size = vec3(raw.get("size"), 0f);
        float inflate = raw.has("inflate") ? raw.get("inflate").getAsFloat() : 0f;

        this.x0 = origin[0] - inflate;
        this.y0 = origin[1] - inflate;
        this.z0 = origin[2] - inflate;
        this.x1 = origin[0] + size[0] + inflate;
        this.y1 = origin[1] + size[1] + inflate;
        this.z1 = origin[2] + size[2] + inflate;

        float[] pivot = vec3(raw.get("pivot"), 0f);
        this.pivotX = pivot[0];
        this.pivotY = pivot[1];
        this.pivotZ = pivot[2];

        float[] rotation = vec3(raw.get("rotation"), 0f);
        this.rotX = rotation[0];
        this.rotY = rotation[1];
        this.rotZ = rotation[2];
        this.rotated = rotX != 0f || rotY != 0f || rotZ != 0f;

        JsonElement uv = raw.get("uv");
        if (uv != null && uv.isJsonObject()) {
            JsonObject faces = uv.getAsJsonObject();
            for (int i = 0; i < FACES.length; i++) {
                JsonElement face = faces.get(FACES[i]);
                if (face == null || !face.isJsonObject()) continue;
                JsonObject f = face.getAsJsonObject();
                float[] at = vec2(f.get("uv"));
                float[] span = vec2(f.get("uv_size"));
                faceUv[i] = new float[]{
                        at[0] / texWidth, at[1] / texHeight,
                        (at[0] + span[0]) / texWidth, (at[1] + span[1]) / texHeight,
                };
            }
        } else if (uv != null && uv.isJsonArray()) {
            boxUv(vec2(uv), size, texWidth, texHeight);
        }
    }

    /** The classic single-corner unwrap, for models that predate per-face UVs. */
    private void boxUv(float[] at, float[] size, float texWidth, float texHeight) {
        float u = at[0];
        float v = at[1];
        float dx = size[0];
        float dy = size[1];
        float dz = size[2];
        put(0, u + dz, v + dz, u + dz + dx, v + dz + dy, texWidth, texHeight);           // north
        put(1, u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy, texWidth, texHeight);
        put(2, u, v + dz, u + dz, v + dz + dy, texWidth, texHeight);                     // west
        put(3, u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy, texWidth, texHeight); // east
        put(4, u + dz, v, u + dz + dx, v + dz, texWidth, texHeight);                     // up
        put(5, u + dz + dx, v + dz, u + dz + dx + dx, v, texWidth, texHeight);           // down
    }

    private void put(int face, float u0, float v0, float u1, float v1, float tw, float th) {
        faceUv[face] = new float[]{u0 / tw, v0 / th, u1 / tw, v1 / th};
    }

    public boolean hasFace(int face) {
        return faceUv[face] != null;
    }

    public float[] uv(int face) {
        return faceUv[face];
    }

    public float[] normal(int face) {
        return FACE_NORMALS[face];
    }

    /** Writes a face's four corners, in texture order, into {@code out} as x,y,z triples. */
    public void corners(int face, float[] out) {
        int[] indices = FACE_CORNERS[face];
        for (int i = 0; i < 4; i++) {
            int c = indices[i];
            out[i * 3] = (c & 1) != 0 ? x1 : x0;
            out[i * 3 + 1] = (c & 4) != 0 ? y1 : y0;
            out[i * 3 + 2] = (c & 2) != 0 ? z1 : z0;
        }
    }

    static float[] vec3(JsonElement element, float fallback) {
        float[] out = {fallback, fallback, fallback};
        if (element == null || !element.isJsonArray()) return out;
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < 3 && i < array.size(); i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    private static float[] vec2(JsonElement element) {
        float[] out = {0f, 0f};
        if (element == null || !element.isJsonArray()) return out;
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < 2 && i < array.size(); i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }
}
