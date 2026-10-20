package me.Padej_.soupapi.compat;

/**
 * An axis-angle rotation, standing in for {@code org.joml.Quaternionf} as produced by
 * {@link RotationAxis} in 1.21.4.
 *
 * <p>Every rotation the mod builds is a single-axis rotation, so an axis-angle pair reproduces them
 * exactly while avoiding quaternion round-tripping. {@link #apply(Mat4)} composes the rotation onto
 * a matrix the same way {@code MatrixStack.multiply(Quaternionf)} does.
 */
public final class Quat {
    public final float angleRad;
    public final float x, y, z;

    public Quat(float angleRad, float x, float y, float z) {
        this.angleRad = angleRad;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void apply(Mat4 target) {
        target.rotate(angleRad, x, y, z);
    }
}
