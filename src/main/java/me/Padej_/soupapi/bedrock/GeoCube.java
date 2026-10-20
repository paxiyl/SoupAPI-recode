package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One box of a Bedrock model, baked into Blockbench/GeckoLib "Java" space.
 *
 * <p>This is the convention the pack's own 1.8.9 renderer (DeltaForce Melee) uses, and matching it
 * exactly is what finally put the weapons where the pack draws them. The mapping, applied once at
 * load:
 * <ul>
 *   <li>cube origin X is mirrored: {@code x = -(origin.x + size.x)} .. {@code -origin.x};</li>
 *   <li>a cube's own rotation negates X and Y and turns about the pivot {@code (-pivot.x, pivot.y,
 *       pivot.z)}, in Z·Y·X order.</li>
 * </ul>
 * Bone pivots and rotations are mirrored the same way in {@link GeoBone}, and the model is then
 * drawn with no further X flip. Each face is baked to a quad of four vertices, {@code (x,y,z,u,v,
 * nx,ny,nz)} with the UV already divided by the texture size and a normal from the quad's diagonals.
 */
public final class GeoCube {

    /** Each quad is four vertices of eight floats: x, y, z, u, v, nx, ny, nz. */
    public final List<float[]> quads = new ArrayList<float[]>();

    private final float texWidth;
    private final float texHeight;

    GeoCube(JsonObject raw, float texWidth, float texHeight) {
        this.texWidth = texWidth;
        this.texHeight = texHeight;

        float[] o = vec3(raw.get("origin"), 0f);
        float[] s = vec3(raw.get("size"), 0f);
        float inf = raw.has("inflate") ? raw.get("inflate").getAsFloat() : 0f;

        // Mirror X across the origin, exactly like the reference renderer.
        float x0 = -(o[0] + s[0]) - inf, y0 = o[1] - inf, z0 = o[2] - inf;
        float x1 = -(o[0] + s[0]) + s[0] + inf, y1 = o[1] + s[1] + inf, z1 = o[2] + s[2] + inf;
        float[] P1 = {x0, y0, z0}, P2 = {x0, y0, z1}, P3 = {x0, y1, z0}, P4 = {x0, y1, z1};
        float[] P5 = {x1, y0, z0}, P6 = {x1, y0, z1}, P7 = {x1, y1, z0}, P8 = {x1, y1, z1};

        Mat4 m = null;
        if (raw.has("rotation")) {
            float[] cp = vec3(raw.get("pivot"), 0f);
            float[] cr = vec3(raw.get("rotation"), 0f);
            m = new Mat4();
            m.translate(-cp[0], cp[1], cp[2]);
            m.rotateZYX(-cr[0], -cr[1], cr[2]);
            m.translate(cp[0], -cp[1], -cp[2]);
        }

        JsonElement uvEl = raw.get("uv");
        if (uvEl != null && uvEl.isJsonObject()) {
            JsonObject f = uvEl.getAsJsonObject();
            face(f, "west", new float[][]{P4, P3, P1, P2}, m);
            face(f, "east", new float[][]{P7, P8, P6, P5}, m);
            face(f, "north", new float[][]{P3, P7, P5, P1}, m);
            face(f, "south", new float[][]{P8, P4, P2, P6}, m);
            face(f, "up", new float[][]{P4, P8, P7, P3}, m);
            face(f, "down", new float[][]{P1, P5, P6, P2}, m);
        } else if (uvEl != null && uvEl.isJsonArray()) {
            boxUv(vec2(uvEl), s, new float[][]{P1, P2, P3, P4, P5, P6, P7, P8}, m);
        }
    }

    private void face(JsonObject faces, String name, float[][] verts, Mat4 m) {
        if (!faces.has(name) || !faces.get(name).isJsonObject()) return;
        JsonObject f = faces.getAsJsonObject(name);
        if (!f.has("uv")) return;
        float[] at = vec2(f.get("uv"));
        float[] span = vec2(f.get("uv_size"));
        int rot = f.has("uv_rotation") ? ((f.get("uv_rotation").getAsInt() / 90) % 4 + 4) % 4 : 0;
        emit(verts, at[0], at[1], at[0] + span[0], at[1] + span[1], rot, m);
    }

    /** The classic single-corner unwrap, for models that predate per-face UVs. */
    private void boxUv(float[] at, float[] s, float[][] p, Mat4 m) {
        float u = at[0], v = at[1];
        float dx = (float) Math.floor(s[0]), dy = (float) Math.floor(s[1]), dz = (float) Math.floor(s[2]);
        float[] P1 = p[0], P2 = p[1], P3 = p[2], P4 = p[3], P5 = p[4], P6 = p[5], P7 = p[6], P8 = p[7];
        emit(new float[][]{P4, P3, P1, P2}, u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy, 0, m);
        emit(new float[][]{P7, P8, P6, P5}, u, v + dz, u + dz, v + dz + dy, 0, m);
        emit(new float[][]{P3, P7, P5, P1}, u + dz, v + dz, u + dz + dx, v + dz + dy, 0, m);
        emit(new float[][]{P8, P4, P2, P6}, u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy, 0, m);
        emit(new float[][]{P4, P8, P7, P3}, u + dz, v, u + dz + dx, v + dz, 0, m);
        emit(new float[][]{P1, P5, P6, P2}, u + dz + dx, v + dz, u + dz + dx + dx, v, 0, m);
    }

    private void emit(float[][] verts, float u1, float v1, float u2, float v2, int rot, Mat4 m) {
        float[][] uvs = {{u2, v1}, {u1, v1}, {u1, v2}, {u2, v2}};
        if (rot != 0) {
            float[][] r = new float[4][];
            for (int i = 0; i < 4; i++) r[(i + rot) % 4] = uvs[i];
            uvs = r;
        }
        float[][] p = new float[4][3];
        float[] out = new float[3];
        for (int i = 0; i < 4; i++) {
            if (m != null) {
                m.transform(verts[i][0], verts[i][1], verts[i][2], out);
                p[i][0] = out[0]; p[i][1] = out[1]; p[i][2] = out[2];
            } else {
                p[i][0] = verts[i][0]; p[i][1] = verts[i][1]; p[i][2] = verts[i][2];
            }
        }
        // normal from the two diagonals (robust for any quad, rotated or not)
        float ax = p[2][0] - p[0][0], ay = p[2][1] - p[0][1], az = p[2][2] - p[0][2];
        float bx = p[3][0] - p[1][0], by = p[3][1] - p[1][1], bz = p[3][2] - p[1][2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-7f) return; // degenerate
        nx /= len; ny /= len; nz /= len;
        float[] q = new float[32];
        for (int i = 0; i < 4; i++) {
            int b = i * 8;
            q[b] = p[i][0]; q[b + 1] = p[i][1]; q[b + 2] = p[i][2];
            q[b + 3] = uvs[i][0] / texWidth; q[b + 4] = uvs[i][1] / texHeight;
            q[b + 5] = nx; q[b + 6] = ny; q[b + 7] = nz;
        }
        quads.add(q);
    }

    static float[] vec3(JsonElement element, float fallback) {
        float[] out = {fallback, fallback, fallback};
        if (element == null || element.isJsonNull()) return out;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 1) {
                float v = array.get(0).getAsFloat();
                return new float[]{v, v, v};
            }
            for (int i = 0; i < 3 && i < array.size(); i++) out[i] = array.get(i).getAsFloat();
        } else if (element.isJsonPrimitive()) {
            float v = element.getAsFloat();
            return new float[]{v, v, v};
        }
        return out;
    }

    private static float[] vec2(JsonElement element) {
        float[] out = {0f, 0f};
        if (element == null || !element.isJsonArray()) return out;
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < 2 && i < array.size(); i++) out[i] = array.get(i).getAsFloat();
        return out;
    }
}
