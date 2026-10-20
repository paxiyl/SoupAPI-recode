package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.SoupAPI;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.RenderWithAnimatedColor;

import java.awt.*;
import java.util.Random;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

public class Palette {
    private static final Random random = new Random();

    /**
     * The theme named in the settings, or {@link Theme#CUSTOM}.
     *
     * <p>Only the settings screen cares which one it is. A theme no longer answers for the colours
     * here — it writes them into the four pickers when it is chosen — so that editing one works
     * instead of being quietly overruled.
     */
    public static Theme getTheme() {
        Theme theme = SoupAPI.configHolder.get().themeStyle;
        return theme == null ? Theme.CUSTOM : theme;
    }

    public static int getRawColor1() {
        return SoupAPI.configHolder.get().c1;
    }

    public static int getRawColor2() {
        return SoupAPI.configHolder.get().c2;
    }

    public static int getRawColor3() {
        return SoupAPI.configHolder.get().c3;
    }

    public static int getRawColor4() {
        return SoupAPI.configHolder.get().c4;
    }

    public static int getColorsCount() {
        switch (getStyle()) {
            case SOLO:
                return 1;
            case DUO:
                return 2;
            case TRIO:
                return 3;
            case QUARTET:
                return 4;
        }
        throw new IllegalStateException("Unexpected value: " + getStyle());
    }

    public static Color getInterpolatedPaletteColor(float progress) {
        progress = progress % 1f;
        if (progress < 0f) progress += 1f;

        Palette.ColorsStyle style = SoupAPI.configHolder.get().paletteStyle;
        int c1 = getRawColor1();
        int c2 = getRawColor2();
        int c3 = getRawColor3();
        int c4 = getRawColor4();

        switch (style) {
            case SOLO:
                return new Color(c1);
            case DUO:
                return OklabUtils.interpolate(new Color(c1), new Color(c2), progress);
            case TRIO:
                if (progress < 0.5f) {
                    return OklabUtils.interpolate(new Color(c1), new Color(c2), progress * 2f);
                } else {
                    return OklabUtils.interpolate(new Color(c2), new Color(c3), (progress - 0.5f) * 2f);
                }
            case QUARTET:
                if (progress < 1f / 3f) {
                    return OklabUtils.interpolate(new Color(c1), new Color(c2), progress * 3f);
                } else if (progress < 2f / 3f) {
                    return OklabUtils.interpolate(new Color(c2), new Color(c3), (progress - 1f / 3f) * 3f);
                } else {
                    return OklabUtils.interpolate(new Color(c3), new Color(c4), (progress - 2f / 3f) * 3f);
                }
            default:
                return new Color(c1); // fallback
        }
    }

    public static Color getBackColor() {
        int alpha = (int) (255 * (CONFIG.backAlpha / 100f));
        int back = CONFIG.backColor;
        return Render2D.injectAlpha(new Color(back), alpha);
    }

    public static ColorsStyle getStyle() {
        return SoupAPI.configHolder.get().paletteStyle;
    }

    /**
     * The palette colour at {@code position} for whatever is drawing right now.
     *
     * <p>Every one of the mod's ~130 colour reads goes through here, which is why the RGB modes live
     * here and not in each module: the event bridge scopes each module to its {@link Element}, and
     * that element's switch decides whether its colours move. Positions outside 0..1 wrap instead
     * of extrapolating past the last colour, which callers relied on without it being true.
     */
    public static Color getColor(float position) {
        return getColor(current, position);
    }

    /** The palette colour for a specific element, regardless of the current scope. */
    public static Color getColor(Element element, float position) {
        // 1.0 is "the last colour" to the callers that pass it, so only out-of-range values wrap.
        float p = position >= 0f && position <= 1f ? position : wrap(position);
        RgbMode mode = mode();
        if (mode == RgbMode.STATIC || !animates(element)) {
            return staticColor(p);
        }
        float t = phase(element);
        switch (mode) {
            case FLOW:
                return cyclicColor(wrap(p + t * 0.25f));
            case RAINBOW: {
                float spread = CONFIG.rgbSpread / 100f;
                float hue = wrap(p * spread + t * 0.25f);
                float sat = clamp01(CONFIG.rgbSaturation / 100f);
                float bri = clamp01(CONFIG.rgbBrightness / 100f);
                return new Color(Color.HSBtoRGB(hue, sat, bri));
            }
            case PULSE: {
                Color base = staticColor(p);
                float wave = 0.5f + 0.5f * (float) Math.sin((t + p * CONFIG.rgbSpread / 100f) * Math.PI * 2.0);
                float k = 0.45f + 0.55f * wave;
                return new Color(clamp255(base.getRed() * k), clamp255(base.getGreen() * k), clamp255(base.getBlue() * k));
            }
            case SHIFT: {
                // The palette's own hues, rotated around the colour wheel together.
                Color base = staticColor(p);
                float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
                return new Color(Color.HSBtoRGB(wrap(hsb[0] + t * 0.25f), hsb[1], hsb[2]));
            }
            default:
                return staticColor(p);
        }
    }

    /** The fixed (non-animated) palette, exactly as it always was for in-range positions. */
    public static Color staticColor(float position) {
        ColorsStyle style = getStyle();
        switch (style) {
            case SOLO:
                return new Color(getRawColor1());
            case DUO:
                return OklabUtils.interpolate(new Color(getRawColor1()), new Color(getRawColor2()), position);
            case TRIO:
                return interpolate3(position, new Color(getRawColor1()), new Color(getRawColor2()), new Color(getRawColor3()));
            case QUARTET:
                return interpolate4(position, new Color(getRawColor1()), new Color(getRawColor2()), new Color(getRawColor3()), new Color(getRawColor4()));
        }
        throw new IllegalStateException("Unexpected value: " + style);
    }

    /**
     * The palette as a loop, last colour back to the first, so a colour flowing along it never
     * jumps when the position wraps.
     */
    private static Color cyclicColor(float p) {
        int n = getColorsCount();
        if (n == 1) {
            // One colour has nothing to flow between; let it breathe instead of standing still.
            Color c = new Color(getRawColor1());
            float k = 0.7f + 0.3f * (float) Math.sin(p * Math.PI * 2.0);
            return new Color(clamp255(c.getRed() * k), clamp255(c.getGreen() * k), clamp255(c.getBlue() * k));
        }
        int[] raw = {getRawColor1(), getRawColor2(), getRawColor3(), getRawColor4()};
        float scaled = p * n;
        int i = Math.min(n - 1, (int) scaled);
        float f = scaled - i;
        return OklabUtils.interpolate(new Color(raw[i]), new Color(raw[(i + 1) % n]), f);
    }

    // ---- RGB animation ------------------------------------------------------------------------

    /** How the palette moves over time. */
    public enum RgbMode {
        /** Fixed colours: what the pickers say, nothing moves. */
        STATIC,
        /** The palette's own colours flow along the element, looping. */
        FLOW,
        /** A full hue cycle, with its own saturation and brightness. */
        RAINBOW,
        /** The palette breathes brighter and darker. */
        PULSE,
        /** The palette's hues rotate around the colour wheel together. */
        SHIFT
    }

    /** What is drawing, so each part of the mod can have RGB on or off independently. */
    public enum Element {
        GENERAL, GUI, HUD, CROSSHAIR, WEAPON, TRAILS, PARTICLES, SKY, AMBIENCE
    }

    private static Element current = Element.GENERAL;

    /** Sets the element being drawn and returns the previous one, for the caller to restore. */
    public static Element scope(Element element) {
        Element previous = current;
        current = element == null ? Element.GENERAL : element;
        return previous;
    }

    public static Element currentElement() {
        return current;
    }

    public static RgbMode mode() {
        RgbMode mode = CONFIG.rgbMode;
        return mode == null ? RgbMode.STATIC : mode;
    }

    /** Whether this element's colours move; an element switched off keeps the fixed palette. */
    public static boolean animates(Element element) {
        if (mode() == RgbMode.STATIC) return false;
        switch (element == null ? Element.GENERAL : element) {
            case GUI: return CONFIG.rgbGui;
            case HUD: return CONFIG.rgbHud;
            case CROSSHAIR: return CONFIG.rgbCrosshair;
            case WEAPON: return CONFIG.rgbWeapon;
            case TRAILS: return CONFIG.rgbTrails;
            case PARTICLES: return CONFIG.rgbParticles;
            case SKY: return CONFIG.rgbSky;
            case AMBIENCE: return CONFIG.rgbAmbience;
            default: return true;
        }
    }

    /**
     * Where an element is in the cycle, in cycles. One clock for everything; with Sync off each
     * element is offset by a fixed amount so they read as related but not identical.
     */
    public static float phase(Element element) {
        float speed = Math.max(0f, CONFIG.rgbSpeed / 100f);
        float t = FxClock.seconds() * speed;
        if (CONFIG.rgbReverse) t = -t;
        if (!CONFIG.rgbSync && element != null) t += element.ordinal() * 0.137f;
        return t;
    }

    private static float wrap(float v) {
        v = v % 1f;
        return v < 0f ? v + 1f : v;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static Color interpolate3(float t, Color c1, Color c2, Color c3) {
        if (t < 0.5f) {
            return OklabUtils.interpolate(c1, c2, t * 2f);
        } else {
            return OklabUtils.interpolate(c2, c3, (t - 0.5f) * 2f);
        }
    }

    private static Color interpolate4(float t, Color c1, Color c2, Color c3, Color c4) {
        if (t < 1f / 3f) {
            return OklabUtils.interpolate(c1, c2, t * 3f);
        } else if (t < 2f / 3f) {
            return OklabUtils.interpolate(c2, c3, (t - 1f / 3f) * 3f);
        } else {
            return OklabUtils.interpolate(c3, c4, (t - 2f / 3f) * 3f);
        }
    }

    public static Color getRandomColor() {
        int steps = 20;
        int index = random.nextInt(steps);
        float position = index / (float) (steps - 1); // от 0 до 1 включительно

        return getColor(position);
    }

    public static int getTextColor() {
        return CONFIG.textColor + 0xFF000000;
    }

    public enum ColorsStyle {
        SOLO, DUO, TRIO, QUARTET
    }
}


