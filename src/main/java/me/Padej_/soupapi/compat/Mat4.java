package me.Padej_.soupapi.compat;

/**
 * A minimal column-major 4x4 float matrix.
 *
 * <p>1.8.9 predates both {@code org.joml} and {@code net.minecraft.util.math.Matrix4f}, so the
 * 1.21.4 sources' use of {@link org.joml.Matrix4f} has nowhere to land. This class provides the
 * subset of the JOML API the mod actually used, with the same column-major element order and the
 * same "right-multiply" composition semantics, so the ported render math is unchanged.
 *
 * <p>Element naming follows JOML: {@code mXY} is column X, row Y. In memory the elements are laid
 * out so that {@code m00 m01 m02 m03} is the first column, which is also the order OpenGL's
 * {@code glMultMatrix} expects, letting {@link #writeTo(java.nio.FloatBuffer)} be a straight copy.
 *
 * <p>Vertices are transformed on the CPU ({@link #transformX}/{@link #transformY}/{@link
 * #transformZ}) rather than by pushing matrices to the GPU. That mirrors what 1.21.4 does when it
 * calls {@code buffer.vertex(matrix, x, y, z)}, and it keeps the GL matrix stack untouched, which
 * is friendlier to gl4es on PojavLauncher: fewer driver-side matrix ops and no reliance on the
 * fixed-function stack depth beyond what vanilla already uses.
 */
public final class Mat4 {
    public float m00, m01, m02, m03;
    public float m10, m11, m12, m13;
    public float m20, m21, m22, m23;
    public float m30, m31, m32, m33;

    public Mat4() {
        identity();
    }

    public Mat4(Mat4 other) {
        set(other);
    }

    public Mat4 identity() {
        m00 = 1f; m01 = 0f; m02 = 0f; m03 = 0f;
        m10 = 0f; m11 = 1f; m12 = 0f; m13 = 0f;
        m20 = 0f; m21 = 0f; m22 = 1f; m23 = 0f;
        m30 = 0f; m31 = 0f; m32 = 0f; m33 = 1f;
        return this;
    }

    public Mat4 set(Mat4 o) {
        m00 = o.m00; m01 = o.m01; m02 = o.m02; m03 = o.m03;
        m10 = o.m10; m11 = o.m11; m12 = o.m12; m13 = o.m13;
        m20 = o.m20; m21 = o.m21; m22 = o.m22; m23 = o.m23;
        m30 = o.m30; m31 = o.m31; m32 = o.m32; m33 = o.m33;
        return this;
    }

    public Mat4 copy() {
        return new Mat4(this);
    }

    /** {@code this = this * other}, matching JOML's {@code mul}. */
    public Mat4 mul(Mat4 r) {
        float n00 = m00 * r.m00 + m10 * r.m01 + m20 * r.m02 + m30 * r.m03;
        float n01 = m01 * r.m00 + m11 * r.m01 + m21 * r.m02 + m31 * r.m03;
        float n02 = m02 * r.m00 + m12 * r.m01 + m22 * r.m02 + m32 * r.m03;
        float n03 = m03 * r.m00 + m13 * r.m01 + m23 * r.m02 + m33 * r.m03;

        float n10 = m00 * r.m10 + m10 * r.m11 + m20 * r.m12 + m30 * r.m13;
        float n11 = m01 * r.m10 + m11 * r.m11 + m21 * r.m12 + m31 * r.m13;
        float n12 = m02 * r.m10 + m12 * r.m11 + m22 * r.m12 + m32 * r.m13;
        float n13 = m03 * r.m10 + m13 * r.m11 + m23 * r.m12 + m33 * r.m13;

        float n20 = m00 * r.m20 + m10 * r.m21 + m20 * r.m22 + m30 * r.m23;
        float n21 = m01 * r.m20 + m11 * r.m21 + m21 * r.m22 + m31 * r.m23;
        float n22 = m02 * r.m20 + m12 * r.m21 + m22 * r.m22 + m32 * r.m23;
        float n23 = m03 * r.m20 + m13 * r.m21 + m23 * r.m22 + m33 * r.m23;

        float n30 = m00 * r.m30 + m10 * r.m31 + m20 * r.m32 + m30 * r.m33;
        float n31 = m01 * r.m30 + m11 * r.m31 + m21 * r.m32 + m31 * r.m33;
        float n32 = m02 * r.m30 + m12 * r.m31 + m22 * r.m32 + m32 * r.m33;
        float n33 = m03 * r.m30 + m13 * r.m31 + m23 * r.m32 + m33 * r.m33;

        m00 = n00; m01 = n01; m02 = n02; m03 = n03;
        m10 = n10; m11 = n11; m12 = n12; m13 = n13;
        m20 = n20; m21 = n21; m22 = n22; m23 = n23;
        m30 = n30; m31 = n31; m32 = n32; m33 = n33;
        return this;
    }

    public Mat4 translate(float x, float y, float z) {
        m30 += m00 * x + m10 * y + m20 * z;
        m31 += m01 * x + m11 * y + m21 * z;
        m32 += m02 * x + m12 * y + m22 * z;
        m33 += m03 * x + m13 * y + m23 * z;
        return this;
    }

    public Mat4 scale(float x, float y, float z) {
        m00 *= x; m01 *= x; m02 *= x; m03 *= x;
        m10 *= y; m11 *= y; m12 *= y; m13 *= y;
        m20 *= z; m21 *= z; m22 *= z; m23 *= z;
        return this;
    }

    /** Rotation about an arbitrary normalised axis, angle in radians. */
    public Mat4 rotate(float angleRad, float ax, float ay, float az) {
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len < 1.0e-7f) return this;
        ax /= len; ay /= len; az /= len;

        float s = (float) Math.sin(angleRad);
        float c = (float) Math.cos(angleRad);
        float omc = 1f - c;

        float r00 = ax * ax * omc + c;
        float r01 = ay * ax * omc + az * s;
        float r02 = az * ax * omc - ay * s;
        float r10 = ax * ay * omc - az * s;
        float r11 = ay * ay * omc + c;
        float r12 = az * ay * omc + ax * s;
        float r20 = ax * az * omc + ay * s;
        float r21 = ay * az * omc - ax * s;
        float r22 = az * az * omc + c;

        float n00 = m00 * r00 + m10 * r01 + m20 * r02;
        float n01 = m01 * r00 + m11 * r01 + m21 * r02;
        float n02 = m02 * r00 + m12 * r01 + m22 * r02;
        float n03 = m03 * r00 + m13 * r01 + m23 * r02;

        float n10 = m00 * r10 + m10 * r11 + m20 * r12;
        float n11 = m01 * r10 + m11 * r11 + m21 * r12;
        float n12 = m02 * r10 + m12 * r11 + m22 * r12;
        float n13 = m03 * r10 + m13 * r11 + m23 * r12;

        float n20 = m00 * r20 + m10 * r21 + m20 * r22;
        float n21 = m01 * r20 + m11 * r21 + m21 * r22;
        float n22 = m02 * r20 + m12 * r21 + m22 * r22;
        float n23 = m03 * r20 + m13 * r21 + m23 * r22;

        m00 = n00; m01 = n01; m02 = n02; m03 = n03;
        m10 = n10; m11 = n11; m12 = n12; m13 = n13;
        m20 = n20; m21 = n21; m22 = n22; m23 = n23;
        return this;
    }

    public Mat4 rotateX(float angleRad) {
        return rotate(angleRad, 1f, 0f, 0f);
    }

    public Mat4 rotateY(float angleRad) {
        return rotate(angleRad, 0f, 1f, 0f);
    }

    public Mat4 rotateZ(float angleRad) {
        return rotate(angleRad, 0f, 0f, 1f);
    }

    public float transformX(float x, float y, float z) {
        return m00 * x + m10 * y + m20 * z + m30;
    }

    public float transformY(float x, float y, float z) {
        return m01 * x + m11 * y + m21 * z + m31;
    }

    public float transformZ(float x, float y, float z) {
        return m02 * x + m12 * y + m22 * z + m32;
    }

    public float transformW(float x, float y, float z) {
        return m03 * x + m13 * y + m23 * z + m33;
    }

    /** Builds a right-handed perspective projection, matching JOML's {@code setPerspective}. */
    public Mat4 setPerspective(float fovyRad, float aspect, float zNear, float zFar) {
        identity();
        float h = (float) Math.tan(fovyRad * 0.5f);
        m00 = 1f / (h * aspect);
        m11 = 1f / h;
        m22 = (zFar + zNear) / (zNear - zFar);
        m23 = -1f;
        m32 = 2f * zFar * zNear / (zNear - zFar);
        m33 = 0f;
        return this;
    }

    /** Builds an off-centre orthographic projection, matching JOML's {@code setOrtho}. */
    public Mat4 setOrtho(float left, float right, float bottom, float top, float zNear, float zFar) {
        identity();
        m00 = 2f / (right - left);
        m11 = 2f / (top - bottom);
        m22 = -2f / (zFar - zNear);
        m30 = -(right + left) / (right - left);
        m31 = -(top + bottom) / (top - bottom);
        m32 = -(zFar + zNear) / (zFar - zNear);
        return this;
    }

    /** Writes the 16 elements in OpenGL's expected column-major order. */
    public void writeTo(java.nio.FloatBuffer buf) {
        buf.put(m00).put(m01).put(m02).put(m03);
        buf.put(m10).put(m11).put(m12).put(m13);
        buf.put(m20).put(m21).put(m22).put(m23);
        buf.put(m30).put(m31).put(m32).put(m33);
    }

    public Mat4 readFrom(java.nio.FloatBuffer buf) {
        m00 = buf.get(); m01 = buf.get(); m02 = buf.get(); m03 = buf.get();
        m10 = buf.get(); m11 = buf.get(); m12 = buf.get(); m13 = buf.get();
        m20 = buf.get(); m21 = buf.get(); m22 = buf.get(); m23 = buf.get();
        m30 = buf.get(); m31 = buf.get(); m32 = buf.get(); m33 = buf.get();
        return this;
    }

    /**
     * Inverts the matrix in place, returning {@code false} (leaving it untouched) if it is singular.
     * Used by the world-to-screen projection in {@code MathUtility}.
     */
    public boolean invert() {
        float a = m00 * m11 - m01 * m10;
        float b = m00 * m12 - m02 * m10;
        float c = m00 * m13 - m03 * m10;
        float d = m01 * m12 - m02 * m11;
        float e = m01 * m13 - m03 * m11;
        float f = m02 * m13 - m03 * m12;
        float g = m20 * m31 - m21 * m30;
        float h = m20 * m32 - m22 * m30;
        float i = m20 * m33 - m23 * m30;
        float j = m21 * m32 - m22 * m31;
        float k = m21 * m33 - m23 * m31;
        float l = m22 * m33 - m23 * m32;

        float det = a * l - b * k + c * j + d * i - e * h + f * g;
        if (Math.abs(det) < 1.0e-12f) return false;
        float s = 1f / det;

        float n00 = ( m11 * l - m12 * k + m13 * j) * s;
        float n01 = (-m01 * l + m02 * k - m03 * j) * s;
        float n02 = ( m31 * f - m32 * e + m33 * d) * s;
        float n03 = (-m21 * f + m22 * e - m23 * d) * s;
        float n10 = (-m10 * l + m12 * i - m13 * h) * s;
        float n11 = ( m00 * l - m02 * i + m03 * h) * s;
        float n12 = (-m30 * f + m32 * c - m33 * b) * s;
        float n13 = ( m20 * f - m22 * c + m23 * b) * s;
        float n20 = ( m10 * k - m11 * i + m13 * g) * s;
        float n21 = (-m00 * k + m01 * i - m03 * g) * s;
        float n22 = ( m30 * e - m31 * c + m33 * a) * s;
        float n23 = (-m20 * e + m21 * c - m23 * a) * s;
        float n30 = (-m10 * j + m11 * h - m12 * g) * s;
        float n31 = ( m00 * j - m01 * h + m02 * g) * s;
        float n32 = (-m30 * d + m31 * b - m32 * a) * s;
        float n33 = ( m20 * d - m21 * b + m22 * a) * s;

        m00 = n00; m01 = n01; m02 = n02; m03 = n03;
        m10 = n10; m11 = n11; m12 = n12; m13 = n13;
        m20 = n20; m21 = n21; m22 = n22; m23 = n23;
        m30 = n30; m31 = n31; m32 = n32; m33 = n33;
        return true;
    }

    @Override
    public String toString() {
        return "Mat4[" + m00 + " " + m10 + " " + m20 + " " + m30 + " / "
                + m01 + " " + m11 + " " + m21 + " " + m31 + " / "
                + m02 + " " + m12 + " " + m22 + " " + m32 + " / "
                + m03 + " " + m13 + " " + m23 + " " + m33 + "]";
    }
}
