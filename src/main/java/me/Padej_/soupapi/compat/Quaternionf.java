package me.Padej_.soupapi.compat;

import me.Padej_.soupapi.compat.Mat4;

/**
 * Replacement for {@code org.joml.Quaternionf}, which 1.8.9 has no equivalent of.
 *
 * <p>Only the operations the mod uses are implemented, but they follow JOML's conventions exactly,
 * because getting them wrong would silently mis-orient the billboarded soul/halo particles:
 *
 * <ul>
 *   <li>{@code rotateX/Y/Z} are <i>intrinsic</i> (local) rotations. JOML documents them as
 *       "the new quaternion will be Q * R, so the rotation represented by R will be applied
 *       first", so each is implemented as a right-multiplication by the axis rotation.</li>
 *   <li>{@link #get(Mat4)} produces the same rotation matrix JOML's
 *       {@code Matrix4f.rotate(Quaternionf)} would compose.</li>
 * </ul>
 */
public final class Quaternionf {

    public float x, y, z, w;

    public Quaternionf() {
        identity();
    }

    public Quaternionf(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternionf identity() {
        x = 0f;
        y = 0f;
        z = 0f;
        w = 1f;
        return this;
    }

    public Quaternionf set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /** {@code this = this * r} (Hamilton product). */
    public Quaternionf mul(Quaternionf r) {
        float nx = w * r.x + x * r.w + y * r.z - z * r.y;
        float ny = w * r.y - x * r.z + y * r.w + z * r.x;
        float nz = w * r.z + x * r.y - y * r.x + z * r.w;
        float nw = w * r.w - x * r.x - y * r.y - z * r.z;
        x = nx;
        y = ny;
        z = nz;
        w = nw;
        return this;
    }

    private Quaternionf rotateAxis(float angleRad, float ax, float ay, float az) {
        float half = angleRad * 0.5f;
        float s = (float) Math.sin(half);
        float c = (float) Math.cos(half);
        return mul(new Quaternionf(ax * s, ay * s, az * s, c));
    }

    public Quaternionf rotateX(float angleRad) {
        return rotateAxis(angleRad, 1f, 0f, 0f);
    }

    public Quaternionf rotateY(float angleRad) {
        return rotateAxis(angleRad, 0f, 1f, 0f);
    }

    public Quaternionf rotateZ(float angleRad) {
        return rotateAxis(angleRad, 0f, 0f, 1f);
    }

    public Quaternionf rotationX(float angleRad) {
        return identity().rotateX(angleRad);
    }

    public Quaternionf rotationY(float angleRad) {
        return identity().rotateY(angleRad);
    }

    public Quaternionf rotationZ(float angleRad) {
        return identity().rotateZ(angleRad);
    }

    public Quaternionf normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (len > 1.0e-7f) {
            float inv = 1f / len;
            x *= inv;
            y *= inv;
            z *= inv;
            w *= inv;
        }
        return this;
    }

    public Quaternionf conjugate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /** Writes this rotation into {@code dest} as a pure rotation matrix. */
    public Mat4 get(Mat4 dest) {
        float xx = x * x, yy = y * y, zz = z * z;
        float xy = x * y, xz = x * z, yz = y * z;
        float wx = w * x, wy = w * y, wz = w * z;

        dest.identity();
        dest.m00 = 1f - 2f * (yy + zz);
        dest.m01 = 2f * (xy + wz);
        dest.m02 = 2f * (xz - wy);

        dest.m10 = 2f * (xy - wz);
        dest.m11 = 1f - 2f * (xx + zz);
        dest.m12 = 2f * (yz + wx);

        dest.m20 = 2f * (xz + wy);
        dest.m21 = 2f * (yz - wx);
        dest.m22 = 1f - 2f * (xx + yy);
        return dest;
    }

    public Mat4 toMatrix() {
        return get(new Mat4());
    }
}
