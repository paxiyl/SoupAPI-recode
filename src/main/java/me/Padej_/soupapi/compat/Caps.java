package me.Padej_.soupapi.compat;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.Locale;

/**
 * Runtime platform and GL capability detection.
 *
 * <p>The port has to run on two very different stacks: desktop LWJGL 2 against a real desktop GL
 * driver, and PojavLauncher on Android, where LWJGL 2 is a stub over a GL ES translation layer
 * (gl4es, or Zink/ANGLE on newer Pojav builds). Most of 1.8.9's rendering is plain fixed-function
 * work that translates fine, but a handful of things the 1.21.4 sources relied on do not:
 *
 * <ul>
 *   <li><b>Destination-alpha blending.</b> The target HUD masked the player head with
 *       {@code glBlendFunc(GL_DST_ALPHA, GL_ONE_MINUS_DST_ALPHA)} after clearing the alpha channel.
 *       That needs an alpha channel in the draw buffer, which Android EGL configs frequently do not
 *       have, and a mid-frame {@code glClear} of the colour buffer, which is expensive on tiled
 *       mobile GPUs. The port composites rounded masks on the CPU instead
 *       ({@code TexturesManager#roundedHead}), so this flag only reports what the hardware can do
 *       and is not needed for correct output.</li>
 *   <li><b>AWT font rasterisation.</b> 1.8.9 itself needs {@code BufferedImage} and {@code ImageIO}
 *       so those are always present, but {@code java.awt.Font} + {@code Graphics2D} glyph rendering
 *       depends on libfontmanager/freetype being present and working in the JRE, which is not
 *       guaranteed on Pojav. {@link #canRasteriseAwtFonts()} probes it once and the font renderer
 *       falls back to vanilla's {@code TextRenderer} if it fails.</li>
 *   <li><b>Line width.</b> gl4es clamps {@code glLineWidth} to 1.0, so outlines drawn as GL lines
 *       come out hairline-thin. Shapes that need a visible stroke are drawn as triangle strips
 *       instead when {@link #thickLinesSupported()} is false.</li>
 * </ul>
 *
 * <p>All GL-dependent probes are lazy and must only be called with a current GL context; they cache
 * their result and degrade to the conservative answer if the query throws.
 */
public final class Caps {

    private Caps() {
    }

    private static final boolean ANDROID;
    private static final boolean POJAV;

    static {
        boolean android = false;
        try {
            // Pojav's JRE runs under Android's userspace, which always exports these.
            android = System.getenv("ANDROID_ROOT") != null
                    || System.getenv("ANDROID_DATA") != null
                    || new File("/system/build.prop").exists();
        } catch (Throwable ignored) {
            // SecurityManager or an unusual sandbox: assume desktop.
        }
        ANDROID = android;

        boolean pojav = false;
        try {
            pojav = System.getenv("POJAV_RENDERER") != null
                    || System.getProperty("pojav.renderer") != null
                    || String.valueOf(System.getProperty("java.home")).contains("pojav")
                    || (android && String.valueOf(System.getProperty("java.home")).startsWith("/data/"));
        } catch (Throwable ignored) {
        }
        POJAV = pojav || android;
    }

    /** True when running under Android userspace (PojavLauncher and friends). */
    public static boolean isAndroid() {
        return ANDROID;
    }

    /** True when running under PojavLauncher, or anything else Android-hosted. */
    public static boolean isPojav() {
        return POJAV;
    }

    // ---- GL-context-dependent probes -------------------------------------------------------

    private static Boolean gl4es;
    private static Boolean destAlpha;
    private static Boolean thickLines;
    private static String rendererName;

    /** The raw {@code GL_RENDERER} string, or {@code ""} if it cannot be read yet. */
    public static String rendererName() {
        if (rendererName == null) {
            String s = "";
            try {
                String r = GL11.glGetString(GL11.GL_RENDERER);
                String v = GL11.glGetString(GL11.GL_VERSION);
                s = ((r == null ? "" : r) + " " + (v == null ? "" : v)).toLowerCase(Locale.ROOT);
            } catch (Throwable ignored) {
                // No context yet; do not cache a wrong answer.
                return "";
            }
            rendererName = s;
        }
        return rendererName;
    }

    /** True when the GL implementation is a GL ES translation layer rather than desktop GL. */
    public static boolean isTranslatedGl() {
        if (gl4es == null) {
            String r = rendererName();
            if (r.isEmpty()) return POJAV;
            gl4es = Boolean.valueOf(r.contains("gl4es")
                    || r.contains("opengl es")
                    || r.contains("angle")
                    || r.contains("zink")
                    || r.contains("virgl")
                    || r.contains("swiftshader"));
        }
        return gl4es.booleanValue();
    }

    /**
     * True when the current draw buffer has an alpha channel, so destination-alpha blending would
     * actually work. The port does not rely on this, but it is used to pick the cheaper path on
     * desktop where it is available.
     */
    public static boolean hasDestinationAlpha() {
        if (destAlpha == null) {
            try {
                destAlpha = Boolean.valueOf(GL11.glGetInteger(GL11.GL_ALPHA_BITS) > 0);
            } catch (Throwable ignored) {
                return false;
            }
        }
        return destAlpha.booleanValue();
    }

    /** True when {@code glLineWidth} values above 1.0 are honoured. */
    public static boolean thickLinesSupported() {
        if (thickLines == null) {
            if (isTranslatedGl()) {
                thickLines = Boolean.FALSE;
            } else {
                try {
                    java.nio.FloatBuffer range = org.lwjgl.BufferUtils.createFloatBuffer(16);
                    GL11.glGetFloat(GL11.GL_LINE_WIDTH_RANGE, range);
                    thickLines = Boolean.valueOf(range.get(1) > 1.5f);
                } catch (Throwable ignored) {
                    return false;
                }
            }
        }
        return thickLines.booleanValue();
    }

    // ---- AWT font rasterisation probe ------------------------------------------------------

    private static Boolean awtFonts;

    /**
     * Probes whether {@code java.awt.Font} glyph rasterisation works. Pojav JREs ship AWT (1.8.9
     * needs {@code BufferedImage}/{@code ImageIO} to boot at all) but the font pipeline is a
     * separate native library and is not always functional.
     */
    public static boolean canRasteriseAwtFonts() {
        if (awtFonts == null) {
            boolean ok = false;
            try {
                java.awt.image.BufferedImage probe =
                        new java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = probe.createGraphics();
                java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 8);
                g.setFont(font);
                // getFontMetrics and a real drawString both go through the native font pipeline.
                g.getFontMetrics().charWidth('A');
                g.drawString("A", 0, 8);
                g.dispose();
                ok = true;
            } catch (Throwable t) {
                SoupLog.warn("AWT font rasterisation unavailable, falling back to the vanilla font renderer", t);
            }
            awtFonts = Boolean.valueOf(ok);
        }
        return awtFonts.booleanValue();
    }

    /** Human-readable summary for the log line emitted at startup. */
    public static String describe() {
        return "android=" + ANDROID
                + " pojav=" + POJAV
                + " translatedGl=" + isTranslatedGl()
                + " destAlpha=" + hasDestinationAlpha()
                + " thickLines=" + thickLinesSupported()
                + " awtFonts=" + canRasteriseAwtFonts()
                + " renderer='" + rendererName() + "'";
    }
}
