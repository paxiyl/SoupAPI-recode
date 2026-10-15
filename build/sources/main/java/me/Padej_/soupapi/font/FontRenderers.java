package me.Padej_.soupapi.font;

import me.Padej_.soupapi.compat.Caps;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.SoupAPI;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

/**
 * The mod's font renderers.
 *
 * <p>Initialisation is lazy rather than eager. The 1.21.4 version built these from a mixin on
 * {@code Minecraft.<init>}, but on 1.8.9 the constructor runs before game options exist, and
 * constructing a renderer reads the GUI scale — so building them there would fail. Instead
 * {@link #ensureLoaded()} is called from the render and tick hooks, which only run once the client
 * is fully up, and is a cheap boolean check after the first call.
 *
 * <p>If AWT cannot rasterise glyphs — see {@link Caps#canRasteriseAwtFonts()}, which is a real
 * possibility on PojavLauncher — every renderer falls back to vanilla's bitmap font instead of the
 * mod losing all of its text.
 */
public class FontRenderers {
    public static FontRenderer modules;
    public static FontRenderer sf_bold;
    public static FontRenderer sf_bold_17;
    public static FontRenderer sf_bold_12;
    public static FontRenderer sf_bold_mini;
    public static FontRenderer sf_medium;
    public static FontRenderer minecraft;

    private static boolean loaded;
    private static boolean usingFallback;

    /** True when the renderers are backed by vanilla's font rather than the bundled TTFs. */
    public static boolean isUsingFallback() {
        return usingFallback;
    }

    /** Builds the renderers on first call; subsequent calls return immediately. */
    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        if (!Caps.canRasteriseAwtFonts()) {
            useFallback("AWT font rasterisation is unavailable");
            return;
        }

        try {
            sf_bold = create(16f, "sf_bold");
            sf_bold_17 = create(17f, "sf_bold");
            sf_bold_12 = create(12f, "sf_bold");
            sf_bold_mini = create(14f, "sf_bold");
            sf_medium = create(16f, "sf_medium");
            modules = sf_bold;
            minecraft = sf_bold;
        } catch (Throwable t) {
            SoupLog.error("Could not load the bundled fonts", t);
            useFallback("the bundled fonts could not be loaded");
        }
    }

    private static void useFallback(String reason) {
        usingFallback = true;
        SoupLog.warn("Using the vanilla font renderer because " + reason);
        sf_bold = FontRenderer.vanillaBacked(8f);
        sf_bold_17 = FontRenderer.vanillaBacked(8.5f);
        sf_bold_12 = FontRenderer.vanillaBacked(6f);
        sf_bold_mini = FontRenderer.vanillaBacked(7f);
        sf_medium = FontRenderer.vanillaBacked(8f);
        modules = sf_bold;
        minecraft = sf_bold;
    }

    public static FontRenderer create(float size, String name) throws IOException, FontFormatException {
        String path = "assets/soupapi/fonts/" + name + ".ttf";
        InputStream in = SoupAPI.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IOException("Missing bundled font: " + path);
        }
        try {
            return new FontRenderer(
                    Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(Font.PLAIN, size / 2f),
                    size / 2f);
        } finally {
            in.close();
        }
    }
}
