package me.Padej_.soupapi.bedrock;

/**
 * A 4x4 matrix, column-major like OpenGL's.
 *
 * <p>Only here so the model's bounding box can be measured off the render thread: the posed bones
 * are walked with the same transforms the renderer pushes onto the GL stack, which is what lets the
 * first-person fit be computed once at load instead of guessed.
 */
public final class Mat4 {

    public final float[] m = new float[16];

    public Mat4() {
        identity();
    }

    public Mat4 identity() {
        for (int i = 0; i < 16; i++) m[i] = 0f;
        m[0] = m[5] = m[10] = m[15] = 1f;
        return this;
    }

    public Mat4 set(Mat4 other) {
        System.arraycopy(other.m, 0, m, 0, 16);
        return this;
    }

    /** this = this * other. */
    public Mat4 multiply(Mat4 other) {
        float[] a = m;
        float[] b = other.m;
        float[] out = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                out[c * 4 + r] = a[r] * b[c * 4]
                        + a[4 + r] * b[c * 4 + 1]
                        + a[8 + r] * b[c * 4 + 2]
                        + a[12 + r] * b[c * 4 + 3];
            }
        }
        System.arraycopy(out, 0, m, 0, 16);
        return this;
    }

    public Mat4 translate(float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
        m[15] += m[3] * x + m[7] * y + m[11] * z;
        return this;
    }

    public Mat4 scale(float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
        return this;
    }

    public Mat4 rotateDeg(float degrees, float x, float y, float z) {
        if (degrees == 0f) return this;
        double a = Math.toRadians(degrees);
        float c = (float) Math.cos(a);
        float s = (float) Math.sin(a);
        float t = 1f - c;
        Mat4 r = new Mat4();
        r.m[0] = t * x * x + c;
        r.m[1] = t * x * y + s * z;
        r.m[2] = t * x * z - s * y;
        r.m[4] = t * x * y - s * z;
        r.m[5] = t * y * y + c;
        r.m[6] = t * y * z + s * x;
        r.m[8] = t * x * z + s * y;
        r.m[9] = t * y * z - s * x;
        r.m[10] = t * z * z + c;
        return multiply(r);
    }

    /** Transforms a point, writing x/y/z into {@code out}. */
    public void transform(float x, float y, float z, float[] out) {
        out[0] = m[0] * x + m[4] * y + m[8] * z + m[12];
        out[1] = m[1] * x + m[5] * y + m[9] * z + m[13];
        out[2] = m[2] * x + m[6] * y + m[10] * z + m[14];
    }

    /** Transforms a direction (w = 0): the translation column is ignored. */
    public void transformDir(float x, float y, float z, float[] out) {
        out[0] = m[0] * x + m[4] * y + m[8] * z;
        out[1] = m[1] * x + m[5] * y + m[9] * z;
        out[2] = m[2] * x + m[6] * y + m[10] * z;
    }

    public Mat4 copy() {
        return new Mat4().set(this);
    }

    // ---- explicit axis rotations -----------------------------------------------------------
    // Copied byte-for-byte from the DeltaForce source's Mat4 so the Bedrock → Java convention
    // matches the pack's own renderer exactly; rotateDeg above stays for other callers.

    private final float[] rot = new float[16];

    /** this = this * Rx(deg). */
    public Mat4 rotateX(float deg) {
        if (deg == 0f) return this;
        double a = Math.toRadians(deg);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        identity(rot);
        rot[5] = c; rot[6] = s; rot[9] = -s; rot[10] = c;
        return multiply(rot);
    }

    public Mat4 rotateY(float deg) {
        if (deg == 0f) return this;
        double a = Math.toRadians(deg);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        identity(rot);
        rot[0] = c; rot[2] = -s; rot[8] = s; rot[10] = c;
        return multiply(rot);
    }

    public Mat4 rotateZ(float deg) {
        if (deg == 0f) return this;
        double a = Math.toRadians(deg);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        identity(rot);
        rot[0] = c; rot[1] = s; rot[4] = -s; rot[5] = c;
        return multiply(rot);
    }

    /** Bedrock / GeckoLib order: matrix = this * Rz * Ry * Rx. */
    public Mat4 rotateZYX(float rx, float ry, float rz) {
        rotateZ(rz);
        rotateY(ry);
        rotateX(rx);
        return this;
    }

    /** this = this * b, where b is a raw column-major 4x4. */
    public Mat4 multiply(float[] b) {
        float[] a = m;
        float[] out = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                out[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1]
                        + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
            }
        }
        System.arraycopy(out, 0, m, 0, 16);
        return this;
    }

    private static void identity(float[] a) {
        for (int i = 0; i < 16; i++) a[i] = 0f;
        a[0] = a[5] = a[10] = a[15] = 1f;
    }

    /** Writes the matrix into a float buffer in GL (column-major) order. */
    public void store(java.nio.FloatBuffer buf) {
        buf.clear();
        buf.put(m);
        buf.flip();
    }

    /** General 4x4 inverse. Returns false if singular (the matrix is then left unchanged). */
    public boolean invert() {
        float[] a = m;
        float[] inv = new float[16];
        inv[0] = a[5]*a[10]*a[15] - a[5]*a[11]*a[14] - a[9]*a[6]*a[15] + a[9]*a[7]*a[14] + a[13]*a[6]*a[11] - a[13]*a[7]*a[10];
        inv[4] = -a[4]*a[10]*a[15] + a[4]*a[11]*a[14] + a[8]*a[6]*a[15] - a[8]*a[7]*a[14] - a[12]*a[6]*a[11] + a[12]*a[7]*a[10];
        inv[8] = a[4]*a[9]*a[15] - a[4]*a[11]*a[13] - a[8]*a[5]*a[15] + a[8]*a[7]*a[13] + a[12]*a[5]*a[11] - a[12]*a[7]*a[9];
        inv[12] = -a[4]*a[9]*a[14] + a[4]*a[10]*a[13] + a[8]*a[5]*a[14] - a[8]*a[6]*a[13] - a[12]*a[5]*a[10] + a[12]*a[6]*a[9];
        inv[1] = -a[1]*a[10]*a[15] + a[1]*a[11]*a[14] + a[9]*a[2]*a[15] - a[9]*a[3]*a[14] - a[13]*a[2]*a[11] + a[13]*a[3]*a[10];
        inv[5] = a[0]*a[10]*a[15] - a[0]*a[11]*a[14] - a[8]*a[2]*a[15] + a[8]*a[3]*a[14] + a[12]*a[2]*a[11] - a[12]*a[3]*a[10];
        inv[9] = -a[0]*a[9]*a[15] + a[0]*a[11]*a[13] + a[8]*a[1]*a[15] - a[8]*a[3]*a[13] - a[12]*a[1]*a[11] + a[12]*a[3]*a[9];
        inv[13] = a[0]*a[9]*a[14] - a[0]*a[10]*a[13] - a[8]*a[1]*a[14] + a[8]*a[2]*a[13] + a[12]*a[1]*a[10] - a[12]*a[2]*a[9];
        inv[2] = a[1]*a[6]*a[15] - a[1]*a[7]*a[14] - a[5]*a[2]*a[15] + a[5]*a[3]*a[14] + a[13]*a[2]*a[7] - a[13]*a[3]*a[6];
        inv[6] = -a[0]*a[6]*a[15] + a[0]*a[7]*a[14] + a[4]*a[2]*a[15] - a[4]*a[3]*a[14] - a[12]*a[2]*a[7] + a[12]*a[3]*a[6];
        inv[10] = a[0]*a[5]*a[15] - a[0]*a[7]*a[13] - a[4]*a[1]*a[15] + a[4]*a[3]*a[13] + a[12]*a[1]*a[7] - a[12]*a[3]*a[5];
        inv[14] = -a[0]*a[5]*a[14] + a[0]*a[6]*a[13] + a[4]*a[1]*a[14] - a[4]*a[2]*a[13] - a[12]*a[1]*a[6] + a[12]*a[2]*a[5];
        inv[3] = -a[1]*a[6]*a[11] + a[1]*a[7]*a[10] + a[5]*a[2]*a[11] - a[5]*a[3]*a[10] - a[9]*a[2]*a[7] + a[9]*a[3]*a[6];
        inv[7] = a[0]*a[6]*a[11] - a[0]*a[7]*a[10] - a[4]*a[2]*a[11] + a[4]*a[3]*a[10] + a[8]*a[2]*a[7] - a[8]*a[3]*a[6];
        inv[11] = -a[0]*a[5]*a[11] + a[0]*a[7]*a[9] + a[4]*a[1]*a[11] - a[4]*a[3]*a[9] - a[8]*a[1]*a[7] + a[8]*a[3]*a[5];
        inv[15] = a[0]*a[5]*a[10] - a[0]*a[6]*a[9] - a[4]*a[1]*a[10] + a[4]*a[2]*a[9] + a[8]*a[1]*a[6] - a[8]*a[2]*a[5];
        float det = a[0]*inv[0] + a[1]*inv[4] + a[2]*inv[8] + a[3]*inv[12];
        if (Math.abs(det) < 1e-12f) return false;
        det = 1f / det;
        for (int i = 0; i < 16; i++) m[i] = inv[i] * det;
        return true;
    }
}
