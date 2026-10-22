package me.Padej_.soupapi.bedrock;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Triangle geometry for a model's bones, read from a pack's {@code .mesh} file.
 *
 * <p>Bedrock geometry is boxes, and boxes are why a weapon built from it reads as a toy: an edge is
 * always a right angle and a curve is always a staircase. The weapons in this pack are meant to
 * have curling tendrils, braided shafts, feathers and petals, none of which is a box. This carries
 * arbitrary triangles with their own normals instead, grouped by bone name, so a weapon can be any
 * shape while the rig, the clips, the poses, the glow passes and the per-bone display lists stay
 * exactly as they are: a mesh group is attached to its bone and rides it like a cube would.
 *
 * <p>The file is binary because the text of one of these is megabytes and a phone has to parse it:
 *
 * <pre>
 *   magic   "SMSH"            4 bytes
 *   version 1                 u16
 *   scale                     f32   model units per stored unit
 *   groups                    u16
 *   per group:
 *     name                    UTF    the bone it belongs to
 *     vertices                u16
 *       position              3 x s16   (x scale)
 *       uv                    2 x u16   (0..65535 across the texture)
 *       normal                3 x s8    (-127..127)
 *       padding               1 byte
 *     indices                 u16 count, then that many u16
 * </pre>
 *
 * <p>Positions are quantised to a sixteenth of a model unit and normals to a byte, which is well
 * under what a weapon held at arm's length can show, and makes the whole thing about a tenth of
 * the size the same geometry would be as JSON.
 */
public final class GeoMesh {

    /** One bone's triangles, already unpacked into the layout the display list wants. */
    public static final class Group {
        /** Interleaved x, y, z, u, v, nx, ny, nz per vertex. */
        public final float[] vertices;
        public final int[] indices;

        Group(float[] vertices, int[] indices) {
            this.vertices = vertices;
            this.indices = indices;
        }

        public int triangles() {
            return indices.length / 3;
        }
    }

    private static final int MAGIC = ('S' << 24) | ('M' << 16) | ('S' << 8) | 'H';
    private static final int FLOATS_PER_VERTEX = 8;

    private final Map<String, Group> groups;

    private GeoMesh(Map<String, Group> groups) {
        this.groups = groups;
    }

    public Group group(String bone) {
        return groups.get(bone);
    }

    public int size() {
        return groups.size();
    }

    public int triangles() {
        int n = 0;
        for (Group g : groups.values()) n += g.triangles();
        return n;
    }

    /**
     * Reads a mesh file.
     *
     * <p>Mirrors the Bedrock → Java flip the cubes get in {@link GeoCube}: X is negated on both
     * positions and normals, and the winding is reversed to match, so a mesh and a cube on the same
     * bone end up in the same space and face the same way.
     */
    public static GeoMesh read(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(raw, 1 << 16));
        try {
            if (in.readInt() != MAGIC) throw new IOException("not a mesh file");
            int version = in.readUnsignedShort();
            if (version != 1) throw new IOException("mesh version " + version + " is not supported");
            float scale = in.readFloat();
            int groupCount = in.readUnsignedShort();
            Map<String, Group> groups = new HashMap<String, Group>(groupCount * 2);
            for (int g = 0; g < groupCount; g++) {
                String name = in.readUTF();
                int vertexCount = in.readUnsignedShort();
                float[] vertices = new float[vertexCount * FLOATS_PER_VERTEX];
                for (int v = 0, o = 0; v < vertexCount; v++, o += FLOATS_PER_VERTEX) {
                    vertices[o] = -in.readShort() * scale;
                    vertices[o + 1] = in.readShort() * scale;
                    vertices[o + 2] = in.readShort() * scale;
                    vertices[o + 3] = (in.readUnsignedShort() & 0xFFFF) / 65535f;
                    vertices[o + 4] = (in.readUnsignedShort() & 0xFFFF) / 65535f;
                    vertices[o + 5] = -in.readByte() / 127f;
                    vertices[o + 6] = in.readByte() / 127f;
                    vertices[o + 7] = in.readByte() / 127f;
                    in.readByte();
                }
                int indexCount = in.readUnsignedShort() | (in.readUnsignedShort() << 16);
                int[] indices = new int[indexCount];
                for (int i = 0; i < indexCount; i += 3) {
                    // Reversed, because negating X turns every triangle inside out.
                    int a = in.readUnsignedShort();
                    int b = in.readUnsignedShort();
                    int c = in.readUnsignedShort();
                    indices[i] = c;
                    indices[i + 1] = b;
                    indices[i + 2] = a;
                }
                groups.put(name, new Group(vertices, indices));
            }
            return new GeoMesh(groups);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
}
