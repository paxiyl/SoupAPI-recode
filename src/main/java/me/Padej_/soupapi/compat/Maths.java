package me.Padej_.soupapi.compat;

/**
 * Math helpers that 1.8.9's {@code MathHelper} does not have.
 *
 * <p>{@code MathHelper.lerp} arrived in 1.14 and the ported code calls it 39 times, mostly to
 * interpolate entity positions between ticks. Argument order matches vanilla's
 * ({@code delta} first), so call sites only change the receiver.
 */
public final class Maths {

    private Maths() {
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static float lerpAngleDegrees(float delta, float start, float end) {
        return start + delta * net.minecraft.util.MathHelper.wrapAngleTo180_float(end - start);
    }

    /**
     * MCP suffixes its clamps by type ({@code clamp_int}, {@code clamp_float},
     * {@code clamp_double}), so these overloads let the ported call sites keep writing
     * {@code clamp(...)} and let javac pick.
     */
    public static int clamp(int value, int min, int max) {
        return net.minecraft.util.MathHelper.clamp_int(value, min, max);
    }

    public static float clamp(float value, float min, float max) {
        return net.minecraft.util.MathHelper.clamp_float(value, min, max);
    }

    public static double clamp(double value, double min, double max) {
        return net.minecraft.util.MathHelper.clamp_double(value, min, max);
    }

    /** Likewise for angle wrapping, which MCP suffixes by type. */
    public static float wrapDegrees(float degrees) {
        return net.minecraft.util.MathHelper.wrapAngleTo180_float(degrees);
    }

    public static double wrapDegrees(double degrees) {
        return net.minecraft.util.MathHelper.wrapAngleTo180_double(degrees);
    }

    /** Inverse lerp, clamped to [0,1]. */
    public static float inverseLerp(float value, float start, float end) {
        if (end == start) return 0f;
        float t = (value - start) / (end - start);
        return t < 0f ? 0f : (t > 1f ? 1f : t);
    }
}
