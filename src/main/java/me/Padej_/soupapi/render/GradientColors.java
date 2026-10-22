package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.Maths;
import me.Padej_.soupapi.utils.Palette;

import java.awt.Color;

/**
 * The four corner colours every gradient in the mod is drawn from.
 *
 * <p>The theme is four colours; a gradient wants one per corner; and the whole HUD is meant to
 * shimmer together rather than each panel cycling on its own clock. So the cycle lives here, once:
 * every corner walks to the next theme colour, all four in step, and anything that wants a gradient
 * — potion bars, trails, the mouse-move dot, the watermark, ambient particles, the 3D target render
 * — reads the corners as they stand this frame.
 *
 * <p>This used to live inside the target HUD, which is why it ran even when that was turned off:
 * everything else depended on it. The target HUD is gone; the colours it carried are not.
 */
public final class GradientColors {

    private GradientColors() {
    }

    /** How far through one full rotation of the four colours we are, in [0, 1). */
    public static float progress = 0f;
    /** The same cycle at half speed, for health bars that should drift rather than pulse. */
    public static float healthProgress = 0f;
    /** A free-running counter for anything that wants to wobble; advanced with the cycle. */
    public static float ticks = 0f;

    public static Color topLeft = new Color(0xFFFFFFFF);
    public static Color topRight = new Color(0xFFFFFFFF);
    public static Color bottomLeft = new Color(0xFFFFFFFF);
    public static Color bottomRight = new Color(0xFFFFFFFF);

    private static final float SPEED = 0.01f;
    private static long lastUpdate = System.currentTimeMillis();

    /** Advances the cycle by real elapsed time and recolours the corners. Called once a frame. */
    public static void tick() {
        long now = System.currentTimeMillis();
        float elapsed = Math.min((now - lastUpdate) / 1000f, 0.1f);
        lastUpdate = now;
        float steps = elapsed * 60f;               // the cycle was authored per 60Hz frame
        progress = (progress + steps * SPEED) % 1.0f;
        healthProgress = (healthProgress + steps * SPEED / 2f) % 1.0f;
        ticks += 0.1f * steps;
        apply(progress);
    }

    /** The real elapsed frame time, normalised to 60Hz steps, for callers that animate with us. */
    public static float delta() {
        return Math.min((System.currentTimeMillis() - lastUpdate) / 1000f, 0.1f) * 60f;
    }

    private static void apply(float cycle) {
        Color c1 = Palette.getColor(0f);
        Color c2 = Palette.getColor(0.33f);
        Color c3 = Palette.getColor(0.66f);
        Color c4 = Palette.getColor(1f);

        // Four quarter-turns: in each one every corner walks to the colour of the next corner
        // round, so after a whole cycle the gradient has rotated once and is back where it began.
        float t = cycle % 1.0f;
        if (t < 0.25f) {
            float p = t / 0.25f;
            topLeft = lerp(c1, c2, p);
            topRight = lerp(c2, c3, p);
            bottomRight = lerp(c3, c4, p);
            bottomLeft = lerp(c4, c1, p);
        } else if (t < 0.5f) {
            float p = (t - 0.25f) / 0.25f;
            topLeft = lerp(c2, c3, p);
            topRight = lerp(c3, c4, p);
            bottomRight = lerp(c4, c1, p);
            bottomLeft = lerp(c1, c2, p);
        } else if (t < 0.75f) {
            float p = (t - 0.5f) / 0.25f;
            topLeft = lerp(c3, c4, p);
            topRight = lerp(c4, c1, p);
            bottomRight = lerp(c1, c2, p);
            bottomLeft = lerp(c2, c3, p);
        } else {
            float p = (t - 0.75f) / 0.25f;
            topLeft = lerp(c4, c1, p);
            topRight = lerp(c1, c2, p);
            bottomRight = lerp(c2, c3, p);
            bottomLeft = lerp(c3, c4, p);
        }
    }

    private static Color lerp(Color start, Color end, float progress) {
        return new Color(
                Math.round(Maths.lerp(progress, start.getRed(), end.getRed())),
                Math.round(Maths.lerp(progress, start.getGreen(), end.getGreen())),
                Math.round(Maths.lerp(progress, start.getBlue(), end.getBlue())),
                Math.round(Maths.lerp(progress, start.getAlpha(), end.getAlpha())));
    }
}
