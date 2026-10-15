package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code org.joml.Vector3f}; only the accessors and arithmetic the mod uses.
 *
 * <p>Public fields <i>and</i> getters are provided because the ported code mixes both styles.
 */
public final class Vector3f {

    public float x, y, z;

    public Vector3f() {
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3f add(float dx, float dy, float dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    public Vector3f mul(float scalar) {
        x *= scalar;
        y *= scalar;
        z *= scalar;
        return this;
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3f normalize() {
        float len = length();
        if (len > 1.0e-7f) {
            mul(1f / len);
        }
        return this;
    }
}
