package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code org.joml.Vector4f}.
 *
 * <p>{@link #mul(Mat4)} follows JOML's convention of computing {@code matrix * this} (i.e. it
 * transforms the vector by the matrix), which is what the target-HUD world-to-screen projection
 * relies on.
 */
public final class Vector4f {

    public float x, y, z, w;

    public Vector4f() {
    }

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
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

    public float w() {
        return w;
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

    public float getW() {
        return w;
    }

    public Vector4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /** {@code this = matrix * this}, matching JOML's {@code Vector4f.mul(Matrix4fc)}. */
    public Vector4f mul(Mat4 m) {
        float nx = m.m00 * x + m.m10 * y + m.m20 * z + m.m30 * w;
        float ny = m.m01 * x + m.m11 * y + m.m21 * z + m.m31 * w;
        float nz = m.m02 * x + m.m12 * y + m.m22 * z + m.m32 * w;
        float nw = m.m03 * x + m.m13 * y + m.m23 * z + m.m33 * w;
        x = nx;
        y = ny;
        z = nz;
        w = nw;
        return this;
    }
}
