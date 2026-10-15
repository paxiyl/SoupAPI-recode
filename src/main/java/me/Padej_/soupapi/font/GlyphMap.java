package me.Padej_.soupapi.font;

import me.Padej_.soupapi.compat.Caps;
import me.Padej_.soupapi.compat.SoupLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One page of rasterised glyphs, baked into a single texture atlas.
 *
 * <p>Two things changed in the 1.8.9 port:
 *
 * <ul>
 *   <li><b>Texture upload is far simpler.</b> The 1.21.4 version built a {@code NativeImage},
 *       reached into its off-heap pointer via an accessor mixin and wrote ABGR ints through
 *       {@code MemoryUtil}. 1.8.9's {@link NativeImageBackedTexture} takes a {@link BufferedImage}
 *       directly and handles the channel order itself, so all of that goes away — along with the
 *       {@code NativeImageAccessor} mixin and the LWJGL 3 {@code MemoryUtil} dependency, neither of
 *       which exists here.</li>
 *   <li><b>The atlas is padded to a power of two.</b> 1.8.9 happily uploads NPOT textures on
 *       desktop drivers, but gl4es on PojavLauncher is only reliable with power-of-two dimensions
 *       for this kind of dynamic texture. Padding is invisible to callers because glyph UVs are
 *       computed against {@link #width}/{@link #height}, which are the padded dimensions.</li>
 * </ul>
 *
 * <p>fastutil's {@code Char2ObjectArrayMap} is replaced with a plain {@link HashMap}: fastutil is a
 * 1.14+ Minecraft dependency and is not on the 1.8.9 classpath.
 */
class GlyphMap {
    final char fromIncl, toExcl;
    final Font font;
    final Identifier bindToTexture;
    final int pixelPadding;
    private final Map<Character, Glyph> glyphs = new HashMap<Character, Glyph>();
    int width, height;

    boolean generated = false;

    public GlyphMap(char from, char to, Font font, Identifier identifier, int padding) {
        this.fromIncl = from;
        this.toExcl = to;
        this.font = font;
        this.bindToTexture = identifier;
        this.pixelPadding = padding;
    }

    public Glyph getGlyph(char c) {
        if (!generated) {
            generate();
        }
        return glyphs.get(Character.valueOf(c));
    }

    public void destroy() {
        MinecraftClient.getInstance().getTextureManager().close(this.bindToTexture);
        this.glyphs.clear();
        this.width = -1;
        this.height = -1;
        generated = false;
    }

    public boolean contains(char c) {
        return c >= fromIncl && c < toExcl;
    }

    private Font getFontForGlyph(char c) {
        font.canDisplay(c);
        // No font can display it, so it does not matter which is picked; it will always be missing.
        return this.font;
    }

    public void generate() {
        if (generated) {
            return;
        }
        int range = toExcl - fromIncl - 1;
        // Lay the page out roughly twice as wide as it is tall.
        int charsVert = (int) (Math.ceil(Math.sqrt(range)) * 1.5);
        glyphs.clear();
        int generatedChars = 0;
        int charNX = 0;
        int maxX = 0, maxY = 0;
        int currentX = 0, currentY = 0;
        int currentRowMaxY = 0;
        List<Glyph> pending = new ArrayList<Glyph>();
        AffineTransform af = new AffineTransform();
        FontRenderContext frc = new FontRenderContext(af, true, false);
        while (generatedChars <= range) {
            char currentChar = (char) (fromIncl + generatedChars);
            Font glyphFont = getFontForGlyph(currentChar);
            Rectangle2D stringBounds = glyphFont.getStringBounds(String.valueOf(currentChar), frc);

            int glyphWidth = (int) Math.ceil(stringBounds.getWidth());
            int glyphHeight = (int) Math.ceil(stringBounds.getHeight());
            generatedChars++;
            maxX = Math.max(maxX, currentX + glyphWidth);
            maxY = Math.max(maxY, currentY + glyphHeight);
            if (charNX >= charsVert) {
                currentX = 0;
                // Advance by the tallest glyph in the row that just ended.
                currentY += currentRowMaxY + pixelPadding;
                charNX = 0;
                currentRowMaxY = 0;
            }
            currentRowMaxY = Math.max(currentRowMaxY, glyphHeight);
            pending.add(new Glyph(currentX, currentY, glyphWidth, glyphHeight, currentChar, this));
            currentX += glyphWidth + pixelPadding;
            charNX++;
        }

        int neededWidth = Math.max(maxX + pixelPadding, 1);
        int neededHeight = Math.max(maxY + pixelPadding, 1);
        // gl4es wants power-of-two dimensions for dynamic textures; see the class comment.
        int imageWidth = nextPowerOfTwo(neededWidth);
        int imageHeight = nextPowerOfTwo(neededHeight);

        BufferedImage bi = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        width = imageWidth;
        height = imageHeight;
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(new Color(255, 255, 255, 0));
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);

        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int i = 0; i < pending.size(); i++) {
            Glyph glyph = pending.get(i);
            g2d.setFont(getFontForGlyph(glyph.value()));
            FontMetrics fontMetrics = g2d.getFontMetrics();
            g2d.drawString(String.valueOf(glyph.value()), glyph.u(), glyph.v() + fontMetrics.getAscent());
            glyphs.put(Character.valueOf(glyph.value()), glyph);
        }
        g2d.dispose();

        registerBufferedImageTexture(bindToTexture, bi);
        generated = true;
    }

    private static int nextPowerOfTwo(int value) {
        int p = 1;
        while (p < value) {
            p <<= 1;
        }
        return p;
    }

    /**
     * Uploads a baked atlas and registers it under {@code id}.
     *
     * <p>Must run on the render thread, which in 1.8.9 is the only thread that exists for
     * rendering; glyph pages are baked lazily from draw calls, so that holds. The prebake worker
     * only computes metrics, it does not upload.
     */
    public static void registerBufferedImageTexture(Identifier id, BufferedImage bi) {
        try {
            NativeImageBackedTexture tex = new NativeImageBackedTexture(bi);
            tex.upload();
            MinecraftClient.getInstance().getTextureManager().loadTexture(id, tex);
        } catch (Throwable t) {
            // A failed glyph upload must not take the frame down; text just will not draw.
            SoupLog.error("Failed to upload glyph atlas " + id
                    + (Caps.isPojav() ? " (Pojav/translated GL)" : ""), t);
        }
    }
}
