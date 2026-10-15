package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.util.math.RotationAxis}, which does not exist in 1.8.9.
 *
 * <p>Same six constants and the same {@code rotationDegrees}/{@code rotation} factory methods, so
 * call sites port across unchanged.
 */
public final class RotationAxis {
    public static final RotationAxis POSITIVE_X = new RotationAxis(1f, 0f, 0f);
    public static final RotationAxis NEGATIVE_X = new RotationAxis(-1f, 0f, 0f);
    public static final RotationAxis POSITIVE_Y = new RotationAxis(0f, 1f, 0f);
    public static final RotationAxis NEGATIVE_Y = new RotationAxis(0f, -1f, 0f);
    public static final RotationAxis POSITIVE_Z = new RotationAxis(0f, 0f, 1f);
    public static final RotationAxis NEGATIVE_Z = new RotationAxis(0f, 0f, -1f);

    private final float x, y, z;

    private RotationAxis(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quat rotationDegrees(float degrees) {
        return new Quat((float) Math.toRadians(degrees), x, y, z);
    }

    public Quat rotation(float radians) {
        return new Quat(radians, x, y, z);
    }
}
