package me.Padej_.soupapi.bedrock;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** One bone: a pivot, an optional base rotation, its cubes, and its children. */
public final class GeoBone {

    public final String name;
    public final String parentName;
    public final float pivotX, pivotY, pivotZ;
    public final float rotX, rotY, rotZ;
    public final List<GeoCube> cubes = new ArrayList<GeoCube>();
    public final List<GeoBone> children = new ArrayList<GeoBone>();
    /** Triangles attached to this bone, when the pack ships a mesh for it. */
    public GeoMesh.Group mesh;

    GeoBone parent;
    /** Set when the caller wants this bone's geometry left out; children still draw. */
    boolean hidden;
    /** Index into the model's flat bone array; also the index into a pose. */
    int index;
    /** The compiled display list for this bone's cubes, or 0 when it has none. */
    int displayList;

    GeoBone(JsonObject raw, float texWidth, float texHeight) {
        this.name = raw.has("name") ? raw.get("name").getAsString() : "";
        this.parentName = raw.has("parent") && !raw.get("parent").isJsonNull()
                ? raw.get("parent").getAsString() : null;

        // Bedrock → Java space, the same mapping the pack's own renderer uses: pivot X is mirrored,
        // and the rest rotation negates X and Y. The cubes are mirrored the same way in GeoCube, so
        // the whole model is drawn with no runtime X flip.
        float[] pivot = GeoCube.vec3(raw.get("pivot"), 0f);
        this.pivotX = -pivot[0];
        this.pivotY = pivot[1];
        this.pivotZ = pivot[2];

        float[] rotation = GeoCube.vec3(raw.get("rotation"), 0f);
        this.rotX = -rotation[0];
        this.rotY = -rotation[1];
        this.rotZ = rotation[2];

        if (raw.has("cubes") && raw.get("cubes").isJsonArray()) {
            for (com.google.gson.JsonElement cube : raw.getAsJsonArray("cubes")) {
                if (cube.isJsonObject()) {
                    cubes.add(new GeoCube(cube.getAsJsonObject(), texWidth, texHeight));
                }
            }
        }
    }

    public int index() {
        return index;
    }

    public GeoBone parent() {
        return parent;
    }

    public boolean hasGeometry() {
        return !cubes.isEmpty();
    }
}
